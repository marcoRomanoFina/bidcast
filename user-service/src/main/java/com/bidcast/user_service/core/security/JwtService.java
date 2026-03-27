package com.bidcast.user_service.core.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import com.bidcast.user_service.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio encargado de la gestión integral de JSON Web Tokens (JWT).
 * Provee lógica para la generación, validación y extracción de información de tokens
 * asegurando la integridad de la identidad del usuario en el ecosistema.
 */
@Service
public class JwtService {
    
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Obtiene la clave de firma HMAC-SHA a partir de la clave secreta codificada en Base64.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extrae el payload (claims) completo del token, verificando su firma y vigencia.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser() 
                .verifyWith(getSignInKey()) 
                .build()
                .parseSignedClaims(token) 
                .getPayload(); 
    }
    
    /**
     * Extrae un claim específico del token utilizando una función transformadora.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Obtiene el subject (usualmente el email) del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Obtiene la fecha de expiración configurada en el token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Genera un nuevo token JWT a partir de la entidad User.
     * Incluye automáticamente el 'userId' y la lista de 'roles' con el prefijo ROLE_.
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("roles", user.getRoles().stream()
                .map(Enum::name)
                .map(role -> "ROLE_" + role)
                .collect(java.util.stream.Collectors.toList()));
        return generateToken(extraClaims, user);
    }

    /**
     * Método base para la construcción del token JWT.
     * Define el subject, fecha de emisión, expiración y firma con algoritmo HS256.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256) 
                .compact();
    }

    /**
     * Valida que el token pertenezca al usuario indicado y no haya expirado.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Verifica si la fecha actual ha superado la fecha de expiración del token.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
