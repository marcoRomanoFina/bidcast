# Advertisement Service: Advertising Campaign Management

The `advertisement-service` is the microservice responsible for managing advertising campaigns within the Bidcast ecosystem. In its current state, it focuses on campaign creation, persistence of the main campaign metadata, and linking each campaign to the authenticated advertiser who created it.

## Tech Stack

*   **Runtime:** Java 21, Spring Boot 4.0.3.
*   **API:** Spring MVC, Bean Validation.
*   **Persistence:** Spring Data JPA, Hibernate 7, PostgreSQL 16.
*   **Testing:** JUnit 5, Mockito, MockMvc, RestTestClient, Testcontainers.

## Service Responsibilities

1.  **Campaign creation:** creation of new campaigns with initial status `DRAFT`.
2.  **Authenticated advertiser association:** use of the identity propagated by the gateway to set `advertiserId`.
3.  **Payload validation:** rejection of invalid requests before persistence.
4.  **Initial state persistence:** consistent initialization of `status` and `spent`.

## Technical Implementation

### Controller and Gateway contract

The controller exposes `POST /api/campaigns` and takes the authenticated user identifier from the `X-User-Id` header, which is the trusted header propagated by the `gateway-service` after JWT and role validation.

```java
@PostMapping
public ResponseEntity<Campaign> create(
        @Valid @RequestBody CampaignRequest body,
        @RequestHeader("X-User-Id") UUID advertiserId) {

    log.info("Campaign creation request received: advertiserId={}, name={}", advertiserId, body.name());
    Campaign savedCampaign = campaignService.createCampaign(advertiserId, body);
    return new ResponseEntity<>(savedCampaign, HttpStatus.CREATED);
}
```

This prevents the microservice from having to parse JWTs on its own and keeps a simple contract with the edge: the gateway authorizes, and the microservice persists using the already verified identity.

### Service Logic

The current business logic creates the campaign in `DRAFT` status, associates the authenticated advertiser, and initializes the `spent` amount to zero.

```java
@Transactional
public Campaign createCampaign(UUID advertiserId, CampaignRequest data) {
    log.info("Creating new campaign in DRAFT status: {}", data.name());

    Campaign campaign = Campaign.builder()
            .name(data.name())
            .advertiserId(advertiserId)
            .budget(data.budget())
            .bidCpm(data.bidCpm())
            .status(CampaignStatusType.DRAFT)
            .build();

    Campaign saved = campaignRepository.save(campaign);
    log.info("Draft saved successfully with ID: {}", saved.getId());

    return saved;
}
```

### Campaign Entity

The `Campaign` entity models budget, CPM bid, owning advertiser, status, and automatic timestamps. It also leaves the relationship with creatives (`Creative`) ready for future extensions of the module.

```java
@Entity
@Table(name = "campaigns")
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private UUID advertiserId;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal budget;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal spent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal bidCpm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatusType status;
}
```

### Validation and error handling

The request DTO validates name, budget, and bid before reaching the service layer. Failed validations are transformed into a `Map<String, String>` so the client can identify exactly which field failed.

```java
public record CampaignRequest(
    @NotBlank(message = "Name must not be blank")
    String name,

    @NotNull @Positive BigDecimal budget,
    @NotNull @Positive BigDecimal bidCpm
) {}
```

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleValidations(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage())
    );

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errors);
}
```

## Testing

The `advertisement-service` test suite combines unit tests, controller MVC tests, and integration tests with a real PostgreSQL instance.

### Current coverage

*   **`CampaignServiceTest`:** validates that the campaign is built with the correct fields, uses the `advertiserId` propagated through the header, and persists `status=DRAFT` and `spent=0`.
*   **`CampaignControllerTest`:** covers `201 Created` on the happy path, `400 Bad Request` for invalid payloads, and `400` when the `X-User-Id` header is missing.
*   **`CampaignIntegrationTest`:** boots the full Spring Boot context with real PostgreSQL via Testcontainers, verifying real database persistence, correct `advertiserId`, initial status, initial zero spend, HTTP validations, and lack of persistence for invalid requests.

### Test infrastructure

*   Unit tests run with Mockito.
*   Web tests use `@WebMvcTest` and `MockMvc`.
*   Integration tests use `@SpringBootTest`, `RestTestClient`, and PostgreSQL with Testcontainers through `@ServiceConnection`.
*   Docker Desktop or Docker Engine must be running to execute the full suite.

### Running tests

```bash
mvn test
```

If you only want to run this module:

```bash
cd advertisement-service
mvn test
```
