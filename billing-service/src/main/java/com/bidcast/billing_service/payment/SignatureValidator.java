package com.bidcast.billing_service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@Slf4j
public class SignatureValidator {

    @Value("${mercadopago.webhook.secret}")
    private String webhookSecret;

    public boolean isValid(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || xRequestId == null || webhookSecret == null || dataId == null) {
            log.warn("Missing required parameters to validate the Mercado Pago signature");
            return false;
        }

        try {
            // El formato de x-signature de MP es: ts=TIMESTAMP,v1=HASH
            String ts = extractValue(xSignature, "ts=");
            String v1 = extractValue(xSignature, "v1=");

            if (ts.isEmpty() || v1.isEmpty()) {
                log.warn("Invalid x-signature format: {}", xSignature);
                return false;
            }

            // El string a firmar según MP V2:
            // "id:[data.id];request-id:[x-request-id];ts:[ts];"
            String manifest = String.format("id:%s;request-id:%s;ts:%s;", dataId, xRequestId, ts);
            
            String generatedHash = hmacSha256(manifest, webhookSecret);
            
           
            return MessageDigest.isEqual(
                    generatedHash.getBytes(StandardCharsets.UTF_8), 
                    v1.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            log.error("Cryptographic error while validating Mercado Pago signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae de forma segura el valor de un key=value dentro del header
     */
    private String extractValue(String header, String prefix) {
        String[] parts = header.split(",");
        for (String part : parts) {
            String trimmedPart = part.trim();
            if (trimmedPart.startsWith(prefix) && trimmedPart.length() > prefix.length()) {
                return trimmedPart.substring(prefix.length());
            }
        }
        return "";
    }

    private String hmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
