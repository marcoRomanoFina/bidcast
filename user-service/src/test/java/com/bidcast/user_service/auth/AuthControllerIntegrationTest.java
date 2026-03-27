package com.bidcast.user_service.auth;

import com.bidcast.user_service.auth.dto.LoginRequest;
import com.bidcast.user_service.auth.dto.UserRegisterRequest;
import com.bidcast.user_service.user.UserRepository;
import com.bidcast.user_service.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "application.security.jwt.secret-key=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=",
        "application.security.jwt.expiration=60000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Debe registrar un usuario correctamente y devolver 200 OK")
    void shouldRegisterUserSuccessfully() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest(
                "Nuevo Usuario",
                "nuevo@bidcast.com",
                "Password123!",
                Set.of(UserRole.ADVERTISER)
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findByEmail("nuevo@bidcast.com").isPresent());
    }

    @Test
    @DisplayName("Debe rechazar el registro si el payload es inválido y devolver errores por campo")
    void shouldFailRegistrationWithInvalidData() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest(
                "",
                "correo-roto",
                "123",
                Set.of()
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fullName").value("Full name is required"))
                .andExpect(jsonPath("$.email").value("Email format is invalid"))
                .andExpect(jsonPath("$.password").value("Password must be at least 6 characters long"))
                .andExpect(jsonPath("$.roles").value("You must select at least one role"));
    }

    @Test
    @DisplayName("Debe devolver 409 Conflict si el email ya existe")
    void shouldRejectRegistrationWhenEmailAlreadyExists() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest(
                "Nuevo Usuario",
                "duplicado@bidcast.com",
                "Password123!",
                Set.of(UserRole.ADVERTISER)
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email is already registered in Bidcast"));
    }

    @Test
    @DisplayName("Debe loguear correctamente a un usuario existente")
    void shouldLoginUserSuccessfully() throws Exception {
        UserRegisterRequest registerRequest = new UserRegisterRequest(
                "Usuario Login",
                "login@bidcast.com",
                "SuperSeguro99",
                Set.of(UserRole.ADVERTISER)
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest("login@bidcast.com", "SuperSeguro99");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Debe devolver 401 si las credenciales son inválidas")
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        UserRegisterRequest registerRequest = new UserRegisterRequest(
                "Usuario Login",
                "login-fail@bidcast.com",
                "SuperSeguro99",
                Set.of(UserRole.ADVERTISER)
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest("login-fail@bidcast.com", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Incorrect email or password"));
    }

    @Test
    @DisplayName("Debe rechazar el login si el payload es inválido")
    void shouldRejectLoginWithInvalidPayload() throws Exception {
        LoginRequest loginRequest = new LoginRequest("correo-roto", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email format is invalid"))
                .andExpect(jsonPath("$.password").value("Password is required"));
    }
}
