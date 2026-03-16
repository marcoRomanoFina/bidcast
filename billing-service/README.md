# Bidcast - Billing Service 💰

El **Billing Service** es el motor de orquestación de pagos de Bidcast. Se encarga de la integración con Mercado Pago para permitir que los anunciantes recarguen su saldo de forma segura y eficiente.

## 🚀 Funcionalidades Core

1.  **Generación de Preferencias**: Crea links de pago (Checkout Pro) personalizados para cada anunciante.
2.  **Procesamiento de Webhooks**: Recibe notificaciones automáticas de Mercado Pago cuando un pago es aprobado.
3.  **Gestión de Transacciones**: Mantiene un registro local de cada intento de pago con estados (`PENDING`, `APPROVED`, etc.).
4.  **Integración Asíncrona**: Notifica al `Wallet Service` vía RabbitMQ para acreditar el saldo una vez confirmado el pago.

## 🛠️ Stack Tecnológico
* **Java 21** & **Spring Boot 4.0.3**
* **Spring Data JPA** & **PostgreSQL**
* **RabbitMQ** (Mensajería por eventos)
* **Mercado Pago SDK v2.8.0**

---

## 🔒 Seguridad y Resiliencia (Critical)

### 1. Validación de Webhooks (Shield)
Para evitar que atacantes simulen pagos, el servicio valida la firma de Mercado Pago en cada notificación.
*   **Header**: `x-signature`
*   **Algoritmo**: HMAC-SHA256
*   **Variable de Entorno**: `MP_WEBHOOK_SECRET`
*   **Comportamiento**: Si la variable no está configurada (o es la por defecto en dev), el sistema muestra un `WARN` y permite procesar. En producción, la validación es obligatoria y devuelve `403 Forbidden` si la firma es inválida.

### 2. Idempotencia y Concurrencia
*   **Optimistic Locking**: Se usa `@Version` en la entidad `Payment` para evitar que múltiples hilos procesen el mismo pago simultáneamente (Double-Credit Prevention).
*   **Doble Check**: Antes de procesar cualquier notificación, se verifica si el pago ya existe en estado `APPROVED` en la base de datos local.

---

## ⚙️ Configuración de Mercado Pago

Para que el servicio funcione correctamente, deben configurarse las siguientes variables de entorno:

| Variable | Descripción |
| :--- | :--- |
| `MP_ACCESS_TOKEN` | Token de acceso de tu aplicación en Mercado Pago. |
| `MP_WEBHOOK_SECRET` | Clave secreta que se encuentra en la sección de Webhooks del Dashboard de MP. |
| `MP_NOTIFICATION_URL` | URL pública donde MP enviará los POSTs (ej: `https://tu-dominio.com/api/v1/billing/webhook`). |

---

## 🧪 Ejecución de Tests
El servicio cuenta con una suite completa de tests:
*   **Unit Tests**: Verifican la lógica de negocio y creación de preferencias.
*   **Integration Tests**: Usan **Testcontainers** para levantar un Postgres y RabbitMQ reales, asegurando que la persistencia y la mensajería funcionen correctamente.

```bash
./mvnw test
```
