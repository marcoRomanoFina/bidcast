package com.bidcast.billing_service.infrastructure.event;

import com.bidcast.billing_service.config.RabbitMQConfig;
import com.bidcast.billing_service.core.event.DomainEvent;
import com.bidcast.billing_service.core.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura para RabbitMQ.
 * Implementa el puerto EventPublisher traduciendo eventos de dominio a mensajes de RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        // En este diseño, usamos el nombre de la clase del evento como parte de la routing key
        // para que sea fácil de filtrar por los consumidores.
        String routingKey = "event." + event.getClass().getSimpleName().toLowerCase();
        
        log.info("Publishing event {} to exchange {} with routing key {}", 
                 event.getClass().getSimpleName(), RabbitMQConfig.EXCHANGE_BILLING, routingKey);
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_BILLING,
                routingKey,
                event
        );
    }
}
