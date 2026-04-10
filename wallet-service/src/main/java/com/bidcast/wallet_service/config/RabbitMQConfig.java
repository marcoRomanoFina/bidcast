package com.bidcast.wallet_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_SELECTION = "selection.exchange";
    public static final String EXCHANGE_BILLING = "billing.exchange";
    
    public static final String QUEUE_SESSION_SETTLEMENT = "wallet.session.settlement.queue";
    public static final String QUEUE_CREDIT = "wallet.credit.queue";
    
    public static final String ROUTING_KEY_SETTLEMENT = "event.sessionsettledevent";
    public static final String ROUTING_KEY_PAYMENT_CONFIRMED = "event.paymentconfirmedevent";

    @Bean
    public TopicExchange selectionExchange() {
        return new TopicExchange(EXCHANGE_SELECTION);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(EXCHANGE_BILLING);
    }

    @Bean
    public Queue sessionSettlementQueue() {
        return new Queue(QUEUE_SESSION_SETTLEMENT, true);
    }

    @Bean
    public Queue creditQueue() {
        return new Queue(QUEUE_CREDIT, true);
    }

    @Bean
    public Binding bindingSessionSettlement(Queue sessionSettlementQueue, TopicExchange selectionExchange) {
        return BindingBuilder.bind(sessionSettlementQueue).to(selectionExchange).with(ROUTING_KEY_SETTLEMENT);
    }

    @Bean
    public Binding bindingCredit(Queue creditQueue, TopicExchange billingExchange) {
        return BindingBuilder.bind(creditQueue).to(billingExchange).with(ROUTING_KEY_PAYMENT_CONFIRMED);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
