# Selection Service

`selection-service` is the playback decision engine for a venue session.
It does not model classic RTB impression bidding. It models continuous screen playback where multiple devices inside the same venue request the next paid media items to display.

The domain is slot-based:
- playback uses fixed 5-second units
- each creative consumes one or more slots
- economic value is `pricePerSlot * slotCount`
- selection returns confirmed playback items, not soft auction hints

## Domain Model

### Venue session playback

A venue session can have multiple devices requesting playback at nearly the same time.
Each device asks for the next `N` selections it can safely show, while the service coordinates:

- hot budget consumption
- local repetition control per device
- soft global pacing per campaign
- signed playback authorization
- proof-of-play confirmation

### SessionOffer

A `SessionOffer` is the economic entry of a campaign into a session.

It includes:
- `sessionId`
- `advertiserId`
- `campaignId`
- `totalBudget`
- `pricePerSlot`
- `deviceCooldownSeconds`
- a list of `CreativeSnapshot`s

Each campaign enters the session once as an offer. Creatives do not compete as separate bids. The engine scores the offer, then rotates the next eligible creative inside that offer.

### CreativeSnapshot

A `CreativeSnapshot` is a lightweight local copy fetched from `advertisement-service` when the offer is created.

It includes:
- `creativeId`
- `mediaUrl`
- `slotCount`

## Responsibilities

`selection-service` is responsible for:

- registering `SessionOffer`s for a session
- fetching campaign and creative snapshots from `advertisement-service`
- maintaining Redis hot state for active offers and remaining operational budget
- selecting the next confirmed playback items for a device
- consuming hot budget before returning a selection
- reserving local cooldown per `device + creative`
- validating signed receipts
- recording `Proof of Play`
- applying soft global recency per campaign
- closing the session with settlement-oriented data for downstream financial processing

## Selection Model

Selection is no longer framed as “who won the auction?”.
It is framed as “what should this device play next inside a shared venue session?”.

The hot path works like this:

1. load active offers for the session
2. exclude creatives already blocked for that device
3. apply a soft recency penalty for recently played campaigns
4. find the best `offer + creative` combination by playback value
5. advance the creative rotation pointer
6. consume hot budget in Redis
7. reserve the local device cooldown
8. return a fully resolved playback item with a signed receipt

This means the response already represents confirmed playback selections.

## Scoring

The scoring model is playback-oriented, not RTB-oriented.

- value is based on `pricePerSlot * slotCount`
- local repetition is controlled per `device + creative`
- global pacing is controlled per `campaign`
- budget is checked before returning the selection

So a cheaper offer per slot can still win if its creative occupies more slots and produces higher total playback value.

## Redis Hot State

Redis is used as operational read/write state, not as the durable source of truth.

Keys currently used:

- `session:{sessionId}:active_offers`
  active offer index for the session
- `session:{sessionId}:offer:{offerId}`
  hash containing `budget` and serialized `metadata`
- `session:{sessionId}:device:{deviceId}:creative:{creativeId}:cooldown`
  hard local cooldown for that device
- `session:{sessionId}:campaign:{campaignId}:last_played`
  soft global recency signal for campaign pacing
- `pop:processed:{receiptId}`
  short idempotency guard for proof-of-play processing

Redis may lose hot state without breaking financial integrity.
PostgreSQL remains the durable source of truth, and offer state can be rehydrated when needed.

## Selection Endpoint

`POST /api/v1/selection/candidates`

Example request:

```json
{
  "sessionId": "session-1",
  "deviceId": "device-1",
  "count": 2,
  "excludedCreativeIds": ["creative-x"]
}
```

Example response:

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

Operational errors:

- `409 Conflict`
  another worker is already selecting for the same session
- `503 Service Unavailable`
  Redis or Redisson is unavailable and the hot path cannot continue safely

## Proof of Play

`POST /api/v1/selection/pop`

Example request:

```json
{
  "sessionId": "session-1",
  "deviceId": "device-1",
  "offerId": "4e8d7c3f-6d8f-4f1c-8d29-4ffb5e4c876e",
  "creativeId": "creative-1",
  "playReceiptId": "signed-receipt"
}
```

When `PoP` arrives, the service:

- validates the signed receipt
- persists playback evidence in PostgreSQL
- applies idempotency checks
- updates global campaign recency

Budget is not first charged at `PoP`.
Budget is already consumed when selection is returned.
`PoP` confirms that the authorized playback really happened.

## Rehydration

If Redis loses the hot state of an offer:

- `OfferRehydrationService` rebuilds metadata and operational budget from PostgreSQL
- durable playback truth comes from `ProofOfPlay`
- the offer is injected back into Redis

Local device cooldowns are intentionally not rehydrated.
They are ephemeral pacing controls and can safely start fresh.

## Failure Model

- Redis down during selection: fail fast with `503`
- session already being selected by another worker: `409`
- missing hot budget: rehydrate and retry once
- hot-state loss: recover from PostgreSQL and `ProofOfPlay`

This keeps the hot path fast while preserving durable financial correctness in the database layer.

## Flow Summary

1. an advertiser registers a `SessionOffer`
2. wallet funds are frozen for that offer
3. a device asks for the next `N` playback items
4. the service returns confirmed selections with signed receipts
5. the device plays one of them
6. the device sends `Proof of Play`
7. playback evidence is persisted and campaign recency is updated
8. the session later closes and downstream settlement can use the final durable state

## Testing

Run the module test suite with:

```bash
./mvnw test
```
