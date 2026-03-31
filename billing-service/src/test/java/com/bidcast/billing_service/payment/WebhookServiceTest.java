package com.bidcast.billing_service.payment;

import com.bidcast.billing_service.core.event.EventPublisher;
import com.bidcast.billing_service.payment.event.PaymentConfirmedEvent;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private WebhookService webhookService;

    private UUID localPaymentId;
    private com.bidcast.billing_service.payment.Payment localPayment;
    private Payment mpPayment;

    @BeforeEach
    void setUp() {
        localPaymentId = UUID.randomUUID();
        localPayment = com.bidcast.billing_service.payment.Payment.builder()
                .id(localPaymentId)
                .advertiserId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();

        mpPayment = mock(Payment.class);
    }

    @Test
    void shouldProcessApprovedPaymentAndPublishEvent() throws Exception {
        String paymentId = "12345";
        when(mpPayment.getExternalReference()).thenReturn(localPaymentId.toString());
        when(paymentClient.get(12345L)).thenReturn(mpPayment);
        when(mpPayment.getStatus()).thenReturn("approved");
        when(paymentRepository.findById(localPaymentId)).thenReturn(Optional.of(localPayment));

        webhookService.processNotification(paymentId);

        verify(paymentRepository).save(localPayment);
        assertEquals(PaymentStatus.APPROVED, localPayment.getStatus());
        assertEquals(paymentId, localPayment.getMpPaymentId());

        // Verificar que se publicó el EVENTO de dominio
        ArgumentCaptor<PaymentConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentConfirmedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        PaymentConfirmedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(localPaymentId, publishedEvent.paymentId());
        assertEquals(localPayment.getAdvertiserId(), publishedEvent.advertiserId());
        assertEquals(new BigDecimal("100.00"), publishedEvent.amount());
    }

    @Test
    void shouldNotPublishEventIfPaymentAlreadyApproved() throws Exception {
        String paymentId = "12345";
        localPayment.approve("old-id"); // Ya está aprobado
        
        when(mpPayment.getExternalReference()).thenReturn(localPaymentId.toString());
        when(paymentClient.get(12345L)).thenReturn(mpPayment);
        when(mpPayment.getStatus()).thenReturn("approved");
        when(paymentRepository.findById(localPaymentId)).thenReturn(Optional.of(localPayment));

        webhookService.processNotification(paymentId);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldIgnoreWebhookWhenPaymentIdIsInvalid() {
        webhookService.processNotification("abc");

        verifyNoInteractions(paymentClient, paymentRepository, eventPublisher);
    }
}
