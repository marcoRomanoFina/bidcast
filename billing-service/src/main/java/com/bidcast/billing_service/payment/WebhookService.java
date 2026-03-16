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

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentClient paymentClient;

    @Transactional
    public void processNotification(String paymentId) {
        log.info("Procesando notificación de pago de Mercado Pago: {}", paymentId);

        try {
            // 1. Consultamos el estado real del pago en Mercado Pago (Double-Check)
            // Se inyecta el client directamente desde MercadoPagoConfiguration
            Payment mpPayment = paymentClient.get(Long.parseLong(paymentId));
            String externalReference = mpPayment.getExternalReference();
            
            if (externalReference == null) {
                log.error("El pago {} no tiene 'external_reference'. Ignorando.", paymentId);
                return;
            }

            // 2. Buscamos el registro local en nuestra DB
            com.bidcast.billing_service.payment.Payment localPayment = 
                paymentRepository.findById(java.util.UUID.fromString(externalReference))
                    .orElseThrow(() -> new RuntimeException("Pago no encontrado para external_reference: " + externalReference));

            // 3. IDEMPOTENCIA: Si ya está aprobado, salimos inmediatamente.
            if (localPayment.getStatus() == PaymentStatus.APPROVED) {
                log.info("Pago {} ya estaba procesado (APPROVED). No hacemos nada.", externalReference);
                return;
            }

            // 4. Si el pago está aprobado en MP, actualizamos y disparamos crédito
            if ("approved".equalsIgnoreCase(mpPayment.getStatus())) {
                log.info("Pago {} verificado como APROBADO en MP.", paymentId);
                
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
                
                log.info("Crédito de {} enviado a RabbitMQ para advertiser: {}", 
                         localPayment.getAmount(), localPayment.getAdvertiserId());

            } else {
                log.warn("El pago {} tiene estado {} en MP. No se acredita saldo.", 
                         paymentId, mpPayment.getStatus());
            }

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            log.warn("Colisión de concurrencia detectada para el pago {}. Ya procesado.", paymentId);
        } catch (Exception e) {
            log.error("Error crítico procesando webhook {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Error en procesamiento de webhook", e);
        }
    }
}
