package com.bidcast.gateway_service.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

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
        Optional<Claims> claimsOptional = jwtValidator.validateAndExtract(token);

        // THEN
        assertTrue(claimsOptional.isPresent());
        assertEquals("test-user", claimsOptional.get().getSubject());
    }

    @Test
    void shouldReturnEmptyWhenTokenIsExpired() {
        // GIVEN
        String expiredToken = createToken("expired-user", -60000); // Expired 1 minute ago

        // WHEN
        Optional<Claims> claimsOptional = jwtValidator.validateAndExtract(expiredToken);

        // THEN
        assertTrue(claimsOptional.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenTokenIsInvalid() {
        // GIVEN
        String invalidToken = "invalid.token.string";

        // WHEN
        Optional<Claims> claimsOptional = jwtValidator.validateAndExtract(invalidToken);

        // THEN
        assertTrue(claimsOptional.isEmpty());
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
