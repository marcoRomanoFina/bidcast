package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.core.exception.InvalidReceiptSignatureException;
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

    public ReceiptTokenService(@Value("${bidcast.auction.receipt-secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    /*
     Genera un token firmado (JWS-like) con metadatos incrustados.
     Formato: sessionId:bidId:advertiserId:bidPrice:timestamp:firma
     */
    public String generateReceiptId(String sessionId, UUID bidId, String advertiserId, BigDecimal bidPrice) {
        long timestamp = Instant.now().getEpochSecond();
        String payload = String.format("%s:%s:%s:%s:%d", sessionId, bidId, advertiserId, bidPrice.toPlainString(), timestamp);
        String signature = calculateHmac(payload);
        return payload + ":" + signature;
    }

    /*
     * El "Validador de la Verdad":
     * 1. Verifica que la firma coincida con los datos (Integridad).
     * 2. Verifica que el ticket no tenga más de 10 minutos (TTL).
     * 3. Extrae los datos originales sin consultar DB
     */
    public ValidatedReceipt validateAndExtract(String receiptId, String expectedSessionId, UUID expectedBidId, long maxAgeSeconds) {
        String[] parts = receiptId.split(":");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid receipt format");
        }

        String sessionId = parts[0];
        String bidIdStr = parts[1];
        String advertiserId = parts[2];
        BigDecimal bidPrice = new BigDecimal(parts[3]);
        long timestamp = Long.parseLong(parts[4]);
        String providedSignature = parts[5];

        if (!sessionId.equals(expectedSessionId) || !bidIdStr.equals(expectedBidId.toString())) {
            throw new IllegalArgumentException("Receipt does not match the expected session or bid");
        }

        if (Instant.now().getEpochSecond() - timestamp > maxAgeSeconds) {
            throw new IllegalArgumentException("Receipt expired (time-to-live exceeded)");
        }

        String payload = String.format("%s:%s:%s:%s:%d", sessionId, bidIdStr, advertiserId, bidPrice.toPlainString(), timestamp);
        String calculatedSignature = calculateHmac(payload);
        
        if (!calculatedSignature.equals(providedSignature)) {
            log.error("SECURITY ALERT: Received receipt with invalid signature for session {}", sessionId);
            throw new InvalidReceiptSignatureException(sessionId, "Invalid receipt signature (possible tampering)");
        }

        return new ValidatedReceipt(advertiserId, bidPrice);
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
