package com.bidcast.user_service.auth;

import com.bidcast.user_service.auth.dto.LoginRequest;
import com.bidcast.user_service.auth.dto.UserRegisterRequest;
import com.bidcast.user_service.user.UserRole;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.bidcast.user_service.auth.dto.AuthResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthControllerIT {
        
@SuppressWarnings("resource")
@Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("application.security.jwt.secret-key", () -> "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");
        registry.add("application.security.jwt.expiration", () -> "60000");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_then_login_returns_token() {
        UserRegisterRequest register = new UserRegisterRequest(
                "Test User",
                "test@bidcast.com",
                "secret123",
                Set.of(UserRole.ADVERTISER)
        );

        ResponseEntity<AuthResponse> registerResponse =
                restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertTrue(registerResponse.getBody().token().length() > 10);

        LoginRequest login = new LoginRequest("test@bidcast.com", "secret123");
        ResponseEntity<AuthResponse> loginResponse =
                restTemplate.postForEntity("/api/auth/login", login, AuthResponse.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().token().length() > 10);
    }

    @Test
    void register_duplicate_returns_409() {
        UserRegisterRequest register = new UserRegisterRequest(
                "Test User",
                "duplicate@bidcast.com",
                "secret123",
                Set.of(UserRole.ADVERTISER)
        );

        restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);

        ResponseEntity<JsonNode> duplicateResponse =
                restTemplate.postForEntity("/api/auth/register", register, JsonNode.class);

        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
    }

    @Test
    void login_with_invalid_credentials_returns_401() {
        UserRegisterRequest register = new UserRegisterRequest(
                "Test User",
                "wrongpass@bidcast.com",
                "secret123",
                Set.of(UserRole.ADVERTISER)
        );
        restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);

        LoginRequest login = new LoginRequest("wrongpass@bidcast.com", "badpass");
        ResponseEntity<JsonNode> loginResponse =
                restTemplate.postForEntity("/api/auth/login", login, JsonNode.class);

        assertEquals(HttpStatus.UNAUTHORIZED, loginResponse.getStatusCode());
    }
}
