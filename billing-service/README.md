# Billing Service: Payment Orchestration and External Reconciliation

The `billing-service` acts as the financial gateway between Bidcast and external payment providers (currently Mercado Pago). Its primary responsibility is managing the lifecycle of balance top-ups, from the creation of the payment preference to final reconciliation via webhooks.

## Service Responsibilities

1.  **Payment Preference Management:** Generation of customized "Checkout Pro" preferences using the Mercado Pago SDK (v2.8.0), linking internal identifiers with external payment sessions.
2.  **Webhook Processing (IPN):** Reception and validation of asynchronous payment status notifications.
3.  **Transaction Auditing:** Persistent logging of payment attempts with state traceability (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`).
4.  **Financial Event Emission:** Notifying the `wallet-service` via RabbitMQ upon successful transaction confirmation for balance crediting.

## Technical Implementation and Security

### Integrity Validation (Signature Shield)
The service implements an HMAC-SHA256 signature validation layer for all incoming requests to the webhook endpoint.
*   **Mechanism:** Validation of the `x-signature` header using the configured `webhook-secret`.
*   **Security:** In production environments, payment notifications with invalid or missing signatures result in a `403 Forbidden`, mitigating false payment injection attacks.
*   **Webhook Robustness:** Malformed payment notifications, unknown `external_reference` values, and duplicate approvals are treated as safe no-ops to avoid unnecessary retries from Mercado Pago.

### Consistency and Concurrency Control
*   **Idempotency:** Implementation of status pre-checks and database uniqueness constraints to prevent duplicate processing of notifications.
*   **Optimistic Locking:** Use of JPA's `@Version` annotation in the `Payment` entity to ensure data integrity during bursts of concurrent updates (Double-Credit Prevention).

## Tech Stack

*   **Runtime:** Java 21, Spring Boot 4.0.3.
*   **Persistence:** Spring Data JPA, Hibernate 7, PostgreSQL 16.
*   **Messaging:** Spring AMQP (RabbitMQ).
*   **Integration:** Mercado Pago Java SDK.

## Infrastructure Configuration

The service requires the following configuration properties for operational functioning:

| Property | Description |
| :--- | :--- |
| `mercadopago.access.token` | Access token from the MP developer portal. |
| `mercadopago.webhook.secret` | Secret key for HMAC signature validation of notifications. |
| `mercadopago.notification-url` | Public endpoint configured in the MP dashboard for event delivery. |

## Testing Strategy

The service ensures its stability through a two-layer testing suite:

1.  **Unit Tests (JUnit 5 + Mockito):** Validation of business logic, DTO construction, and SDK response mappings.
    Webhook controller/service tests cover signature rejection, malformed payment notifications, idempotent approval handling, and wallet event emission.
2.  **Integration Tests (Failsafe + Testcontainers):** Execution of complete flows using ephemeral PostgreSQL 16 and RabbitMQ 3.12 containers, ensuring compatibility with real infrastructure.

### Running the Full Suite

```bash
./mvnw clean verify
```
