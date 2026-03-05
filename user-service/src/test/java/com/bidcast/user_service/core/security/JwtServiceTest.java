package com.bidcast.user_service.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.JwtException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Test
    void generateToken_and_extractUsername() {
        UserDetails user = new User("test@bidcast.com", "pw", Collections.emptyList());
        String token = jwtService.generateToken(user);

        assertEquals("test@bidcast.com", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_whenValid_returnsTrue() {
        UserDetails user = new User("test@bidcast.com", "pw", Collections.emptyList());
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_whenExpired_throws() {
        UserDetails user = new User("test@bidcast.com", "pw", Collections.emptyList());
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        String token = jwtService.generateToken(user);

        assertThrows(JwtException.class, () -> jwtService.isTokenValid(token, user));
    }

    @Test
    void extractUsername_whenSignatureInvalid_throws() {
        UserDetails user = new User("test@bidcast.com", "pw", Collections.emptyList());
        String token = jwtService.generateToken(user);

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secretKey", SECRET_2);
        ReflectionTestUtils.setField(otherService, "jwtExpiration", 60_000L);

        assertThrows(JwtException.class, () -> otherService.extractUsername(token));
    }
}
