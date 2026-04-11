# Gateway Service: Reactive Entry Point and Edge Security

The `gateway-service` is the infrastructure component responsible for centralizing incoming traffic into the Adcast ecosystem. It acts as a high-performance edge server, managing perimeter security, dynamic routing, and flow control (rate limiting) to protect the internal microservice topology.

## Service Responsibilities

1.  **Dynamic and reactive routing:** orchestration of requests toward internal microservices (`user-service`, `selection-service`, `billing-service`, etc.) through a non-blocking architecture.
2.  **Perimeter security (Edge Auth):** centralized JWT validation and user identity shielding before propagating the request.
3.  **Header normalization:** systematic removal of sensitive externally supplied headers and injection of verified identity (`X-User-Id`, `X-User-Email`, `X-User-Roles`) for safe internal consumption.
4.  **Traffic control (Rate Limiting):** implementation of distributed request limits to mitigate denial-of-service (DoS) attacks and API abuse.
5.  **CORS management:** centralized configuration of cross-origin resource sharing policies for Web and Mobile clients.

## Technical Implementation and Security

### Non-blocking architecture (WebFlux)
The service is built on **Spring Cloud Gateway**, leveraging the reactive Project Reactor stack. This allows it to handle thousands of concurrent connections with minimal resource usage, making it well-suited as a highly available entry point.

### Identity shielding pattern
The `AuthenticationFilter` implements a "Zero Trust" strategy for external requests:
*   **Interception:** All requests to protected routes require an `Authorization: Bearer <JWT>` header.
*   **Stateless validation:** Use of the **JJWT** library to verify token integrity and expiration without database queries on the critical path.
*   **Sanitization:** Client-injected headers that attempt identity spoofing are stripped out, ensuring user information only comes from the validated JWT.
*   **Identity completeness:** requests are only forwarded when the JWT includes a valid `userId` claim, preventing partially-authenticated identity propagation.

```java
@Override
public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
        String path = exchange.getRequest().getURI().getPath();
        ServerWebExchange cleanedExchange = cleanRequest(exchange);

        return extractToken(exchange.getRequest())
                .flatMap(jwtValidator::validateAndExtract)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token")))
                .flatMap(claims -> validateRoles(claims, config, path))
                .flatMap(claims -> enrichExchange(cleanedExchange, claims))
                .flatMap(chain::filter)
                .onErrorResume(ex -> {
                    HttpStatus status = HttpStatus.UNAUTHORIZED;
                    if (ex instanceof ResponseStatusException rse) {
                        status = (HttpStatus) rse.getStatusCode();
                    }
                    return onError(cleanedExchange, status);
                });
    };
}
```

At this point, the gateway fulfills two critical roles: it authenticates at the edge and propagates verified identity to internal microservices through `X-User-Email`, `X-User-Id`, and `X-User-Roles`.

### Route-level role validation
The filter supports route-configurable RBAC, allowing required roles to be declared directly in the route definition. It also accepts roles both with and without the `ROLE_` prefix, tolerating different claim formats.

```java
private Mono<Claims> validateRoles(Claims claims, Config config, String path) {
    if (config.getRequiredRoles().isEmpty()) {
        return Mono.just(claims);
    }

    List<String> userRoles = getUserRoles(claims);
    boolean hasRole = config.getRequiredRoles().stream()
            .anyMatch(role -> userRoles.contains("ROLE_" + role.toUpperCase()) || userRoles.contains(role.toUpperCase()));

    if (!hasRole) {
        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient roles"));
    }

    return Mono.just(claims);
}
```

### Distributed rate limiting
The gateway uses the **token bucket** algorithm backed by **Redis**. This guarantees consistent request quotas even in horizontal deployments (multiple gateway instances), because limiter state is shared globally.

The current selection route configuration applies `RequestRateLimiter` on top of a `KeyResolver` based on `X-User-Id`, which allows limiting by authenticated user instead of by IP.

```properties
spring.cloud.gateway.server.webflux.routes[4].id=selection-service
spring.cloud.gateway.server.webflux.routes[4].uri=http://selection-service:8084
spring.cloud.gateway.server.webflux.routes[4].predicates[0]=Path=/api/v1/selection/**, /api/v1/session-offers/**
spring.cloud.gateway.server.webflux.routes[4].filters[0]=AuthenticationFilter=PUBLISHER,ADMIN
spring.cloud.gateway.server.webflux.routes[4].filters[1].name=RequestRateLimiter
spring.cloud.gateway.server.webflux.routes[4].filters[1].args.redis-rate-limiter.replenishRate=50
spring.cloud.gateway.server.webflux.routes[4].filters[1].args.redis-rate-limiter.burstCapacity=100
spring.cloud.gateway.server.webflux.routes[4].filters[1].args.key-resolver=#{@userKeyResolver}
```

The associated `KeyResolver` prioritizes the authenticated user identifier and uses the remote IP as a fallback when no identity is propagated:

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> Mono.just(
            Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                    .orElse(Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                            .map(addr -> addr.getAddress().getHostAddress())
                            .orElse("anonymous"))
    );
}
```

## Tech Stack

*   **Runtime:** Java 21, Spring Boot 4.0.3.
*   **Gateway framework:** Spring Cloud Gateway Server WebFlux (Spring Cloud 2025.1.1).
*   **Server:** Netty (non-blocking thread architecture).
*   **Security:** JJWT (JSON Web Token).
*   **Distributed state:** Redis (Spring Data Redis Reactive).

## Testing

The testing strategy combines unit tests for narrow security branches and integration tests to validate the full gateway running with Spring Boot, WebFlux, and real Redis via Testcontainers.

### Current coverage

*   **`AuthenticationFilterTest`:** covers token extraction, rejection of malformed `Authorization` headers, missing token, invalid token, role validation, claims without `roles`, spoofed header sanitization, and injection of validated identity.
*   **CORS configuration:** allowed origins are configurable instead of globally open, while local development keeps sane defaults for common frontend ports.
*   **`JwtValidatorTest`:** covers valid token, expired token, and invalid token against the configured JWT key.
*   **`GatewaySecurityIntegrationTest`:** covers public and protected routes, end-to-end `401`, `403` for RBAC, propagation of `X-User-Id`, `X-User-Email`, and `X-User-Roles`, routes for `users`, `wallets`, `auction`, `campaigns`, and `billing`, plus real route precedence behavior.
*   **`GatewayRateLimiterIntegrationTest`:** covers `RequestRateLimiter` with real Redis, verifying allowed requests within quota and `429 Too Many Requests` once the quota is exceeded.

### Test infrastructure

*   Integration tests use `@SpringBootTest` and `WebTestClient`.
*   Redis is started with Testcontainers using `@ServiceConnection`.
*   Downstream backends are simulated with a lightweight local HTTP server to validate routing, security, and rate limiting without depending on real microservices.
*   Docker Desktop must be running to execute integration tests.

### Running tests

```bash
mvn test
```
