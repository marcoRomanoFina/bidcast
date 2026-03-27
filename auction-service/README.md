# Auction Service: The RTB (Real-Time Bidding) Engine

The **auction-service** is the heart of Bidcast. It is the microservice responsible for executing real-time auctions between devices and advertising campaigns.  
It manages the state of active sessions, processes advertiser bids, and coordinates with the Wallet Service to ensure financial integrity by freezing balance before the auction and confirming payment once the Proof of Play (PoP) is received.

##

## Core Domain Entities

### 1. Session 

Represents an active and temporary opportunity where a device is ready to receive and display ads.

A session is created when the user, from the `Device Player` (not implemented yet), opens a session by sending a message through RabbitMQ. From that moment, it remains in **ACTIVE** state and becomes available for advertisers to participate.

During the session, advertisers register **Session Bids**, where they define how much budget they allocate to that device, relevant metadata, and how much they are willing to pay per impression (calculated from RPM).

The session defines the context in which an auction is valid: if it does not exist or has expired, nothing is processed. This helps prevent inconsistencies in a distributed system.

> Currently the relationship is 1:1 between session and device. Later on, the idea is to support multiple devices per publisher.

### 2. SessionBid 

Represents an advertiser’s participation within a session.

It contains the budget allocated to that bid, the advertiser, the campaign, the ad URL, and other metadata required to compete in the auction.

The main idea is that the budget assigned to a SessionBid acts as a **freeze** over the advertiser’s total balance. This guarantees the advertiser cannot spend more than they actually have.

When the session ends, settlement is performed: the real spent amount is deducted and the remaining budget is released back to the advertiser balance.

If the remaining budget is not enough to meet the base price (pending implementation), the bid is marked as **EXHAUSTED** and stops participating until more budget is assigned.

### 3. Proof Of Play 

Represents the proof that an ad was actually displayed on the device.

It contains information such as the `advertiserId`, the winning `bidId`, the `session`, the paid impression price, and other relevant metadata.

It is used as the **source of truth** within the system: in case of any inconsistency with Redis, Proof of Play records allow the system to reconstruct and validate the real advertiser spending.

To guarantee integrity and authenticity, a JWS-like mechanism is used. However, this does not provide confidentiality (to be addressed later).

> Note: In the future, the system will incorporate an `auctionId` in order to trace each auction end-to-end (decision, execution and settlement), making debugging and analytics easier.

---

## Proof of Play Flow (ReceiptID)

To ensure an ad was actually displayed before charging it, the system uses a **Receipt ID**: a signed token that connects the winning auction with its actual playback on the device.

This mechanism guarantees financial integrity between the `auction-service` and the `wallet-service`.

### 1. Generation (Auction)

When the Auction Engine selects a winner:

- A unique `Receipt ID` is generated with information such as `campaignId`, `bidAmount`, `sessionId` and timestamp.
- The Wallet Service is requested to freeze the corresponding balance.
- The ad is sent to the device together with this token, but no money is charged yet.

```java
    /*
     Generates a signed token (JWS-like) with embedded metadata.
     Format: sessionId:bidId:advertiserId:bidPrice:timestamp:signature
     */
    public String generateReceiptId(String sessionId, UUID bidId, String advertiserId, BigDecimal bidPrice) {
        long timestamp = Instant.now().getEpochSecond();
        String payload = String.format("%s:%s:%s:%s:%d", sessionId, bidId, advertiserId, bidPrice.toPlainString(), timestamp);
        String signature = calculateHmac(payload);
        return payload + ":" + signature;
    }
```

### 2. Validation (Playback)

When the ad finishes playing:

- The device sends the Receipt ID to the Proof of Play endpoint.
- The token signature is validated, along with expiration and whether the session is still active.
- It is verified that it has not been processed before, preventing double charging.

```java
  
     // Processes a playback confirmation (PoP)
    public void recordPlay(PopRequest request) {
        log.info("Processing Proof of Play for bid {} in session {}", request.bidId(), request.sessionId());

        // 1. Stateless verification
        ValidatedReceipt validated = this.validateTicket(request);
```

### 3. Settlement
If everything is valid:

- The Proof of Play is stored as evidence.
- It is used as the source of truth so the Wallet Service can debit the frozen balance.
- The cycle is completed: the advertiser pays and the publisher receives the credit.


