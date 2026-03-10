## Wallet Service

Este servicio es el **motor financiero y ledger** de BidCast.

- **Tecnología**: Java 21, Spring Boot 4.0.3, PostgreSQL 16, Testcontainers.
- **Patrón de datos**: Ledger inmutable (`WalletTransaction`) con `Wallet` como snapshot con `@Version` (optimistic locking).
- **Reglas clave**:
  - Nunca se actualiza el saldo restando directo; siempre se registran transacciones.
  - Idempotencia fuerte por `proofOfPlayId` para cobros de Proof of Play.
  - Transacciones ACID: descuento a advertiser, pago a publisher y fee de plataforma se aplican en la misma transacción.

### Endpoint principal: Proof of Play Charge

- **URI**: `POST /api/v1/proof-of-play-charges`
- **Body** (`ProofOfPlayChargeCommand`):

```json
{
  "proofOfPlayId": "uuid",
  "grossAmount": 1.0000,
  "publisherAmount": 0.7000,
  "platformFeeAmount": 0.3000,
  "advertiserWalletId": "uuid",
  "publisherWalletId": "uuid",
  "platformWalletId": "uuid"
}
```

- **Respuestas**:
  - `201 Created`: cobro aplicado (o ya aplicado previamente, gracias a idempotencia).
  - `400 Bad Request`: request inválido (Bean Validation o suma `publisher + platform != gross`).
  - `409 Conflict`: saldo insuficiente en la wallet del advertiser.

### Testing

- Tests de integración con Postgres real usando Testcontainers (`TestcontainersConfiguration`).
- `ProofOfPlaySettlementServiceIntegrationTest`: verifica lógica de negocio, ledger e idempotencia.
- `ProofOfPlayChargeControllerTest`: verifica el endpoint HTTP completo con `RestTestClient`.

