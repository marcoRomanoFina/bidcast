package com.bidcast.billing_service.payment;

import com.bidcast.billing_service.payment.dto.PaymentPreferenceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentPreferenceService paymentPreferenceService;
    private final WebhookService webhookService;
    private final SignatureValidator signatureValidator;

    @Value("${mercadopago.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(@RequestBody @Valid PaymentPreferenceRequest request) {
        String initPoint = paymentPreferenceService.createPaymentPreference(request);
        return ResponseEntity.ok(Map.of("init_point", initPoint));
    }

    /**
     * Webhook de Mercado Pago para recibir notificaciones de pagos.
     * Soporta validación de firma x-signature.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId
    ) {
        // Determinamos el ID del pago (MP lo manda de formas distintas según la versión)
        String finalId = (dataId != null) ? dataId : id;
        
        log.info("Webhook recibido: topic={}, type={}, id={}, x-signature={}", topic, type, finalId, signature);

        // --- VALIDACIÓN DE FIRMA (Shield) ---
        // En desarrollo/MVP permitimos saltar la firma si el secreto es el dummy por defecto
        if (!"dummy_secret_for_dev".equals(webhookSecret)) {
            if (!signatureValidator.isValid(signature, requestId, finalId)) {
                log.error("Firma de Webhook INVÁLIDA. Posible intento de fraude para el pago {}", finalId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            log.warn("Saltando validación de firma de Webhook (MODO DESARROLLO)");
        }

        // --- PROCESAMIENTO ---
        // MP manda notificaciones por varios temas, solo nos importa 'payment'
        if ("payment".equals(topic) || "payment".equals(type)) {
            webhookService.processNotification(finalId);
        }

        // Siempre devolver 200/201 a MP para que deje de reintentar
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
