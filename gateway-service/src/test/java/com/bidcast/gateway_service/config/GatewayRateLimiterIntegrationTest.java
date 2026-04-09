package com.bidcast.gateway_service.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "application.security.jwt.secret-key=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false"
})
@AutoConfigureWebTestClient(timeout = "10000")
@Testcontainers
class GatewayRateLimiterIntegrationTest {

    private static final String SECRET_KEY = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
    private static final AtomicInteger backendHits = new AtomicInteger();
    private static final HttpServer backendServer = startBackendServer();

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        String backendBaseUrl = "http://localhost:" + backendServer.getAddress().getPort();

        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "user-auth-public");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri", () -> backendBaseUrl);
        registry.add("spring.cloud.gateway.server.webflux.routes[0].predicates[0]", () -> "Path=/api/v1/auth/login,/api/v1/users/register");

        registry.add("spring.cloud.gateway.server.webflux.routes[1].id", () -> "user-service-protected");
        registry.add("spring.cloud.gateway.server.webflux.routes[1].uri", () -> backendBaseUrl);
        registry.add("spring.cloud.gateway.server.webflux.routes[1].predicates[0]", () -> "Path=/api/v1/users/**");
        registry.add("spring.cloud.gateway.server.webflux.routes[1].filters[0]", () -> "AuthenticationFilter");

        registry.add("spring.cloud.gateway.server.webflux.routes[2].id", () -> "wallet-service-me");
        registry.add("spring.cloud.gateway.server.webflux.routes[2].uri", () -> backendBaseUrl);
        registry.add("spring.cloud.gateway.server.webflux.routes[2].predicates[0]", () -> "Path=/api/v1/wallets/me");
        registry.add("spring.cloud.gateway.server.webflux.routes[2].predicates[1]", () -> "Method=GET");
        registry.add("spring.cloud.gateway.server.webflux.routes[2].filters[0]", () -> "AuthenticationFilter=ADVERTISER,PUBLISHER,ADMIN");

        registry.add("spring.cloud.gateway.server.webflux.routes[3].id", () -> "wallet-service-internal");
        registry.add("spring.cloud.gateway.server.webflux.routes[3].uri", () -> backendBaseUrl);
        registry.add("spring.cloud.gateway.server.webflux.routes[3].predicates[0]", () -> "Path=/api/v1/wallets/**,/api/v1/proof-of-play-charges/**");
        registry.add("spring.cloud.gateway.server.webflux.routes[3].filters[0]", () -> "AuthenticationFilter=ADMIN");

        registry.add("spring.cloud.gateway.server.webflux.routes[4].id", () -> "selection-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[4].uri", () -> backendBaseUrl);
        registry.add("spring.cloud.gateway.server.webflux.routes[4].predicates[0]", () -> "Path=/api/v1/selection/**,/api/v1/session-offers/**");
        registry.add("spring.cloud.gateway.server.webflux.routes[4].filters[0]", () -> "AuthenticationFilter=PUBLISHER,ADMIN");
        registry.add("spring.cloud.gateway.server.webflux.routes[4].filters[1].name", () -> "RequestRateLimiter");
        registry.add("spring.cloud.gateway.server.webflux.routes[4].filters[1].args.redis-rate-limiter.replenishRate", () -> "1");
        registry.add("spring.cloud.gateway.server.webflux.routes[4].filters[1].args.redis-rate-limiter.burstCapacity", () -> "1");
        registry.add("spring.cloud.gateway.server.webflux.routes[4].filters[1].args.key-resolver", () -> "#{@userKeyResolver}");

        registry.add("spring.cloud.gateway.server.webflux.routes[5].id", () -> "advertisement-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[5].uri", () -> backendBaseUrl);
        registry.add("spring.cloud.gateway.server.webflux.routes[5].predicates[0]", () -> "Path=/api/campaigns/**");
        registry.add("spring.cloud.gateway.server.webflux.routes[5].filters[0]", () -> "AuthenticationFilter=ADVERTISER,ADMIN");

        registry.add("spring.cloud.gateway.server.webflux.routes[6].id", () -> "billing-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[6].uri", () -> backendBaseUrl);
        registry.add("spring.cloud.gateway.server.webflux.routes[6].predicates[0]", () -> "Path=/api/v1/billing/**");
        registry.add("spring.cloud.gateway.server.webflux.routes[6].filters[0]", () -> "AuthenticationFilter=ADVERTISER,ADMIN");
    }

    @BeforeEach
    void resetBackendCounter() {
        backendHits.set(0);
    }

    @AfterAll
    static void stopBackendServer() {
        backendServer.stop(0);
    }

    @Test
    void shouldAllowRequestWithinRateLimit() {
        String token = createToken("publisher@bidcast.com", "publisher-rate-ok", List.of("ROLE_PUBLISHER"));

        webTestClient.get()
                .uri("/api/v1/selection/live")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"x-user-id\":\"publisher-rate-ok\"")));

        org.junit.jupiter.api.Assertions.assertEquals(1, backendHits.get());
    }

    @Test
    void shouldRejectRequestWhenRateLimitIsExceeded() {
        String token = createToken("publisher@bidcast.com", "publisher-rate-blocked", List.of("ROLE_PUBLISHER"));

        webTestClient.get()
                .uri("/api/v1/selection/live")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/v1/selection/live")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(429);

        org.junit.jupiter.api.Assertions.assertEquals(1, backendHits.get());
    }

    private static HttpServer startBackendServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", new EchoHandler());
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start rate limiter test backend", e);
        }
    }

    private static String createToken(String subject, String userId, List<String> roles) {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + Duration.ofMinutes(5).toMillis()))
                .signWith(key)
                .compact();
    }

    private static final class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            backendHits.incrementAndGet();
            String response = """
                    {
                      "path":"%s",
                      "method":"%s",
                      "x-user-id":"%s"
                    }
                    """.formatted(
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("X-User-Id")
            );

            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }
}
