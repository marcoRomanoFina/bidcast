package com.bidcast.selection_service.selection;

import com.bidcast.selection_service.core.exception.InvalidReceiptSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptTokenServiceTest {

    private ReceiptTokenService tokenService;
    private final String SECRET = "testSecretKey123!testSecretKey123!";

    @BeforeEach
    void setUp() {
        tokenService = new ReceiptTokenService(SECRET);
    }

    @Test
    @DisplayName("Generación y Validación Exitosa")
    void generateAndValidate_Success() {
        String sessionId = "sess-123";
        UUID bidId = UUID.randomUUID();
        String advertiserId = "adv-456";
        String creativeId = "creative-1";
        int slotCount = 3;
        BigDecimal pricePerSlot = new BigDecimal("0.75");

        String token = tokenService.generateReceiptId(sessionId, bidId, advertiserId, creativeId, slotCount, pricePerSlot);
        assertNotNull(token);

        ValidatedReceipt receipt = tokenService.validateAndExtract(token, sessionId, bidId, creativeId, 600);
        
        assertEquals(advertiserId, receipt.advertiserId());
        assertEquals(creativeId, receipt.creativeId());
        assertEquals(slotCount, receipt.slotCount());
        assertEquals(0, pricePerSlot.compareTo(receipt.pricePerSlot()));
        assertEquals(0, new BigDecimal("2.25").compareTo(receipt.totalPrice()));
    }

    @Test
    @DisplayName("Falla si se manipula el precio en el token")
    void validate_FailsIfPriceIsManipulated() {
        String sessionId = "sess-123";
        UUID bidId = UUID.randomUUID();
        BigDecimal originalPrice = new BigDecimal("0.75");
        
        String token = tokenService.generateReceiptId(sessionId, bidId, "adv-1", "creative-1", 2, originalPrice);
        
        // Manipulamos el precio en el string del token (de 0.75 a 0.99)
        String tamperedToken = token.replace("0.75", "0.99");

        assertThrows(InvalidReceiptSignatureException.class, () -> {
            tokenService.validateAndExtract(tamperedToken, sessionId, bidId, "creative-1", 600);
        });
    }

    @Test
    @DisplayName("Falla si la sesión no coincide")
    void validate_FailsIfSessionMismatch() {
        String token = tokenService.generateReceiptId("sess-A", UUID.randomUUID(), "adv-1", "creative-1", 1, BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.validateAndExtract(token, "sess-B", UUID.randomUUID(), "creative-1", 600);
        });
    }

    @Test
    @DisplayName("Falla si el creative no coincide")
    void validate_FailsIfCreativeMismatch() {
        UUID bidId = UUID.randomUUID();
        String token = tokenService.generateReceiptId("sess-1", bidId, "adv-1", "creative-1", 1, BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.validateAndExtract(token, "sess-1", bidId, "creative-2", 600);
        });
    }

    @Test
    @DisplayName("Falla si el token ha expirado")
    void validate_FailsIfExpired() throws InterruptedException {
        // Para no esperar 10 minutos, usamos un TTL de 0 segundos en la validación
        String token = tokenService.generateReceiptId("sess-1", UUID.randomUUID(), "adv-1", "creative-1", 1, BigDecimal.ONE);
        
        // Esperamos 1 segundo para asegurar que el timestamp sea anterior al 'now'
        Thread.sleep(1100);

        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.validateAndExtract(token, "sess-1", UUID.fromString(token.split(":")[1]), "creative-1", 0);
        });
    }

    @Test
    @DisplayName("Falla si el formato es inválido")
    void validate_FailsIfFormatIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.validateAndExtract("formato:invalido:sin:partes", "sess-1", UUID.randomUUID(), "creative-1", 600);
        });
    }
}
