# Especificaciones: Auction Service (Live Marketplace MVP)

## 1. El Modelo de Negocio (Live Bidding)
El sistema funciona como un "Marketplace en Vivo". Las pantallas abren "Sesiones". Los anunciantes ven las sesiones activas y deciden participar en ellas con un presupuesto total y una puja max por impresión. Al terminar la sesión, se genera un resumen.

## 2. Entidades Core del MVP
* **`DeviceSession`:** Representa una pantalla encendida (Ej: "Gym", "Restaurant"). Tiene un ID y un estado (ACTIVE/CLOSED).
* **`SessionBid`:** La participación de un anunciante en una sesión específica.
  - `sessionId`: La sesión a la que entró.
  - `advertiserId`: Quién paga.
  - `totalBudget`: Cuánta plata máxima quiere gastar en este rato.
  - `bidPrice`: Cuánto paga por cada impresión.
  - `spentBudget`: Dinero consumido hasta el momento.

## 3. El Algoritmo de Subasta (El Pumba)
1. El Player (pantalla) hace un `GET /api/v1/auction/next?sessionId=123`.
2. El servicio busca todos los `SessionBid` para ese `sessionId`.
3. Filtra: Descarta los bids donde `spentBudget + bidPrice > totalBudget`.
4. Ordena: De mayor a menor según el `bidPrice`.
5. Ganador: Devuelve el adId del ganador (First-Price).
* **Restricción Técnica:** Debe resolverse por HTTP REST en < 100ms.

## 4. Cobros y Resúmenes (Event-Driven)
* **Proof of Play:** Al reproducirse el anuncio, se emite un evento a RabbitMQ (`ad.played`) para descontar el saldo al instante y sumar al `spentBudget`.
* **Resumen de Sesión:** Cuando la pantalla se apaga, se recibe un evento `session.closed` vía RabbitMQ para generar el reporte final del anunciante.