package com.bidcast.billing_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_BILLING = "billing.exchange";
    public static final String QUEUE_CREDIT = "wallet.credit.queue";
    public static final String ROUTING_KEY_CREDIT = "payment.approved";

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(EXCHANGE_BILLING);
    }

    @Bean
    public Queue creditQueue() {
        return new Queue(QUEUE_CREDIT);
    }

    @Bean
    public Binding creditBinding(Queue creditQueue, TopicExchange billingExchange) {
        return BindingBuilder.bind(creditQueue).to(billingExchange).with(ROUTING_KEY_CREDIT);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
