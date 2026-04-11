# Wallet Service: Financial Ledger and Settlement Engine

The `wallet-service` is the microservice responsible for transactional accounting across the Adcast ecosystem. It centralizes balances, frozen funds, credits, settlements, and historical auditing through a persistent ledger, maintaining financial integrity between advertisers, publishers, and the platform.

## Service Responsibilities

1.  **Wallet management:** maintenance of available balance and frozen balance per actor (`ADVERTISER`, `PUBLISHER`, `PLATFORM`).
2.  **Credit processing:** idempotent crediting of top-ups or refunds through `referenceId`.
3.  **Financial settlement:** final settlement of bidding sessions, splitting spending between the publisher and platform fee while returning leftovers to the advertiser.
4.  **Ledger auditing:** historical record of movements in `ledger_entries` for full traceability.
5.  **Consistency protection:** concurrency control through optimistic locking and explicit domain validations.

## Technical Design and Consistency

### Wallet domain model

The `Wallet` entity encapsulates critical domain rules: it does not allow invalid credits or debits, prevents overdrafts, and clearly separates `balance` from `frozenBalance`. It also uses `@Version` for optimistic locking, protecting state against concurrent updates.

```java
@Version
@Column(name = "version", nullable = false)
private long version;

public void freeze(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
        throw new InvalidWalletOperationException("Amount to freeze must be positive");
    }
    if (balance.compareTo(amount) < 0) {
        throw new InsufficientWalletBalanceException(id);
    }
    balance = balance.subtract(amount);
    frozenBalance = frozenBalance.add(amount);
}
```

This design keeps sensitive logic inside the aggregate and avoids having the service perform ad hoc financial arithmetic.

### Credit flow with idempotency and ledger

The credit flow in `WalletService` protects against retries using `referenceId + type` before touching balances. If the reference was already processed, the operation returns without duplicating either balance or ledger entries. When the wallet does not yet exist, it is created and persisted before recording the movement so transient entities are not referenced.

```java
@Transactional
public void credit(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount, UUID referenceId, String referenceType) {
    if (transactionRepository.existsByReferenceIdAndType(referenceId, WalletTransactionType.DEPOSIT)) {
        log.warn("Idempotency hit: credit with reference {} was already processed. Ignoring.", referenceId);
        return;
    }

    Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
            .orElseGet(() -> Wallet.builder()
                    .ownerId(ownerId)
                    .ownerType(ownerType)
                    .currencyCode("ARS")
                    .balance(BigDecimal.ZERO)
                    .frozenBalance(BigDecimal.ZERO)
                    .build());

    wallet.credit(amount);
    Wallet persistedWallet = walletRepository.save(wallet);

    transactionRepository.save(WalletTransaction.builder()
            .wallet(persistedWallet)
            .amount(amount)
            .balanceAfter(persistedWallet.getBalance())
            .type(WalletTransactionType.DEPOSIT)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .build());
}
```

The `ledger_entries` table also has a unique constraint on `(reference_id, type)`, adding a persistent defense against duplicates.

### Transactional settlement engine

`SessionSettlementService` implements the final reconciliation of a bidding session. It takes the advertiser's frozen budget, calculates the platform fee, credits the publisher, returns the leftover amount, and records three ledger entries in a single transaction.

```java
@Retryable(
    retryFor = ObjectOptimisticLockingFailureException.class,
    maxAttempts = 5,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
@Transactional
public void processSettlement(SessionSettlementCommand command) {
    if (isAlreadySettled(bidId)) {
        return;
    }

    Wallet advertiser = getWalletOrThrow(UUID.fromString(command.advertiserId()), WalletOwnerType.ADVERTISER);
    Wallet publisher = getWalletOrThrow(UUID.fromString(command.publisherId()), WalletOwnerType.PUBLISHER);
    Wallet platform = getPlatformWallet();

    advertiser.settleAndRefund(spent, command.initialBudget());
    publisher.credit(publisherNet);
    platform.credit(platformFee);

    recordLedgerEntries(bidId, advertiser, publisher, platform, spent, publisherNet, platformFee);
    walletRepository.saveAll(List.of(advertiser, publisher, platform));
}
```

