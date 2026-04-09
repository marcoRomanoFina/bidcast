package com.bidcast.selection_service.receipt;

import com.bidcast.selection_service.core.exception.InvalidReceiptSignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/*
Este servicio es para generar y comprobar la integridad y validez de los PoP, la idea es
que sea stateless sin guardar nada en DB y mediante firmas HMAC-SHA256.
 */
@Service
@Slf4j
public class ReceiptTokenService {

    private final String secretKey;
    private static final String HMAC_ALGO = "HmacSHA256";

    public ReceiptTokenService(@Value("${bidcast.selection.receipt-secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    /*
     Genera un token firmado (JWS-like) con metadatos incrustados.
     Formato: sessionId:offerId:advertiserId:creativeId:slotCount:pricePerSlot:timestamp:firma
     */
    public String generateReceiptId(String sessionId, UUID offerId, String advertiserId, String creativeId,
                                    Integer slotCount, BigDecimal pricePerSlot) {
        long timestamp = Instant.now().getEpochSecond();
        String payload = String.format(
                "%s:%s:%s:%s:%d:%s:%d",
                sessionId,
                offerId,
                advertiserId,
                creativeId,
                slotCount,
                pricePerSlot.toPlainString(),
                timestamp
        );
        String signature = calculateHmac(payload);
        return payload + ":" + signature;
    }

    /*
     * El "Validador de la Verdad":
     * 1. Verifica que la firma coincida con los datos (Integridad).
     * 2. Verifica que el ticket no tenga más de 10 minutos (TTL).
     * 3. Extrae los datos originales sin consultar DB
     */
    public ValidatedReceipt validateAndExtract(String receiptId, String expectedSessionId, UUID expectedOfferId,
                                               String expectedCreativeId, long maxAgeSeconds) {
        String[] parts = receiptId.split(":");
        if (parts.length != 8) {
            throw new IllegalArgumentException("Invalid receipt format");
        }

        String sessionId = parts[0];
        String offerIdStr = parts[1];
        String advertiserId = parts[2];
        String creativeId = parts[3];
        int slotCount = Integer.parseInt(parts[4]);
        BigDecimal pricePerSlot = new BigDecimal(parts[5]);
        long timestamp = Long.parseLong(parts[6]);
        String providedSignature = parts[7];

        if (!sessionId.equals(expectedSessionId)
                || !offerIdStr.equals(expectedOfferId.toString())
                || !creativeId.equals(expectedCreativeId)) {
            throw new IllegalArgumentException("Receipt does not match the expected session, offer, or creative");
        }

        if (Instant.now().getEpochSecond() - timestamp > maxAgeSeconds) {
            throw new IllegalArgumentException("Receipt expired (time-to-live exceeded)");
        }

        String payload = String.format(
                "%s:%s:%s:%s:%d:%s:%d",
                sessionId,
                offerIdStr,
                advertiserId,
                creativeId,
                slotCount,
                pricePerSlot.toPlainString(),
                timestamp
        );
        String calculatedSignature = calculateHmac(payload);
        
        if (!calculatedSignature.equals(providedSignature)) {
            log.error("SECURITY ALERT: Received receipt with invalid signature for session {}", sessionId);
            throw new InvalidReceiptSignatureException(sessionId, "Invalid receipt signature (possible tampering)");
        }

        return new ValidatedReceipt(
                advertiserId,
                creativeId,
                slotCount,
                pricePerSlot,
                pricePerSlot.multiply(BigDecimal.valueOf(slotCount))
        );
    }
    
    // metodo privado para calcular el hmac
    private String calculateHmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Fatal error while calculating HMAC", e);
        }
    }
}
