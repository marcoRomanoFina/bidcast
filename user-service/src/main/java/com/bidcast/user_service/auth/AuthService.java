package com.bidcast.user_service.auth;

import com.bidcast.user_service.auth.dto.AuthResponse;
import com.bidcast.user_service.auth.dto.LoginRequest;
import com.bidcast.user_service.auth.dto.UserRegisterRequest;
import com.bidcast.user_service.core.exception.DuplicateResourceException;
import com.bidcast.user_service.core.security.JwtService;
import com.bidcast.user_service.user.User;
import com.bidcast.user_service.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(UserRegisterRequest request) {
        // 1. Validamos que el email no exista con tu nueva excepción
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("El email ya está registrado en BidCast");
        }

        // 2. Armamos el usuario
        var user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(request.roles())
                .build();

        // 3. Guardamos y generamos token
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Spring Security chequea si el mail y la clave coinciden
        // (Si le pifian, esto tira la BadCredentialsException que ataja el pararrayos)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // 2. Si pasó, buscamos el usuario y generamos token
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(); // Acá no debería fallar nunca porque ya pasó la autenticación
        
        var jwtToken = jwtService.generateToken(user);
        
        return new AuthResponse(jwtToken);
    }
}