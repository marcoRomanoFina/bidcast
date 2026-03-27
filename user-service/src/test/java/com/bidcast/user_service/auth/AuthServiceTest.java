package com.bidcast.user_service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidcast.user_service.auth.dto.AuthResponse;
import com.bidcast.user_service.auth.dto.LoginRequest;
import com.bidcast.user_service.auth.dto.UserRegisterRequest;
import com.bidcast.user_service.core.exception.DuplicateResourceException;
import com.bidcast.user_service.core.security.JwtService;
import com.bidcast.user_service.user.User;
import com.bidcast.user_service.user.UserRepository;
import com.bidcast.user_service.user.UserRole;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void register_whenEmailExists_throwsDuplicate() {
        UserRegisterRequest request = new UserRegisterRequest(
                "Test User",
                "test@bidcast.com",
                "secret123",
                Set.of(UserRole.ADVERTISER)
        );
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void register_whenNewUser_returnsToken() {
        UserRegisterRequest request = new UserRegisterRequest(
                "Test User",
                "test@bidcast.com",
                "secret123",
                Set.of(UserRole.ADVERTISER)
        );
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("Test User", saved.getFullName());
        assertEquals("test@bidcast.com", saved.getEmail());
        assertEquals("hashed", saved.getPassword());
        assertEquals(Set.of(UserRole.ADVERTISER), saved.getRoles());
    }

    @Test
    void register_whenSaveFailsWithDataIntegrity_translatesToDuplicate() {
        UserRegisterRequest request = new UserRegisterRequest(
                "Test User",
                "test@bidcast.com",
                "secret123",
                Set.of(UserRole.ADVERTISER)
        );
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        DuplicateResourceException exception =
                assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        assertEquals("Email is already being processed or already exists", exception.getMessage());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void login_whenValidCredentials_returnsToken() {
        LoginRequest request = new LoginRequest("test@bidcast.com", "secret123");
        User user = User.builder()
                .email("test@bidcast.com")
                .password("hashed")
                .roles(Set.of(UserRole.ADVERTISER))
                .build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        assertEquals("jwt-token", response.token());
    }

    @Test
    void login_whenAuthenticatedButUserMissing_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("test@bidcast.com", "secret123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertInstanceOf(BadCredentialsException.class, exception);
        assertEquals("Incorrect email or password", exception.getMessage());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void login_whenInvalidCredentials_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("test@bidcast.com", "wrong");
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        )).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any(User.class));
    }
}
