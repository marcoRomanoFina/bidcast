package com.bidcast.session_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_SESSION = "session.exchange";
    public static final String ROUTING_KEY_SESSION_CLOSED = "event.sessionclosedevent";

    @Bean
    public TopicExchange sessionExchange() {
        return new TopicExchange(EXCHANGE_SESSION);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
