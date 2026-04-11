# User Service: Identity Management and Access Control

The `user-service` is the component responsible for identity management, authentication, and authorization within the Adcast ecosystem. It acts as the centralized Identity Provider, managing profiles for Advertisers, Publishers, and Administrators.

## Tech Stack

*   **Runtime:** Java 21, Spring Boot 4.0.3.
*   **Security:** Spring Security 6.x, JJWT (JSON Web Token).
*   **Persistence:** Spring Data JPA, Hibernate 7, PostgreSQL 16.
*   **Data Mapping:** MapStruct for efficient conversion between entities and DTOs.
*   **Documentation:** OpenAPI 3 (Swagger) for endpoint specification.

### API Documentation
Once the service is started, the interactive documentation is available at:
*   **Swagger UI:** `http://localhost:8081/swagger-ui.html`


## Service Responsibilities

1.  **User lifecycle management:** registration, updates, and auditing of user profiles with differentiated roles.
2.  **Authentication and token issuance:** validation of credentials and generation of JSON Web Tokens (JWT) to secure asynchronous requests.
3.  **Role-Based Access Control (RBAC):** definition of permissions and access levels based on the actor type in the platform.
4.  **Credential integrity:** implementation of secure hashing algorithms for password storage.

## Technical Implementation and Security

The service currently exposes two public endpoints, `/login` and `/register`, responsible for user authentication and registration respectively.

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

### Stateless Authentication (JWT)
The service uses a stateless authentication model to support horizontal scalability:
*   **Issuance:** After successful authentication, the service emits a JWT signed with a configured secret key (HMAC-SHA256).
```java
public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(); 
        
        var jwtToken = jwtService.generateToken(user);
        
        return new AuthResponse(jwtToken);
    }
```

If authentication succeeds but the user record is no longer available, the service still returns the same `401 Unauthorized` contract used for invalid credentials instead of leaking an internal inconsistency as a `500`.


*   **Content:** The token includes standard claims and custom claims (roles and user metadata) required by the `gateway-service` to perform protected routing.

```java

    /**
     * Generates a new JWT token from the User entity.
     * Automatically includes 'userId' and the list of 'roles' prefixed with ROLE_.
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("roles", user.getRoles().stream()
                .map(Enum::name)
                .map(role -> "ROLE_" + role)
                .collect(java.util.stream.Collectors.toList()));
        return generateToken(extraClaims, user);
    }

    /**
     * Base method for JWT token construction.
     * Defines subject, issued-at date, expiration, and HS256 signature.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256) 
                .compact();
    }
```

## User Entity and Endpoints

At the moment, there is no meaningful endpoint implementation associated with `UserController`.
These endpoints will be needed in the future for different user lookups and information retrieval use cases.

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",           
        joinColumns = @JoinColumn(name = "user_id") 
    )
    @Enumerated(EnumType.STRING)        
    @Column(name = "role")  
    @Builder.Default            
    private Set<UserRole> roles = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false) 
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
 // ...and the remaining UserDetails fields
```



## Testing

The `user-service` test suite currently covers the critical authentication and registration flows at two levels:

### Unit Tests

`AuthService` unit tests validate the internal service logic without booting Spring:

*   Successful registration of a new user.
*   Rejection of registration when the email already exists.
*   Translation of integrity errors (`DataIntegrityViolationException`) into `DuplicateResourceException`.
*   Successful login with valid credentials.
*   Rejection of login with invalid credentials.
*   Edge case where authentication succeeds but the user no longer exists in the repository, still returning the same safe authentication error contract.

### Integration Tests

`AuthController` integration tests boot the full Spring Boot context and use real PostgreSQL through Testcontainers:

*   Successful HTTP registration and real database persistence.
*   Validation of invalid payloads on `/api/auth/register` with field-level errors.
*   Rejection of duplicate registration with `409 Conflict`.
*   Successful HTTP login with JWT issuance.
*   Rejection of login with invalid credentials with `401 Unauthorized`.
*   Validation of invalid payloads on `/api/auth/login`.

### Test requirements

To execute the full suite, Docker Desktop or Docker Engine must be running, since the integration tests use Testcontainers to initialize disposable PostgreSQL.

```bash
mvn test
```

If you only want to run the module:

```bash
cd user-service
mvn test
```
