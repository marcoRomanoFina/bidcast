package com.bidcast.gateway_service.config.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtValidator jwtValidator;

    @Mock
    private GatewayFilterChain filterChain;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter(jwtValidator);
        lenient().when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void shouldAllowPublicPath() {
        // GIVEN
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build()
        );

        // WHEN
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(any(ServerWebExchange.class));
        verifyNoInteractions(jwtValidator);
    }

    @Test
    void shouldDenyProtectedPathWithoutToken() {
        // GIVEN
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/wallets/me").build()
        );

        // WHEN
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowProtectedPathWithValidToken() {
        // GIVEN
        String token = "valid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "malicious-id") // Header que debe ser limpiado
                        .build()
        );

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("test@user.com");
        when(claims.get("userId", String.class)).thenReturn("12345");
        when(jwtValidator.validateAndExtract(token)).thenReturn(Optional.of(claims));

        // WHEN
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(filterChain).filter(captor.capture());
        
        ServerWebExchange capturedExchange = captor.getValue();
        assertEquals("test@user.com", capturedExchange.getRequest().getHeaders().getFirst("X-User-Email"));
        assertEquals("12345", capturedExchange.getRequest().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    void shouldDenyProtectedPathWithInvalidToken() {
        // GIVEN
        String token = "invalid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        when(jwtValidator.validateAndExtract(token)).thenReturn(Optional.empty());

        // WHEN
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }
}
