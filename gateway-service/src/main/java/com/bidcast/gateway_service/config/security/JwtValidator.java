package com.bidcast.gateway_service.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

/**
 * Validador reactivo y sin estado (stateless) para tokens JWT.
 * Se encarga de verificar la firma y extraer los claims de identidad
 */
@Component
public class JwtValidator {

    private final String secretKey;

    public JwtValidator(@Value("${application.security.jwt.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Valida la integridad del token y extrae su contenido de forma no bloqueante.
     * @param token El JWT recibido en el header Authorization.
     * @return Un Mono que emite los Claims si es válido, o un Mono vacío si hay error o expiración.
     */
    public Mono<Claims> validateAndExtract(String token) {
        return Mono.fromCallable(() -> Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload())
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Reconstruye la clave de firma a partir del secreto configurado.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
