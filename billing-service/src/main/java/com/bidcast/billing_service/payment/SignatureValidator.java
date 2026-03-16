package com.bidcast.billing_service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
@Slf4j
public class SignatureValidator {

    @Value("${mercadopago.webhook.secret}")
    private String webhookSecret;

    public boolean isValid(String xSignature, String xRequestId, String queryParams) {
        if (xSignature == null || xRequestId == null || webhookSecret == null) {
            return false;
        }

        try {
            // El formato de x-signature de MP es: ts=TIMESTAMP;v1=HASH
            // Para simplificar en el MVP, buscaremos la firma v1
            String[] parts = xSignature.split(",");
            String ts = "";
            String v1 = "";
            
            for (String part : parts) {
                if (part.contains("ts=")) ts = part.split("=")[1];
                if (part.contains("v1=")) v1 = part.split("=")[1];
            }

            // El string a firmar según la documentación oficial de MP V2:
            // "id:" + id_del_recurso + ";request-id:" + x-request-id + ";ts:" + ts + ";"
            // Nota: Mercado Pago está migrando formatos, esta es la validación robusta:
            String manifest = String.format("id:%s;request-id:%s;ts:%s;", queryParams, xRequestId, ts);
            
            String generatedHash = hmacSha256(manifest, webhookSecret);
            
            return generatedHash.equals(v1);

        } catch (Exception e) {
            log.error("Error validando firma de Mercado Pago: {}", e.getMessage());
            return false;
        }
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
