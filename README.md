# Bidcast - Real-Time Bidding Platform

Plataforma AdTech y FinTech diseñada para la subasta de anuncios en tiempo real (RTB) sobre pantallas físicas, con un motor financiero integrado para la gestión de presupuestos y cobros automáticos.

## Arquitectura del Sistema

El ecosistema está compuesto por microservicios desacoplados bajo una arquitectura Event-Driven:

1.  **API Gateway**: Único punto de entrada reactivo (Spring Cloud Gateway). Centraliza la seguridad y el ruteo.
2.  **User Service**: Gestión de identidades y roles (Advertiser, Publisher).
3.  **Billing Service**: Integración con Mercado Pago, gestión de intenciones de pago y validación de webhooks.
4.  **Wallet Service**: Ledger financiero interno. Procesa créditos y débitos en tiempo real.
5.  **Advertisement Service**: Gestión de campañas, presupuestos y assets multimedia.
6.  **Auction Service**: Motor de subastas de baja latencia (<100ms) que decide el anuncio ganador.
7.  **Device Service**: Gestión de inventario de pantallas y telemetría de sesiones.

## Flujo Crítico de Datos

1.  **Carga de Saldo**: Anunciante paga vía Billing -> Webhook verificado -> Evento asíncrono (RabbitMQ) -> Wallet acredita saldo.
2.  **Subasta**: Pantalla solicita anuncio -> Auction filtra campañas por presupuesto y puja -> Retorna ganador.
3.  **Settlement**: Tras la reproducción -> Evento ProofOfPlay -> Wallet debita el costo de la puja del presupuesto del anunciante.

## Decisiones de Ingeniería y Seguridad

### 1. Integridad Financiera
*   **Concurrencia**: Uso de Optimistic Locking (@Version) en las entidades de Payment y Wallet para prevenir el problema del doble crédito/débito en hilos simultáneos.
*   **Precisión**: Uso estricto de java.math.BigDecimal con precisión configurada en base de datos para evitar errores de redondeo en transacciones.

### 2. Blindaje del Gateway
*   **Header Scrubbing**: El Gateway elimina cualquier header X-User-* entrante para prevenir el spoofing de identidad.
*   **Verified Identity**: Solo el Gateway tiene permiso para inyectar headers de identidad tras validar el JWT, garantizando que los microservicios internos confíen ciegamente en la data recibida.

### 3. Idempotencia y Seguridad Criptográfica
*   **Webhook Security**: Validación de firma HMAC-SHA256 (x-signature) en el microservicio de Billing para asegurar el origen de las notificaciones de pago.
*   **Stateless Proof of Play**: El Auction Service emite tickets de reproducción firmados con HMAC-SHA256. Esto permite validar la integridad del cobro (anunciante y precio) sin necesidad de persistencia intermedia, protegiendo el sistema contra la manipulación de datos por parte de dispositivos externos.
*   **Doble Check**: Verificación de estado previo (APPROVED) antes de procesar cualquier evento para garantizar la idempotencia financiera.

### 4. Calidad y Testing
*   **Testcontainers**: Uso obligatorio de contenedores reales (PostgreSQL, RabbitMQ, Redis) para los tests de integración, garantizando que el comportamiento en desarrollo sea idéntico a producción.

## Stack Tecnológico
*   **Runtime**: Java 21 / Spring Boot 4.0.3
*   **Broker**: RabbitMQ (AMQP)
*   **Persistence**: PostgreSQL (Cold Data) / Redis (Hot Data & Rate Limiting)
*   **Tools**: Docker & Docker Compose

## Backlog Pendiente para MVP

### 1. Rol de Administrador y Moderación
*   **Gobierno de Contenido**: Implementación de un flujo de aprobación manual para campañas creadas por Advertisers antes de ser habilitadas en las subastas.
*   **Gestión Operativa**: Endpoints protegidos para la supervisión de saldos totales del sistema y estados de conectividad de las pantallas.

### 2. Microservicio de Device Player
*   **Client Software**: Software ligero para la ejecución en hardware físico que gestione la descarga de media, el reporte de telemetría y la solicitud de anuncios al Auction Service.

### 3. Almacenamiento de Media
*   **Asset Management**: Integración con servicios de almacenamiento de objetos (S3/Cloudinary) para la gestión escalable de videos e imágenes de alta resolución.

### 4. Dashboard de Analíticas
*   **Reporting**: Visualización en tiempo real para Anunciantes (gasto y alcance) y Publishers (ingresos y disponibilidad de pantallas).

## Ejecución
El ecosistema completo está diseñado para levantarse con una sola instrucción:
```bash
docker-compose up -d
```
