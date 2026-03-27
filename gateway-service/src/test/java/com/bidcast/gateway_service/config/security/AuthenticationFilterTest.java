package com.bidcast.gateway_service.config.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

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
    void shouldDenyProtectedPathWithoutToken() {
        // GIVEN
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        GatewayFilter filter = authenticationFilter.apply(config);
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/wallets/me").build()
        );

        // WHEN
        Mono<Void> result = filter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowWhenRoleMatches() {
        // GIVEN
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        config.setRequiredRoles(List.of("ADVERTISER"));
        GatewayFilter filter = authenticationFilter.apply(config);

        String token = "valid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("advertiser@test.com");
        when(claims.get("userId", String.class)).thenReturn("adv-123");
        when(claims.get("roles")).thenReturn(List.of("ROLE_ADVERTISER"));
        when(jwtValidator.validateAndExtract(token)).thenReturn(Mono.just(claims));

        // WHEN
        Mono<Void> result = filter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldDenyWhenRoleIsInsufficient() {
        // GIVEN
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        config.setRequiredRoles(List.of("ADMIN"));
        GatewayFilter filter = authenticationFilter.apply(config);

        String token = "valid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        Claims claims = mock(Claims.class);
        when(claims.get("roles")).thenReturn(List.of("ROLE_ADVERTISER"));
        when(jwtValidator.validateAndExtract(token)).thenReturn(Mono.just(claims));

        // WHEN
        Mono<Void> result = filter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldInjectIdentityHeadersOnSuccess() {
        // GIVEN
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        GatewayFilter filter = authenticationFilter.apply(config);

        String token = "valid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@test.com");
        when(claims.get("userId", String.class)).thenReturn("user-999");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));
        when(jwtValidator.validateAndExtract(token)).thenReturn(Mono.just(claims));

        // WHEN
        Mono<Void> result = filter.filter(exchange, filterChain);

        // THEN
        StepVerifier.create(result).verifyComplete();
        
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(filterChain).filter(captor.capture());
        
        ServerWebExchange capturedExchange = captor.getValue();
        assertEquals("user@test.com", capturedExchange.getRequest().getHeaders().getFirst("X-User-Email"));
        assertEquals("user-999", capturedExchange.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("ROLE_USER", capturedExchange.getRequest().getHeaders().getFirst("X-User-Roles"));
    }

    @Test
    void shouldDenyWhenAuthorizationHeaderIsMalformed() {
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        GatewayFilter filter = authenticationFilter.apply(config);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/profile")
                        .header("Authorization", "Basic abc123")
                        .build()
        );

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
        verifyNoInteractions(jwtValidator);
    }

    @Test
    void shouldDenyWhenValidatorReturnsEmpty() {
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        GatewayFilter filter = authenticationFilter.apply(config);

        String token = "invalid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        when(jwtValidator.validateAndExtract(token)).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowRoleWithoutPrefixWhenConfigMatches() {
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        config.setRequiredRoles(List.of("ADMIN"));
        GatewayFilter filter = authenticationFilter.apply(config);

        String token = "valid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("admin@test.com");
        when(claims.get("userId", String.class)).thenReturn("admin-1");
        when(claims.get("roles")).thenReturn(List.of("ADMIN"));
        when(jwtValidator.validateAndExtract(token)).thenReturn(Mono.just(claims));

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldDenyWhenRolesClaimIsMissing() {
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        config.setRequiredRoles(List.of("ADMIN"));
        GatewayFilter filter = authenticationFilter.apply(config);

        String token = "valid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        Claims claims = mock(Claims.class);
        when(claims.get("roles")).thenReturn(null);
        when(jwtValidator.validateAndExtract(token)).thenReturn(Mono.just(claims));

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldDenyWhenUserIdClaimIsMissing() {
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        GatewayFilter filter = authenticationFilter.apply(config);

        String token = "valid-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );

        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(null);
        when(jwtValidator.validateAndExtract(token)).thenReturn(Mono.just(claims));

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }
}
