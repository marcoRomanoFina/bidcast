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
import org.springframework.security.authentication.BadCredentialsException;
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
        // 1. Validación de existencia de Mail
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("Email is already registered in Bidcast");
        }

        try {
            // 2. Armado del usuario
            var user = User.builder()
                    .fullName(request.fullName())
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .roles(request.roles())
                    .build();

            // 3. Guardado y generación token
            user = userRepository.save(user);
            var jwtToken = jwtService.generateToken(user);
            
            return new AuthResponse(jwtToken);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Captura atómica si el pre-check falló por concurrencia
            throw new DuplicateResourceException("Email is already being processed or already exists");
        }
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
        // Si el usuario desapareció entre la autenticación y la lectura, devolvemos el mismo
        // contrato que para credenciales inválidas y evitamos filtrar inconsistencias internas.
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Incorrect email or password"));
        
        var jwtToken = jwtService.generateToken(user);
        
        return new AuthResponse(jwtToken);
    }
}
