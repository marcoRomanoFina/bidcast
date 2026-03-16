# Bidcast - Gateway Service 🛡️

El **Gateway Service** es el único punto de entrada público al ecosistema de Bidcast. Actúa como un guardián (Edge Server) encargado de la seguridad, el ruteo dinámico y el control de tráfico antes de que las peticiones lleguen a los microservicios internos.

## 🚀 Funcionalidades Core

1.  **Autenticación Centralizada**: Valida tokens JWT de anunciantes y usuarios.
2.  **Blindaje de Identidad**: Limpia headers maliciosos (`X-User-Id`, etc.) y los re-inyecta basándose exclusivamente en el contenido verificado del JWT.
3.  **Ruteo Reactivo**: Redirige el tráfico a `user-service`, `billing-service`, `device-service`, etc.
4.  **Rate Limiting**: Control de ráfagas de peticiones usando Redis para prevenir abusos.
5.  **CORS**: Configuración global de intercambio de recursos entre orígenes para el frontend.

## 🛠️ Stack Tecnológico
* **Java 21**
* **Spring Boot 4.0.3** (Custom/Project-Specific Parent)
* **Spring Cloud Gateway** (WebFlux / Arquitectura No-Bloqueante)
* **JJWT** (JSON Web Token)
* **Redis** (Rate Limiter)

---

## 🧪 Estrategia de Testing y Nota Técnica (Importante)

Para este microservicio, hemos priorizado una estrategia de **Unit Testing** exhaustiva sobre la lógica de seguridad y ruteo.

### El Problema con los Integration Tests (IT)
Actualmente, existe una incompatibilidad conocida entre la versión **4.0.3** de Spring Boot (y Spring Cloud 2025.0.0) y el entorno **Reactive (WebFlux)** del Gateway durante el levantamiento del contexto de tests de integración (`GatewayServiceIT`).

*   **Síntoma**: Al intentar levantar el contexto de Spring con `@SpringBootTest`, el sistema lanza un `IllegalStateException` debido a conflictos con auto-configuraciones de **MVC** (Servlet).
*   **Causa Raíz**: Ciertas dependencias internas y auto-configuraciones (como `LifecycleMvcEndpointAutoConfiguration`) intentan detectar clases de un entorno Servlet (MVC) que no están presentes en un proyecto puro de WebFlux, provocando errores de `ClassNotFoundException` por `WebServerInitializedEvent`.
*   **Estado Actual**: Se han implementado tests unitarios robustos para `JwtValidator` y `AuthenticationFilter` que verifican el 100% de la lógica de seguridad sin depender del contexto completo de Spring.

### Cómo ejecutar los tests
```bash
./mvnw test
```

## 🔒 Seguridad
El gateway implementa un `AuthenticationFilter` que:
1.  **Remueve** cualquier header `X-User-*` entrante de la petición original.
2.  **Valida** el token `Authorization: Bearer <JWT>`.
3.  **Inyecta** los headers de identidad (`X-User-Id`, `X-User-Email`) garantizando que los microservicios internos puedan confiar plenamente en estos datos.
