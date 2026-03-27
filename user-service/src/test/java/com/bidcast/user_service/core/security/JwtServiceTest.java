package com.bidcast.user_service.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bidcast.user_service.user.User;
import com.bidcast.user_service.user.UserRole;
import io.jsonwebtoken.JwtException;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET_1 = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
    private static final String SECRET_2 = "NDMyMTA5ODc2NTQzMjEwOTg3NjU0MzIxMDk4NzY1NDMyMTA=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_1);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    private User createDomainUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@bidcast.com")
                .password("pw")
                .roles(Set.of(UserRole.ADVERTISER))
                .build();
    }

    @Test
    void generateToken_and_extractUsername() {
        User user = createDomainUser();
        String token = jwtService.generateToken(user);

        assertEquals("test@bidcast.com", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_whenValid_returnsTrue() {
        User user = createDomainUser();
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_whenExpired_throws() {
        User user = createDomainUser();
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        String token = jwtService.generateToken(user);

        assertThrows(JwtException.class, () -> jwtService.isTokenValid(token, user));
    }

    @Test
    void extractUsername_whenSignatureInvalid_throws() {
        User user = createDomainUser();
        String token = jwtService.generateToken(user);

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secretKey", SECRET_2);
        ReflectionTestUtils.setField(otherService, "jwtExpiration", 60_000L);

        assertThrows(JwtException.class, () -> otherService.extractUsername(token));
    }
}
