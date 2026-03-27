package com.bidcast.gateway_service.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private static final String SECRET_KEY = "2UHjkWZU0VNZZeDuGsBZdmYWk8zBbSodnpk/WhB3zOY="; // Same as application.properties
    private JwtValidator jwtValidator;

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator(SECRET_KEY);
    }

    @Test
    void shouldValidateAndExtractClaimsFromValidToken() {
        // GIVEN
        String token = createToken("test-user", 60000); // 1 minute expiry

        // WHEN
        Mono<Claims> result = jwtValidator.validateAndExtract(token);

        // THEN
        StepVerifier.create(result)
                .assertNext(claims -> assertEquals("test-user", claims.getSubject()))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenTokenIsExpired() {
        // GIVEN
        String expiredToken = createToken("expired-user", -60000); // Expired 1 minute ago

        // WHEN
        Mono<Claims> result = jwtValidator.validateAndExtract(expiredToken);

        // THEN
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenTokenIsInvalid() {
        // GIVEN
        String invalidToken = "invalid.token.string";

        // WHEN
        Mono<Claims> result = jwtValidator.validateAndExtract(invalidToken);

        // THEN
        StepVerifier.create(result)
                .verifyComplete();
    }

    private String createToken(String subject, long expirationMillis) {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }
}
