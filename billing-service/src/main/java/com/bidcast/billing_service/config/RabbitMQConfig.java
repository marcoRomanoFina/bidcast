package com.bidcast.billing_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_BILLING = "billing.exchange";
    public static final String ROUTING_KEY_CREDIT = "payment.approved";

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(EXCHANGE_BILLING);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
