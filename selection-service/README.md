# Selection Service

`selection-service` es el servicio que decide que anuncio puede mostrar un device dentro de una session activa. No modela RTB clasico por impresion; modela seleccion por slots de tiempo dentro de una reproduccion continua.

## Responsabilidades

- registrar `SessionOffer`s para una session
- traer snapshots de campaign y creatives desde `advertisement-service`
- mantener estado hot en Redis para offers activas y budget disponible
- devolver los siguientes `N` candidates para un `device`
- registrar `Proof of Play`
- reservar cooldown local por `device + creative` al seleccionar
- aplicar penalizacion global por `campaign` cuando llega el `PoP`
- cerrar la session con settlement contra el frozen en `wallet-service`

## Modelo actual

### SessionOffer

Una `SessionOffer` representa la entrada economica de un advertiser en una session.

Incluye:

- `sessionId`
- `advertiserId`
- `campaignId`
- `totalBudget`
- `pricePerSlot`
- `deviceCooldownSeconds`
- lista de `CreativeSnapshot`

Cada offer entra una sola vez a la session. Los creatives no compiten como ofertas separadas: la seleccion economica se hace sobre la offer y despues se rota el creative elegible dentro de esa offer.

### CreativeSnapshot

Es una foto liviana del creative tomada desde `advertisement-service` al crear la offer.

Incluye:

- `creativeId`
- `mediaUrl`
- `slotCount`

### Proof Of Play

El `PoP` es la confirmacion de que un creative realmente fue reproducido en un device.

Cuando llega un `PoP`:

- se valida el receipt firmado
- se persiste el evento como source of truth
- se descuenta budget hot en Redis
- se registra recencia global por campaign

## Redis

Redis se usa como capa de estado efimero y hot-path:

- `session:{sessionId}:active_offers`
  indice de offers activas
- `session:{sessionId}:offer:{offerId}`
  hash con `budget` y `metadata`
- `session:{sessionId}:campaign:{campaignId}:last_played`
  recencia global para penalizacion soft
- `session:{sessionId}:device:{deviceId}:creative:{creativeId}:cooldown`
  cooldown local fuerte por device
- `pop:processed:{receiptId}`
  idempotencia de PoP

Si Redis pierde los cooldowns locales, no se rompe la integridad financiera: simplemente vuelven a construirse con los proximos `PoP`.

## Seleccion de candidates

`POST /api/v1/selection/candidates`

Request:

```json
{
  "sessionId": "session-1",
  "deviceId": "device-1",
  "count": 2,
  "excludedCreativeIds": ["creative-x"]
}
```

El selector:

1. carga offers activas de la session
2. excluye creatives bloqueados para ese device
3. aplica penalizacion por campaign reciente
4. rota creatives dentro de cada offer
5. bloquea en Redis los creatives seleccionados para ese device
6. devuelve los mejores `N` candidates ya resueltos

Response:

```json
[
  {
    "offerId": "4e8d7c3f-6d8f-4f1c-8d29-4ffb5e4c876e",
    "sessionId": "session-1",
    "deviceId": "device-1",
    "advertiserId": "adv-1",
    "campaignId": "camp-1",
    "pricePerSlot": 10.00,
    "creativeId": "creative-1",
    "mediaUrl": "https://cdn.example.com/creative-1.mp4",
    "slotCount": 1,
    "deviceCooldownSeconds": 300,
    "playReceiptId": "signed-receipt"
  }
]
```

### Errores operativos

- `409 Conflict`
  otra seleccion ya esta corriendo para la misma session
- `503 Service Unavailable`
  Redis o Redisson no estan disponibles y el hot path no puede operar de forma segura

## Proof of Play

`POST /api/v1/selection/pop`

Request:

```json
{
  "sessionId": "session-1",
  "deviceId": "device-1",
  "offerId": "4e8d7c3f-6d8f-4f1c-8d29-4ffb5e4c876e",
  "creativeId": "creative-1",
  "playReceiptId": "signed-receipt"
}
```

## Rehydration

Si Redis pierde el estado hot de una offer:

- `OfferRehydrationService` reconstruye metadata y budget desde PostgreSQL
- el budget real se calcula usando `ProofOfPlay` como source of truth
- la offer vuelve a inyectarse en Redis

Los cooldowns locales por creative no se rehidratan a proposito, porque son efimeros y pueden recomenzar sin impacto contable.

## Flujo resumido

1. el advertiser crea una `SessionOffer`
2. el servicio congela budget en wallet
3. el device pide `N` candidates
4. el servicio responde creatives ya seleccionados
5. el device reproduce uno
6. manda `PoP`
7. el servicio descuenta budget y registra recencia global por campaign
8. al final de la session se hace settlement

## Tests

Para correr la suite del modulo:

```bash
./mvnw test
```
