# Session Service

`session-service` owns the runtime lifecycle of a venue session.
It is the service that knows when a session exists, when it becomes active, which devices are currently participating, and when the session should be closed because no devices remain available.

This service is not a playback decision engine.
It coordinates session state so other services, especially `selection-service`, can operate against a clear session lifecycle.

## Tech Stack

- **Runtime:** Java 21, Spring Boot 4.0.5
- **API:** Spring MVC, Bean Validation
- **Persistence:** Spring Data JPA, Hibernate 7, PostgreSQL
- **Messaging:** RabbitMQ
- **Presence Coordination:** Redis + ShedLock
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Testcontainers support

## Domain Model

### Session

A `Session` represents a live venue runtime window.

It includes:
- `venueId`
- `name`
- `ownerId`
- `basePricePerSlot`
- `status`
- `closedReason`

The `basePricePerSlot` is the minimum economic floor defined by the venue owner for that session.
Other services can use it to reject offers priced below the venue floor.

### Session lifecycle

The current lifecycle is:

- `WAITING_DEVICE`
  the session exists but no device is currently ready to play media
- `ACTIVE`
  at least one device is ready and the session can drive playback
- `CLOSED`
  the session ended, either manually or because no devices remained available

### SessionDevice

A `SessionDevice` models device participation inside one specific session.

It is runtime presence state, not inventory state.
It tells us:

- which `deviceId` joined the session
- whether the device is `READY`, `DISCONNECTED`, or `LEFT`
- the device `lastSeenAt`

## Responsibilities

`session-service` is responsible for:

- creating venue sessions
- enforcing a single open session per venue
- activating a session when the first device becomes ready
- tracking device participation inside the session
- refreshing device presence through heartbeats
- moving active sessions back to `WAITING_DEVICE` when no ready devices remain
- closing idle sessions automatically after a configurable timeout
- notifying `selection-service` when a session becomes active
- publishing `SessionClosedEvent` through an outbox-backed flow

## Session Activation Model

Session creation does not activate playback immediately.

The flow is:

1. a user creates a session
2. the session is persisted in `WAITING_DEVICE`
3. a device sends `ready`
4. if it is the first ready device, the session becomes `ACTIVE`
5. `session-service` notifies `selection-service`
6. additional devices simply join the already active session

This avoids activating playback sessions that never actually receive a live device.

## Presence Model

Device presence is heartbeat-based.

The service exposes:

- `ready`
  the device joins the session and can activate it
- `heartbeat`
  refreshes `lastSeenAt` and keeps the device alive
- `leave`
  explicit clean exit from the session

If a device disappears without sending `leave`, the cleanup job eventually marks it as stale based on `lastSeenAt`.

## Presence Cleanup

Presence cleanup is local to `session-service`.
It does not call `venue-service`.

The cleanup job does this:

1. find `READY` devices whose `lastSeenAt` is older than the stale timeout
2. mark them `DISCONNECTED`
3. if an `ACTIVE` session now has zero `READY` devices, move it back to `WAITING_DEVICE`
4. find `WAITING_DEVICE` sessions that stayed idle longer than the configured timeout
5. close them with `closedReason = NO_DEVICES`
6. publish `SessionClosedEvent`

The job is coordinated with ShedLock so only one worker performs the cleanup cycle at a time.

## Current Presence Defaults

The current defaults are:

- heartbeat interval: `15s`
- device stale after: `60s`
- close empty session after: `5m`
- cleanup interval: `30s`

These values are configurable per environment.

## Integration with Selection Service

When the first device activates a session, `session-service` performs a synchronous notification to `selection-service`.

That notification includes:

- `sessionId`
- `venueId`
- `ownerId`
- `basePricePerSlot`

This allows `selection-service` to create its own local active-session context and enforce playback rules such as the minimum session price floor.

When a session closes, `session-service` publishes `SessionClosedEvent` asynchronously through RabbitMQ using the outbox pattern.

## Outbox and Failure Model

Session closure events are not published directly inside the transaction.

Instead:

- the session state is persisted
- the domain event is stored in the outbox table
- a relay publishes it later to RabbitMQ

This avoids the classic inconsistency where the database commit succeeds but broker publication fails.

## API Endpoints

Base path: `/api/v1/sessions`

- `POST /api/v1/sessions`
  create a new session in `WAITING_DEVICE`
- `POST /api/v1/sessions/{sessionId}/devices/ready`
  mark a device as ready and activate the session if needed
- `POST /api/v1/sessions/{sessionId}/devices/{deviceId}/heartbeat`
  refresh device presence
- `POST /api/v1/sessions/{sessionId}/devices/{deviceId}/leave`
  mark device exit from the session
- `POST /api/v1/sessions/{sessionId}/close`
  close the session manually

## Testing

Run the module test suite with:

```bash
mvn -f session-service/pom.xml test
```
