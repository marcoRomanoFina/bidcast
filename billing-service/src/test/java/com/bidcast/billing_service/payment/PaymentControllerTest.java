package com.bidcast.billing_service.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentPreferenceService paymentPreferenceService;

    @Mock
    private WebhookService webhookService;

    @Mock
    private SignatureValidator signatureValidator;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentController, "webhookSecret", "dummy_secret_for_dev");
    }

    @Test
    void shouldReturnCreatedWhenPaymentWebhookHasNoId() {
        ResponseEntity<Void> response = paymentController.handleWebhook(
                null,
                "payment",
                null,
                null,
                null,
                null
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(webhookService, never()).processNotification(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectWebhookWhenSignatureIsInvalid() {
        ReflectionTestUtils.setField(paymentController, "webhookSecret", "real_secret");
        when(signatureValidator.isValid("ts=1,v1=bad", "req-1", "123")).thenReturn(false);

        ResponseEntity<Void> response = paymentController.handleWebhook(
                "123",
                "payment",
                null,
                null,
                "ts=1,v1=bad",
                "req-1"
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(webhookService, never()).processNotification("123");
    }
}
