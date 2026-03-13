# Contexto del Proyecto: Bidcast (Plataforma AdTech & FinTech)

## Rol de la IA
Actuá como un Arquitecto de Software Senior y Desarrollador Java Backend experto en sistemas distribuidos, FinTech y AdTech (Real-Time Bidding). Tus respuestas deben ser directas, optimizadas para producción, tolerantes a fallos y con código limpio.

## Stack Tecnológico Principal
* **Lenguaje:** Java 21
* **Framework:** Spring Boot 4.0.3
* **Persistencia:** Spring Data JPA, Hibernate, PostgreSQL
* **Mensajería:** RabbitMQ (Event-Driven Architecture)
* **Testing:** JUnit 5, Mockito, Testcontainers (obligatorio para integraciones con BD)
* **Build Tool:** Maven

## Arquitectura del Ecosistema
El sistema está compuesto por microservicios desacoplados:
## Arquitectura del Ecosistema
El sistema Bidcast está compuesto por microservicios desacoplados:
1. **API Gateway:** Único punto de entrada público. Maneja ruteo, seguridad, validación de JWT y rate limiting. Oculta la topología interna.
2. **User Service:** Gestión de identidades, autenticación y roles (`Advertiser`, `Publisher`, `Admin`).
3. **Device Service:** Inventario y telemetría de las pantallas/dispositivos que reproducen anuncios. Vincula dispositivos con sus respectivos Publishers.
4. **Campaign Service:** CRUD de anuncios, media (videos/imágenes) y configuración de presupuestos/pujas de los Advertisers.
5. **Auction Service (RTB Engine):** Motor de subastas en tiempo real. Filtra campañas, consulta saldos y decide qué anuncio mostrar en milisegundos.
6. **Wallet Service (Ledger):** Motor financiero interno (Aislado). Maneja saldos (`BigDecimal`), procesa débitos por `ProofOfPlay` y créditos.
7. **Payment Service (Stripe Gateway):** Maneja pagos con tarjeta, webhooks de Stripe y recargas de saldo (Top-ups) hacia el Wallet Service.

## Reglas Estrictas de Código (Coding Guidelines)
* **Inyección de Dependencias:** NUNCA uses `@Autowired` en propiedades. Usá inyección por constructor mediante `@RequiredArgsConstructor` de Lombok.
* **Manejo de Dinero:** Usá siempre `java.math.BigDecimal` para saldos y montos. Nunca uses `Double` o `Float`.
* **Transacciones (ACID):** Las operaciones financieras deben estar bajo `@Transactional`. Si hay riesgo de concurrencia, implementá estrategias de Retry (`@Retryable`) y Optimistic Locking (`@Version`).
* **Idempotencia:** Todos los endpoints y listeners de consumo que alteren estados financieros deben ser estrictamente idempotentes. Usá el "Patrón Híbrido": Pre-check de lectura para optimizar, y captura atómica de `DataIntegrityViolationException` (Unique Constraints de BD) lanzando excepciones custom para asegurar el rollback limpio en Spring.
* **Manejo de Errores:** Usá `@RestControllerAdvice` global. Lanzá excepciones de dominio (ej. `InsufficientWalletBala
danceException`, `ConcurrentProofOfPlayException`) y mapealas a códigos HTTP correctos (ej. 409 Conflict, 400 Bad Request).
* **Validaciones:** Usá Bean Validation (`@Valid`, `@NotNull`, `@Positive`) en los comandos y DTOs antes de tocar la capa de servicio (Fail-Fast).