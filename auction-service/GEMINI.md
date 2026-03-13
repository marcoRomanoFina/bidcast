# Contexto de IA: Auction Service 

## 1. Visión General y Rol del Servicio
* **Misión:** Es el motor de subastas en tiempo real (RTB) de Bidcast.
* **Modelo de Negocio (Live Bidding):** Las pantallas físicas abren "Sesiones en Vivo". Los anunciantes participan en estas sesiones específicas estableciendo un presupuesto máximo y una puja por impresión.
* **Bounded Context:** Este servicio NO administra la conexión física con las pantallas ni el ciclo de vida de la sesión. Esa es responsabilidad del `device-service`. El `auction-service` reacciona a los eventos de sesión para habilitar el Marketplace y calcula los ganadores ("El Pumba").

## 2. Modelos de Datos Core (Dominio Local)
* **`DeviceSession` (Copia Liviana):**
  * Se crea/cierra escuchando eventos de RabbitMQ provenientes del `device-service`.
  * Campos clave: `sessionId`, `deviceId`, `status` (ACTIVE/CLOSED).
* **`SessionBid` (La Ficha del Anunciante):**
  * Campos clave: `sessionId` (a qué sesión en vivo entró), `advertiserId`, `totalBudget` (presupuesto máximo para este rato), `bidPrice` (cuánto paga por pasada), mediaUrl (url apuntando al video/imagen (la publicidad)).
* **`ProofOfPlay` (Ticket Fiscal):**
  * Ticket inmutable de transparencia extrema. Contiene: `playReceiptId`, `sessionId`, `deviceId`, `advertiserId`, `exactTimestamp` y `costCharged`.

## 3. El Algoritmo de Subasta 
* **Protocolo:** Síncrono, vía HTTP REST (`GET /api/v1/auction/next?sessionId=...`). Debe responder en < 100ms.
* **Lógica de Ejecución:**
  1. Busca en la BD todos los `SessionBid` activos vinculados a ese `sessionId`.
  2. **Hot Data (Redis):** Consulta el gasto actual del anunciante en esa sesión usando Redis.
  3. **Filtro:** Descarta los bids donde `gasto_actual_redis + bidPrice > totalBudget`.
  4. **Ganador:** Ordena los restantes por `bidPrice` (First-Price) y devuelve el anuncio ganador.

## 4. Arquitectura de Cobros y Alta Concurrencia (Event-Driven)
Para soportar ráfagas de micro-transacciones sin bloquear la BD relacional, se usa el patrón Write-Behind con Batching:
* **Paso 1 (RabbitMQ - Proof of Play):** Al reproducirse un anuncio, llega el evento `ad.played` a la cola.
* **Paso 2 (Redis - Contador Atómico):** El listener del evento hace un `INCRBY` hiperveloz en Redis sumando el costo al gasto de esa sesión. No se toca PostgreSQL.
* **Paso 3 (RabbitMQ - Cierre):** Llega el evento `session.closed` desde el `device-service`.
* **Paso 4 (PostgreSQL - Settlement):** El `auction-service` lee el gasto final acumulado en Redis, hace un UNICO `UPDATE` en PostgreSQL para asentar el cobro definitivo, borra la key de Redis y genera el resumen para el anunciante.

## 5. Stack Tecnológico Estricto
* **Core:** Java 21, Spring Boot 4.0.3, Lombok.
* **API:** Spring Web (REST Controllers stateless).
* **Persistencia "Cold":** Spring Data JPA + PostgreSQL.
* **Persistencia "Hot" (Contadores):** Spring Data Redis (`StringRedisTemplate`).
* **Mensajería:** Spring Boot Starter AMQP (RabbitMQ clásico para Eventos y Proof of Play. NO usar Rabbit Streams ni Cloud Bus).
* **Testing:** Testcontainers (PostgreSQL, Redis, RabbitMQ).

## 6. Reglas de Estilo y Código (Mandatos del Desarrollador)
* **Guerra al NULL:** Evitá el uso de `null` a toda costa. No uses `if (obj == null)`. Preferí el uso de `Optional<T>`, excepciones de dominio lanzadas tempranamente o patrones que aseguren la presencia de datos. Si un método de validación falla, debe lanzar una excepción en lugar de devolver null.
