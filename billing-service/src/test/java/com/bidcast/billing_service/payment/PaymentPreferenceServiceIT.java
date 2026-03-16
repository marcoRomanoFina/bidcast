package com.bidcast.billing_service.payment;

import com.bidcast.billing_service.TestcontainersConfiguration;
import com.bidcast.billing_service.payment.dto.PaymentPreferenceRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class PaymentPreferenceServiceIT {

    @Autowired
    private PaymentPreferenceService paymentPreferenceService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PreferenceClient preferenceClient;

    @Test
    void shouldSavePaymentInDatabaseAndReturnInitPoint() throws Exception {
        // GIVEN
        UUID advertiserId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");
        PaymentPreferenceRequest request = new PaymentPreferenceRequest(advertiserId, amount, "Integration Test");

        // Mocking MP Client Response
        Preference mpPreference = mock(Preference.class);
        when(mpPreference.getId()).thenReturn("pref-it-123");
        when(mpPreference.getInitPoint()).thenReturn("http://mp.com/it");
        
        when(preferenceClient.create(any(PreferenceRequest.class))).thenReturn(mpPreference);

        // WHEN
        String initPoint = paymentPreferenceService.createPaymentPreference(request);

        // THEN
        assertNotNull(initPoint);
        assertEquals("http://mp.com/it", initPoint);

        // VERIFY DATABASE
        var payments = paymentRepository.findAll();
        assertFalse(payments.isEmpty());
        
        Payment savedPayment = payments.stream()
                .filter(p -> p.getAdvertiserId().equals(advertiserId))
                .findFirst()
                .orElseThrow();

        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals(amount.compareTo(savedPayment.getAmount()), 0);
        assertEquals("pref-it-123", savedPayment.getMpPreferenceId());
    }
}
