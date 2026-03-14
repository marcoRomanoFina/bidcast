package com.bidcast.gateway_service.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filtro Global de Autenticación con Blindaje de Headers (Fix Inmutabilidad).
 * fix(no detecta lombok rari)
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtValidator jwtValidator;

    public AuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/users/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. LIMPIEZA DE PERÍMETRO: Removemos headers específicos usando el builder seguro.
        ServerHttpRequest cleanRequest = request.mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Email");
                    h.remove("X-User-Role");
                })
                .build();

        ServerWebExchange cleanedExchange = exchange.mutate().request(cleanRequest).build();

        // 2. Omitir validación para rutas públicas
        if (EXCLUDED_PATHS.stream().anyMatch(path::contains)) {
            return chain.filter(cleanedExchange);
        }

        // 3. Extraer y Validar JWT
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Acceso denegado: Token ausente en ruta protegida {}", path);
            return onError(cleanedExchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        return jwtValidator.validateAndExtract(token)
                .map(claims -> {
                    // 4. INYECCIÓN SEGURA: Inyectamos los headers de identidad verificada
                    ServerWebExchange authenticatedExchange = cleanedExchange.mutate()
                            .request(r -> r
                                    .header("X-User-Email", claims.getSubject())
                                    .header("X-User-Id", claims.get("userId", String.class))
                                    .build())
                            .build();
                    
                    return chain.filter(authenticatedExchange);
                })
                .orElseGet(() -> {
                    log.error("Acceso denegado: Token inválido para ruta {}", path);
                    return onError(cleanedExchange, HttpStatus.UNAUTHORIZED);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}