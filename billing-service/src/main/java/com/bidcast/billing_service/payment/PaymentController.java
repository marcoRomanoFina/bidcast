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
import java.util.Objects;

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
        boolean paymentNotification = Objects.equals("payment", topic) || Objects.equals("payment", type);
        
        log.info("Webhook received: topic={}, type={}, id={}, x-signature={}", topic, type, finalId, signature);

        if (paymentNotification && (finalId == null || finalId.isBlank())) {
            log.warn("Payment webhook ignored: missing payment identifier");
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        
        // En desarrollo/MVP permitimos saltar la firma si el secreto es el dummy por defecto
        if (!"dummy_secret_for_dev".equals(webhookSecret)) {
            if (!signatureValidator.isValid(signature, requestId, finalId)) {
                log.error("Invalid webhook signature. Possible fraud attempt for payment {}", finalId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            log.warn("Skipping webhook signature validation (DEVELOPMENT MODE)");
        }

      
        // MP manda notificaciones por varios temas, solo nos importa 'payment'
        if ("payment".equals(topic) || "payment".equals(type)) {
            webhookService.processNotification(finalId);
        }

        // Siempre devolver 200/201 a MP para que deje de reintentar
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
