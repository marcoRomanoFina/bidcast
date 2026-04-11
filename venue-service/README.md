# Venue Service: Venues and Devices Management

The `venue-service` is the microservice responsible for managing venues and the physical devices associated with each venue in the Adcast platform. Its responsibilities are to register venues, group devices under a venue, expose CRUD-style operations for both aggregates, and resolve queries by venue or owner so other services can locate deployed assets.

## Tech Stack

*   **Runtime:** Java 21, Spring Boot 4.0.3.
*   **API:** Spring MVC, Bean Validation.
*   **Persistence:** Spring Data JPA, Hibernate 7, PostgreSQL 16.
*   **Observability:** Logback JSON encoder, Micrometer Tracing.
*   **Testing:** JUnit 5, Mockito, MockMvc, H2 integration tests.

## Service Responsibilities

1.  **Venue registration:** creation of venues associated with an `ownerId`.
2.  **Device registration:** creation of devices associated with a `venueId`.
3.  **Lookup by identifier:** direct retrieval of a venue or device by `UUID`.
4.  **Lookup by owner and venue:** listing venues for an owner and devices for a venue.
5.  **Simple logical removal:** deletion of devices by identifier.
6.  **Payload validation and HTTP errors:** consistent responses for validation failures and missing resources.
7.  **Path parameter hardening:** invalid UUIDs in route parameters are rejected with explicit `400 Bad Request` responses.

## Technical Implementation

### Controller endpoints

The controller exposes a small and direct REST API under `/devices`, delegating logic to the service layer and using annotation-based validation on the request DTO.

```java
@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping()
    @ResponseStatus(value = HttpStatus.CREATED)
    public DeviceResponse createDevice(@Valid @RequestBody CreateDeviceRequest request){
        return deviceService.createDevice(request);
    }

    @GetMapping("/{id}")
    public DeviceResponse getDeviceById(@PathVariable UUID id){
        return deviceService.getDeviceById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeviceById(@PathVariable UUID id){
        deviceService.deleteDevice(id);
    }

    @GetMapping("/owner/{ownerId}")
    public List<DeviceResponse> getDevicesByOwner(@PathVariable UUID ownerId) {
        return deviceService.getDevicesByOwner(ownerId);
    }
}
```

### Service Logic

The service encapsulates repository interaction, centralizes operational logs, and translates missing data into a domain exception (`DeviceNotFoundException`).

```java
public DeviceResponse getDeviceById(UUID deviceId){
    log.info("Searching device by ID. DeviceID: {}", deviceId);
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> {
            log.warn("Device not found. DeviceID: {}", deviceId);
            return new DeviceNotFoundException("Device not found: " + deviceId);
        });
    log.info("Device found. DeviceID: {}, OwnerID: {}", device.getId(), device.getOwnerId());
    return DeviceMapper.toResponse(device);
}
```

The creation operation transforms the input DTO into a persistable entity and returns a response decoupled from the JPA model:

```java
public DeviceResponse createDevice(CreateDeviceRequest request){
    log.info("Starting device creation. OwnerID: {}, Name: {}", request.getOwnerId(), request.getDeviceName());
    Device device = DeviceMapper.fromCreateRequest(request);
    Device saved = deviceRepository.save(device);
    log.info("Device created. DeviceID: {}, OwnerID: {}", saved.getId(), saved.getOwnerId());
    return DeviceResponse.builder()
        .id(saved.getId())
        .ownerId(saved.getOwnerId())
        .deviceName(saved.getDeviceName())
        .createdAt(saved.getCreatedAt())
        .updatedAt(saved.getUpdatedAt())
        .build();
}
```

### Entity and Mapping

The `Device` entity persists the relationship with the owner, the device name, and automatic timestamps managed by Hibernate.

```java
@Entity
@Table(name = "Devices")
@Data
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    @NotNull(message = "owner ID is required")
    private UUID ownerId;

    @Column(name = "device_name", nullable = false, length = 100)
    @NotBlank(message = "device name must not be blank")
    @Size(min = 3, max = 100, message = "device name must be between 3 and 100 characters")
    private String deviceName;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

The mapper keeps an explicit conversion between DTOs and the entity:

```java
public static Device fromCreateRequest(CreateDeviceRequest request) {
    return Device.builder()
        .ownerId(request.getOwnerId())
        .deviceName(request.getDeviceName())
        .build();
}

public static DeviceResponse toResponse(Device device) {
    return DeviceResponse.builder()
        .id(device.getId())
        .ownerId(device.getOwnerId())
        .deviceName(device.getDeviceName())
        .createdAt(device.getCreatedAt())
        .updatedAt(device.getUpdatedAt())
        .build();
}
```

### Error Handling

The service responds with consistent HTTP errors through a `@RestControllerAdvice`, covering validation failures and missing resources.

```java
@ExceptionHandler(DeviceNotFoundException.class)
public ResponseEntity<ErrorResponse> deviceNotFoundHandler(DeviceNotFoundException ex){
    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        ex.getMessage()
    );
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
}
```

It also normalizes malformed route/body inputs into `400 Bad Request`, for example when a client sends an invalid UUID in `/devices/{id}`.

## Testing

The `venue-service` suite is divided into unit tests, controller web tests, and integration tests with an in-memory database.

### Current coverage

*   **`DeviceMapperTest`:** validates the mapping from `CreateDeviceRequest -> Device` and from `Device -> DeviceResponse`.
*   **`DeviceServiceTest`:** covers creation, lookup by ID, lookup by venue, deletion, and errors for missing devices.
*   **`DeviceControllerTest`:** covers `POST`, `GET`, `DELETE`, HTTP validations, and `404` errors using `MockMvc` with `@WebMvcTest`.
*   **`DeviceControllerIntegrationTest`:** boots the full Spring Boot context with H2, verifying real persistence, queries, deletion, validations, invalid UUID handling, and HTTP error handling.
*   **`VenueServiceTest` and `VenueControllerTest`:** cover venue creation and venue lookups with the new enum-based category model.

### Test infrastructure

*   Unit and web tests run without a real database.
*   Integration tests use `@SpringBootTest` + `@AutoConfigureMockMvc`.
*   The integration profile uses an in-memory H2 database in PostgreSQL compatibility mode.
*   Docker is no longer required to execute the full suite locally.

### Running tests

```bash
mvn test
```

If you only want to run this module:

```bash
cd venue-service
mvn test
```
