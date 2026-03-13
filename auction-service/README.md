# 🎯 Auction Service

Motor de subastas en tiempo real (RTB) para la plataforma Bidcast. Responsable de la selección de anuncios, validación de reproducciones y orquestación de liquidaciones.

## 🛠️ Tecnologías
*   **Java 21**: Virtual Threads, Pattern Matching, Sealed Interfaces.
*   **Redis**: Única fuente de verdad para datos en caliente.
*   **PostgreSQL**: Persistencia definitiva y auditoría de consumos.
*   **RabbitMQ**: Comunicación asíncrona para liquidación financiera.

## 🏛️ Arquitectura de Datos (Redis)
| Clave | Tipo | Contenido |
| :--- | :--- | :--- |
| `session:{sid}:active_bids` | **SET** | IDs de pujas activas en la sesión. |
| `session:{sid}:bid:{id}:metadata` | **JSON** | Metadatos del anuncio (URL, Precio, IDs). |
| `session:{sid}:bid:{id}:budget` | **STRING** | Saldo remanente en centavos (Atómico). |

## 🚀 Funcionalidades Clave
1.  **RTB Engine**: Algoritmo funcional First-Price sobre Redis. Latencia < 10ms.
2.  **Self-Healing**: Rehidratación masiva o quirúrgica desde Postgres ante pérdida de datos en Redis.
3.  **Stateless PoP**: Verificación de tickets de reproducción mediante HMAC-SHA256.
4.  **Batch Settlement**: Agrupación de cobros para liquidación diferida en el Wallet Service.

## 🧪 Testing
*   Suite de tests de integración con **Testcontainers (Alpine)**.
*   Escenarios de validación de auto-sanación y concurrencia.