### Reconciliation and additional validations
Besides the main flow, the system applies extra mechanisms to prevent inconsistencies and edge-case scenarios:
- **Synchronous Receipt validation:**
Token expiration is checked at the moment the device submits it.
If the Receipt is older than 10 minutes (`MAX_RECEIPT_AGE_SECONDS = 600`), it is immediately rejected.

- **Idempotency in Redis:**
Processed receipts are stored temporarily (2 hours) to prevent the same PoP from being submitted more than once.

- **Ghost session cleanup:**
A background job periodically closes sessions that have been open for more than 24 hours, preventing inconsistent states.
```java
   @Scheduled(fixedRate = 3600000) // every 1h
    @SchedulerLock(name = "SessionReconciliationJob_reapGhostSessions", lockAtMostFor = "10m")
    public void reapGhostSessions() {
        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        // Finds stale active sessions and force-closes them
        slice = sessionService.findStaleActiveSessions(threshold, pageRequest);
        // ... settlement orchestration
    }
```

### Notes
- The Receipt is signed, so it cannot be forged.
- The system clearly separates intent to spend (freeze) from real spending (PoP).
- Reconciliation acts as a safety net against real-world failures.

##  Concurrency and Idempotency

In this system, processing a PoP twice implies double charging.  
To prevent this, there are several layers of defense working together.

### 1. Idempotency (Redis + DB)

A two-layer approach is used:

- **Redis (fast)**:  
  Before processing a PoP, a flag is attempted to be saved with `SETNX` (2-hour TTL).  
  If it already exists, it is immediately discarded without hitting the database.

```java
    private boolean isAlreadyProcessed(String receiptId) {
        String key = "pop:processed:" + receiptId;
        // Atomic SETNX: returns true only if the key didn't exist
        return Optional.ofNullable(redisTemplate.opsForValue()
                .setIfAbsent(key, "true", Duration.ofHours(2)))
                .map(isNew -> !isNew) // if not new, it was already processed
                .orElse(false);
    }
```

- **Database (Source of Truth)**:  
  The `proof_of_play` table has a unique constraint on the `receipt_id`.  
  If for any reason Redis fails, the DB prevents a duplicate insert and an automatic rollback is triggered.


### 2. Budget decrement in Redis

Campaign budgets are managed in Redis using atomic operations (negative `increment`).

This allows multiple concurrent PoPs to deduct balance without overwriting each other, avoiding race conditions even under high concurrency.

```java
    // Atomic decrement using native Redis commands (HINCRBY)
    long costInCents = validated.advertiserBidPrice().multiply(new BigDecimal("100")).longValue();
    long newBalance = redisTemplate.opsForHash().increment(bidKey, "budget", -costInCents);
```


### 3. Background Jobs (ShedLock)

For tasks such as session cleanup:

- ShedLock is used over Redis.
- If there are multiple instances of the service, only one executes the job.

This prevents multiple nodes from trying to do the same thing at the same time.


### 4. Communication with Wallet (Outbox Pattern)

Charging is not done directly to guarantee that no event is lost during network or broker outages:

- **Local Atomicity**: The PoP record and the event to be sent are saved in the same database transaction (outbox table).
- **Batch Processing**: The `OutboxRelay` processes events in controlled batches (50 at a time) to optimize memory and resources.
- **Isolated Transactions**: Each event is processed in its own transaction (`REQUIRES_NEW`), ensuring an individual failure doesn't stop the rest of the batch.
- **Horizontal Scalability (SKIP LOCKED)**: The query uses row-level locks that allow multiple service instances to work in parallel on the outbox without processing the same event twice.

```java
// OutboxRelay: Batch polling with configurable delay
@Scheduled(fixedDelayString = "5000")
public void scheduleDispatch() {
    List<OutboxEvent> pending = outboxRepository.findPending(PageRequest.of(0, 50));
    pending.forEach(event -> outboxWorker.process(event.getId()));
}

// OutboxWorker: Atomic and independent processing
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void process(UUID eventId) {
    // Tries to lock the row; if locked by another node, it skips it (SKIP LOCKED)
    Optional<OutboxEvent> event = outboxRepository.findAndLockById(eventId);
    if (event.isPresent()) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        event.get().setProcessed(true);
    }
}
```

This ensures that the money is eventually processed without losing events and with *At-least-once delivery* semantics.



### 5. Session Management (Redis TTL)

Sessions live in Redis with a TTL.

- Every time a valid PoP arrives, its life time is extended.
- If there is no activity, they expire automatically.

