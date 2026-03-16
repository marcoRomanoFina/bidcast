package com.bidcast.billing_service.payment;

import com.bidcast.billing_service.payment.dto.PaymentPreferenceRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPreferenceServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PreferenceClient preferenceClient;

    @InjectMocks
    private PaymentPreferenceService paymentPreferenceService;

    @BeforeEach
    void setUp() {
        // Inyectamos manualmente los valores de las properties que @Value normalmente cargaría
        ReflectionTestUtils.setField(paymentPreferenceService, "successUrl", "http://success.com");
        ReflectionTestUtils.setField(paymentPreferenceService, "failureUrl", "http://failure.com");
        ReflectionTestUtils.setField(paymentPreferenceService, "pendingUrl", "http://pending.com");
        ReflectionTestUtils.setField(paymentPreferenceService, "notificationUrl", "https://webhook.com");
    }

    @Test
    void createPaymentPreference_Success() throws Exception {
        // GIVEN
        UUID advertiserId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        PaymentPreferenceRequest request = new PaymentPreferenceRequest(advertiserId, amount, "Test Payment");

        Payment savedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .advertiserId(advertiserId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        // Simulamos el primer save (cuando se crea el registro local)
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        
        // Mock de la respuesta de Mercado Pago
        Preference mpPreference = mock(Preference.class);
        when(mpPreference.getId()).thenReturn("pref-123");
        when(mpPreference.getInitPoint()).thenReturn("https://mercadopago.com/checkout/123");
        
        when(preferenceClient.create(any(PreferenceRequest.class))).thenReturn(mpPreference);

        // WHEN
        String initPoint = paymentPreferenceService.createPaymentPreference(request);

        // THEN
        assertNotNull(initPoint);
        assertEquals("https://mercadopago.com/checkout/123", initPoint);
        
        // Verificamos que se guardó dos veces (creación y actualización con MP ID)
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(preferenceClient).create(any(PreferenceRequest.class));
    }

    @Test
    void createPaymentPreference_MPError_ThrowsException() throws Exception {
        // GIVEN
        UUID advertiserId = UUID.randomUUID();
        PaymentPreferenceRequest request = new PaymentPreferenceRequest(advertiserId, new BigDecimal("100"), "Error Test");
        
        Payment savedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .build();
        
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        
        // Simulamos error de MP
        when(preferenceClient.create(any(PreferenceRequest.class))).thenThrow(new RuntimeException("MP API Down"));

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> paymentPreferenceService.createPaymentPreference(request));
        
        // Verificamos que el primer save se hizo (rastro local)
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
    }
}
