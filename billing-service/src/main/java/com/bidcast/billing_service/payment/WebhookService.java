package com.bidcast.billing_service.payment;

import com.bidcast.billing_service.config.RabbitMQConfig;
import com.bidcast.billing_service.payment.event.WalletCreditEvent;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentClient paymentClient;

    @Transactional
    public void processNotification(String paymentId) {
        log.info("Processing Mercado Pago payment notification: {}", paymentId);

        try {
            if (paymentId == null || paymentId.isBlank()) {
                log.warn("Webhook ignored: empty paymentId");
                return;
            }

            // 1. Consultamos el estado real del pago en Mercado Pago (Double-Check)
            // Se inyecta el client directamente desde MercadoPagoConfiguration
            Payment mpPayment = paymentClient.get(Long.parseLong(paymentId));
            String externalReference = mpPayment.getExternalReference();
            
            if (externalReference == null) {
                log.error("Payment {} has no 'external_reference'. Ignoring.", paymentId);
                return;
            }

            // 2. Buscamos el registro local en nuestra DB
            UUID localPaymentId;
            try {
                localPaymentId = UUID.fromString(externalReference);
            } catch (IllegalArgumentException e) {
                log.warn("Payment {} has an invalid external_reference: {}", paymentId, externalReference);
                return;
            }

            // Si el pago no nació en nuestro sistema, ignoramos el evento sin romper el flujo de MP.
            Optional<com.bidcast.billing_service.payment.Payment> localPaymentOptional = paymentRepository.findById(localPaymentId);
            if (localPaymentOptional.isEmpty()) {
                log.warn("No local payment exists for external_reference={}. Ignoring webhook.", externalReference);
                return;
            }

            com.bidcast.billing_service.payment.Payment localPayment = localPaymentOptional.get();

            // 3. IDEMPOTENCIA: Si ya está aprobado, salimos inmediatamente.
            if (localPayment.getStatus() == PaymentStatus.APPROVED) {
                log.info("Payment {} was already processed (APPROVED). No action taken.", externalReference);
                return;
            }

            // 4. Si el pago está aprobado en MP, actualizamos y disparamos crédito
            if ("approved".equalsIgnoreCase(mpPayment.getStatus())) {
                log.info("Payment {} verified as APPROVED in Mercado Pago.", paymentId);
                
                localPayment.setStatus(PaymentStatus.APPROVED);
                localPayment.setMpPaymentId(paymentId);
                
                // Al persistir, si otro hilo ya lo hizo, @Version lanzará OptimisticLockingFailureException
                // y Spring hará rollback automático.
                paymentRepository.save(localPayment);

                // 5. EVENTO DE CRÉDITO (Event-Driven Architecture)
                WalletCreditEvent event = new WalletCreditEvent(
                        localPayment.getAdvertiserId(),
                        localPayment.getAmount(),
                        paymentId,
                        localPayment.getId().toString()
                );
                
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_BILLING,
                        RabbitMQConfig.ROUTING_KEY_CREDIT,
                        event
                );
                
                log.info("Credit of {} sent to RabbitMQ for advertiser: {}", 
                         localPayment.getAmount(), localPayment.getAdvertiserId());

            } else {
                log.warn("Payment {} has status {} in Mercado Pago. No balance will be credited.", 
                         paymentId, mpPayment.getStatus());
            }

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrency collision detected for payment {}. Already processed.", paymentId);
        } catch (NumberFormatException e) {
            log.warn("Webhook ignored: invalid paymentId={}", paymentId);
        } catch (Exception e) {
            log.error("Critical error while processing webhook {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Webhook processing error", e);
        }
    }
}
