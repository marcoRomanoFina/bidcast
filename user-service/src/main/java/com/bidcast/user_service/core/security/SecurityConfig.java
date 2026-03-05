package com.bidcast.user_service.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable) 
            .authorizeHttpRequests(auth -> auth
                // 1. LA SALA DE ESPERA (Pública)
                // Dejamos que cualquiera pueda registrarse o loguearse sin tener token
                .requestMatchers("/api/auth/**").permitAll() 
                
                // 2. EL RESTO DEL EDIFICIO (VIP)
                // Cualquier otra petición (ej: /api/bids) va a exigir un token válido
                .anyRequest().authenticated() 
            )
            .sessionManagement(session -> session
                // 3. LA REGLA DE ORO DEL JWT
                // Le decimos a Spring que no guarde sesiones en memoria (STATELESS). 
                // Cada petición es independiente y se valida solo con el Token.
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) 
            )
            // 4. PRESENTAMOS AL EQUIPO
            .authenticationProvider(authenticationProvider) // El "Jefe de RRHH" que hace la matemática
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // Ponemos a tu "Patovica" en la puerta
            .build();
    }        
}