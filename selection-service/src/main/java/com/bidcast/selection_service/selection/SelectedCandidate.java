package com.bidcast.selection_service.selection;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

// Este record es el contrato que selection-service le devuelve al device player.
// No representa una "oferta abstracta", sino una reproduccion ya resuelta:
// la offer ya fue elegida, el creative ya fue decidido y el budget ya fue consumido.
//
// - selection-service decide "que" se va a reproducir
// - el device player solo ejecuta esa reproduccion y luego confirma con PoP

@Schema(description = "Confirmed paid play ready for device execution")
public record SelectedCandidate(
        // Identificador de la SessionOffer que financia esta reproduccion.
        // Se usa para trazabilidad, settlement y referencias posteriores.
        @Schema(description = "Offer that funds the play")
        UUID offerId,

        // Sesion de playback donde esta reproduccion fue seleccionada.
        // Ayuda a mantener contexto operativo y a validar PoP.
        @Schema(description = "Session that owns the play")
        String sessionId,

        // Device exacto al que se le entrega esta reproduccion.
        // Es importante porque los cooldowns locales son por device.
        @Schema(description = "Target device")
        String deviceId,

        // Anunciante duenio de la offer.
        // Se incluye para trazabilidad y porque el receipt firmado conserva este dato.
        @Schema(description = "Advertiser that owns the offer")
        String advertiserId,

        // Campaign original a la que pertenece esta reproduccion.
        // Sirve para analytics y para la recencia global por campaign.
        @Schema(description = "Associated campaign")
        String campaignId,

        // Precio base por slot de 5 segundos.
        // El costo real de esta reproduccion se obtiene como:
        // pricePerSlot * slotCount
        @Schema(description = "Base price per 5-second slot", example = "0.50")
        BigDecimal pricePerSlot,

        // Creative puntual que el device debe mostrar.
        // Ya no hay decision pendiente del lado del player.
        @Schema(description = "Exact creative that must be played")
        String creativeId,

        // URL concreta del media a reproducir.
        // El player usa este valor para descargar/cachear/ejecutar el asset.
        @Schema(description = "Media URL to play")
        String mediaUrl,

        // Duracion logica del creative en slots de 5 segundos.
        // Ejemplo: 3 => este creative ocupa 15 segundos de pantalla.
        @Schema(description = "Number of 5-second slots occupied by this creative", example = "3")
        Integer slotCount,

        // Cooldown sugerido para que este mismo creative no vuelva a aparecer
        // enseguida en el mismo device. El bloqueo local nace en selection.
        @Schema(description = "Suggested local cooldown for this creative on this device, in seconds", example = "300")
        Integer deviceCooldownSeconds,

        // Receipt firmado que prueba que esta reproduccion fue emitida por selection-service.
        // El device lo devuelve luego en el endpoint de Proof of Play para validar:
        // - session
        // - offer
        // - creative
        // - precio
        // - duracion
        @Schema(description = "Signed receipt returned later in the Proof of Play")
        String playReceiptId
) {
}
