package com.bidcast.billing_service.payment;

import com.bidcast.billing_service.config.RabbitMQConfig;
import com.bidcast.billing_service.payment.event.WalletCreditEvent;
import com.mercadopago.client.payment.PaymentClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private WebhookService webhookService;

    @Test
    void shouldApprovePaymentAndPublishWalletCreditEvent() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID advertiserId = UUID.randomUUID();

        Payment localPayment = Payment.builder()
                .id(paymentId)
                .advertiserId(advertiserId)
                .amount(new BigDecimal("1500.00"))
                .status(PaymentStatus.PENDING)
                .build();

        com.mercadopago.resources.payment.Payment mpPayment = mock(com.mercadopago.resources.payment.Payment.class);
        when(mpPayment.getExternalReference()).thenReturn(paymentId.toString());
        when(mpPayment.getStatus()).thenReturn("approved");
        when(paymentClient.get(123L)).thenReturn(mpPayment);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(localPayment));

        webhookService.processNotification("123");

        assertEquals(PaymentStatus.APPROVED, localPayment.getStatus());
        assertEquals("123", localPayment.getMpPaymentId());
        verify(paymentRepository).save(localPayment);

        ArgumentCaptor<WalletCreditEvent> eventCaptor = ArgumentCaptor.forClass(WalletCreditEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_BILLING),
                eq(RabbitMQConfig.ROUTING_KEY_CREDIT),
                eventCaptor.capture()
        );

        WalletCreditEvent event = eventCaptor.getValue();
        assertEquals(advertiserId, event.advertiserId());
        assertEquals(new BigDecimal("1500.00"), event.amount());
        assertEquals("123", event.paymentId());
        assertEquals(paymentId.toString(), event.referenceId());
    }

    @Test
    void shouldIgnoreWebhookWhenLocalPaymentDoesNotExist() throws Exception {
        UUID unknownPaymentId = UUID.randomUUID();

        com.mercadopago.resources.payment.Payment mpPayment = mock(com.mercadopago.resources.payment.Payment.class);
        when(mpPayment.getExternalReference()).thenReturn(unknownPaymentId.toString());
        when(paymentClient.get(456L)).thenReturn(mpPayment);
        when(paymentRepository.findById(unknownPaymentId)).thenReturn(Optional.empty());

        webhookService.processNotification("456");

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void shouldIgnoreWebhookWhenPaymentIdIsInvalid() {
        webhookService.processNotification("abc");

        verifyNoInteractions(paymentClient, paymentRepository, rabbitTemplate);
    }
}
