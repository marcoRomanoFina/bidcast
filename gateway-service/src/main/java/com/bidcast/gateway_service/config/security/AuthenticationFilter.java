package com.bidcast.gateway_service.config.security;

import io.jsonwebtoken.Claims;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * Filtro de seguridad de perímetro (Edge Security) que implementa RBAC y Blindaje de Identidad.
 * Intercepta solicitudes, valida el token JWT y propaga la identidad verificada hacia el interior.
 * Utiliza un enfoque reactivo fluido para mejorar la legibilidad y el mantenimiento.
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final JwtValidator jwtValidator;

    public AuthenticationFilter(JwtValidator jwtValidator) {
        super(Config.class);
        this.jwtValidator = jwtValidator;
    }

    /**
     * Configuración dinámica del filtro, permitiendo definir roles requeridos por ruta.
     */
    @Data
    public static class Config {
        private List<String> requiredRoles = Collections.emptyList();
        // no funca lombok (a arreglar)
        public List<String> getRequiredRoles() {
            return requiredRoles;
        }
        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("requiredRoles");
    }

    /**
     * Lógica principal del filtro ejecutada en el pipeline reactivo de Netty.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // 1. Limpieza de request (Sanitización)
            ServerWebExchange cleanedExchange = cleanRequest(exchange);

            // 2. Extracción y validación reactiva
            return extractToken(exchange.getRequest())
                    // 3. Validar token JWT
                    .flatMap(jwtValidator::validateAndExtract)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token")))
                    
                    // 4. Validar roles (RBAC)
                    .flatMap(claims -> validateRoles(claims, config, path))
                    
                    // 5. Enriquecer request con identidad verificada
                    .flatMap(claims -> enrichExchange(cleanedExchange, claims))
                    
                    // 6. Continuar pipeline
                    .flatMap(chain::filter)
                    
                    // 7. Manejo de errores centralizado
                    .onErrorResume(ex -> {
                        HttpStatus status = HttpStatus.UNAUTHORIZED;
                        if (ex instanceof ResponseStatusException rse) {
                            status = (HttpStatus) rse.getStatusCode();
                        }
                        log.warn("Auth error on {}: {} (Status: {})", path, ex.getMessage(), status);
                        return onError(cleanedExchange, status);
                    });
        };
    }

    /**
     * Elimina sistemáticamente cualquier header de identidad que el cliente intente suplantar.
     */
    private ServerWebExchange cleanRequest(ServerWebExchange exchange) {
        ServerHttpRequest cleanRequest = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Email");
                    h.remove("X-User-Roles");
                })
                .build();

        return exchange.mutate().request(cleanRequest).build();
    }

    /**
     * Extrae el token de forma reactiva del header de Authorization.
     */
    private Mono<String> extractToken(ServerHttpRequest request) {
        return Mono.justOrEmpty(request.getHeaders().getFirst("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token missing")));
    }

    /**
     * Verifica si el usuario posee los roles necesarios de forma reactiva.
     */
    private Mono<Claims> validateRoles(Claims claims, Config config, String path) {
        if (config.getRequiredRoles().isEmpty()) {
            return Mono.just(claims);
        }

        List<String> userRoles = getUserRoles(claims);
        boolean hasRole = config.getRequiredRoles().stream()
                .anyMatch(role -> userRoles.contains("ROLE_" + role.toUpperCase()) || userRoles.contains(role.toUpperCase()));

        if (!hasRole) {
            log.warn("Forbidden access: insufficient privileges for {}", path);
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient roles"));
        }

        return Mono.just(claims);
    }

    /**
     * Propaga la identidad ya validada hacia los microservicios internos mediante headers estandarizados.
     */
    private Mono<ServerWebExchange> enrichExchange(ServerWebExchange exchange, Claims claims) {
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token missing userId"));
        }

        return Mono.fromSupplier(() ->
                exchange.mutate()
                        .request(r -> r
                                .header("X-User-Email", claims.getSubject())
                                .header("X-User-Id", userId)
                                .header("X-User-Roles", String.join(",", getUserRoles(claims)))
                                .build())
                        .build()
        );
    }

    /**
     * Extrae de forma segura la lista de roles del payload del JWT.
     */
    @SuppressWarnings("unchecked")
    private List<String> getUserRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return Collections.emptyList();
    }

    /**
     * Finaliza la cadena de filtros con un código de error HTTP específico.
     */
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