This avoids keeping "dead" sessions unnecessarily.

##  State Management with Redis

Redis is not just used as a cache, but as an in-memory state layer to make real-time decisions without hitting the database constantly.

### 1. Active Bids Index

- **Key**: `session:{sessionId}:active_bids`
- Stores the bids that can participate in a session.
- Used in the auction to avoid querying PostgreSQL.

---

### 2. Real-time Budgets

- **Key**: `session:{sessionId}:bid:{bidId}`
- Stores:
  - `budget`: remaining balance (in cents).
  - `status`: bid status.

The deduction is made with atomic operations (`HINCRBY`), allowing many concurrent PoPs without race conditions.

If the budget reaches 0, the bid is marked as exhausted and stops being used.

---

### 3. PoP Idempotency

- **Key**: `pop:processed:{receiptId}`
- TTL: 2 hours.

Used as a fast filter to avoid processing the same PoP more than once before hitting the database.

---

## State Rehydration

Since Redis is volatile, it can lose data (restart, eviction, etc.).  
For this, there is a rehydration mechanism.

### When does it happen?

- When information is missing in Redis (cache miss).
- When inconsistencies with the database are detected.

---

### What does it do?

1. Goes to PostgreSQL (Source of Truth).
2. Recalculates the real bid state:
   - initial budget - confirmed PoPs.

```java
    // State recovery on cache-miss or inconsistency
    rehydrationService.rehydrateFullBid(bidUuid); 
    long balance = rehydrationService.calculateRealBalanceCents(bidUuid) - cost;
    redisTemplate.opsForHash().put(bidKey, "budget", String.valueOf(balance));
```
3. Loads that value back into Redis.
4. If the bid was already exhausted, it clears it so it doesn't participate anymore.


This allows maintaining Redis as a fast layer without losing long-term consistency.

##  Stack

- **Java 21**: use of Virtual Threads to handle high concurrency.
- **Spring Boot**: service base.
- **PostgreSQL**: Source of Truth (auditing + outbox).
- **Redis (Redisson)**: real-time state of auctions.
- **RabbitMQ**: communication between services (event-driven).
- **ShedLock**: prevents background jobs from running in parallel across multiple instances.

---

##  Error Handling

Domain exceptions are used with a global `@RestControllerAdvice` to map clear responses:

- `NoAdFoundException` → 404 (no eligible campaigns).
- `InvalidPlayReceiptException` → 400 (invalid or expired PoP).
- `SessionConcurrencyException` → 409 (state conflict).
- `WalletCommunicationException` → 503 (error interacting with wallet).

The idea is that each error represents a real domain problem, not just a generic 500.

---

##  API

Documented with OpenAPI (Swagger):

- Swagger UI: http://localhost:8083/swagger-ui/index.html  
- JSON spec: http://localhost:8083/v3/api-docs  

### Main Endpoints

- `POST /v1/auctions/request` → requests an auction.
- `POST /v1/pop/record` → registers a Proof of Play.
- `POST /v1/bids/register` → registers a bid in a session.

 ## Testing

The goal is not only to test logic, but to cover real scenarios: concurrency, duplicates, and infrastructure failures.

### 1. Unit tests

The most critical parts are tested in isolation:

- **AuctionEngine**: ad selection logic (budget, priority, etc.).
- **ReceiptTokenService**: token signing and validation (expiration, tampering).
- Mocks (Mockito) are used to avoid dependency on Redis, DB, or Wallet.

---

### 2. Integration tests (with Testcontainers)

Instead of using embedded tools like H2, tests start real services with Docker:

- **PostgreSQL**: to validate constraints (e.g., idempotency) and the outbox.
- **RabbitMQ**: to verify that events are published correctly.
- **Redis**: to test atomic decrements and locks.

The idea is to test against something as close to production as possible.

```java
    @Testcontainers
    class ProofOfPlayIntegrationTest {
        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        
        @Container
        static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        
        // Tests validating idempotency and real persistence
    }
```

---

### 3. Edge cases being tested

Not just happy path:

- **Double spending**: attempting to process the same Receipt multiple times.
- **Budget exhaustion**: when a campaign runs out of balance.
- **Network failures**: outbox retries when RabbitMQ is unavailable.

---

### 4. How to run tests

Requires Docker (for Testcontainers):

```bash
# all tests
./mvnw test

# specific test
./mvnw test -Dtest=ProofOfPlayServiceTest

# coverage
./mvnw verify
```