That block is the financial core of the service: idempotent, transactional, and tolerant of concurrency collisions through retries on optimistic locking.

### HTTP contract and domain errors

The controller exposes routes aligned with the gateway (`/api/v1/wallets/...`), and the API returns explicit errors through `GlobalExceptionHandler` instead of propagating generic `RuntimeException`s. The read endpoint now returns a response DTO instead of exposing the JPA entity directly.

```java
@ExceptionHandler(WalletNotFoundException.class)
public ResponseEntity<Map<String, String>> handleWalletNotFound(WalletNotFoundException ex) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
}

@ExceptionHandler(InvalidWalletOperationException.class)
public ResponseEntity<Map<String, String>> handleInvalidWalletOperation(InvalidWalletOperationException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
}
```

The current contract is:

*   `400 Bad Request` for request validation errors.
*   `404 Not Found` when the wallet does not exist.
*   `409 Conflict` for insufficient balance or invalid domain rules.
*   `500 Internal Server Error` if the platform wallet is missing during settlement.

## Tech Stack

*   **Runtime:** Java 21, Spring Boot 4.0.3.
*   **Persistence:** Spring Data JPA, Hibernate 7, PostgreSQL 16.
*   **Messaging:** Spring AMQP / RabbitMQ for credit and settlement events.
*   **Resilience:** Spring Retry for retries on optimistic locking conflicts.
*   **Documentation:** Springdoc OpenAPI.

## Interactive Documentation

With the service running locally, the interactive documentation is available at:

*   **Swagger UI:** `http://localhost:8083/swagger-ui/index.html`
*   **OpenAPI JSON:** `http://localhost:8083/v3/api-docs`

## Testing

The testing strategy combines domain rules, HTTP contract checks, listeners, end-to-end settlement flows, and integrations with real PostgreSQL via Testcontainers. Since this is a financial microservice, the suite prioritizes idempotency, concurrency, and ledger consistency.

### Current coverage

*   **`WalletTest`:** covers pure `Wallet` aggregate rules, including freeze/unfreeze, settle/refund, insufficient balance, and invalid operations.
*   **`WalletServiceTest`:** covers wallet creation on first credit, idempotency by `referenceId`, persistence-race handling on the unique ledger constraint, lookup by owner, freeze/unfreeze/settle, and `WalletNotFoundException` errors.
*   **`WalletControllerTest`:** covers `GET /api/v1/wallets/me`, `400` validations, missing header, invalid UUID payloads, `404` for missing wallets, and `409` for insufficient balance through `GlobalExceptionHandler`.
*   **`SessionSettlementServiceTest`:** covers idempotency short-circuiting, missing wallet, missing platform wallet, and duplicate-ledger race handling.
*   **`SessionSettlementServiceIntegrationTest`:** verifies with real PostgreSQL fund distribution, advertiser refund, platform fee, publisher credit, and settlement deduplication.
*   **`WalletServiceIntegrationTest`:** verifies real idempotent crediting in the database without duplicating wallet or ledger records.
*   **`WalletConcurrencyIntegrationTest`:** tests real optimistic locking with stale snapshots, guaranteeing rejection of stale updates.
*   **`WalletCreditEventListenerTest` and `SettlementEventListenerTest`:** validate correct event delegation into the service layer.

### Test infrastructure

*   Integration tests use `@SpringBootTest` and PostgreSQL started with Testcontainers.
*   Database connectivity is resolved through `@ServiceConnection`, just like in the rest of the project's microservices.
*   The `test` profile disables auto-start for RabbitMQ listeners so the suite does not depend on a real broker.
*   Docker Desktop must be running to execute integration tests.

### Running tests

```bash
mvn test
```
