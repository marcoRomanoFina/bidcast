package com.bidcast.selection_service.config;

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
    public static final String EXCHANGE_SESSION = "session.exchange";
    public static final String QUEUE_SELECTION_SESSION_CLOSED = "selection.session.closed.queue";
    public static final String ROUTING_KEY_SESSION_CLOSED = "event.sessionclosedevent";

    @Bean
    public TopicExchange selectionExchange() {
        return new TopicExchange(EXCHANGE_SELECTION);
    }

    @Bean
    public TopicExchange sessionExchange() {
        return new TopicExchange(EXCHANGE_SESSION);
    }

    @Bean
    public Queue selectionSessionClosedQueue() {
        return new Queue(QUEUE_SELECTION_SESSION_CLOSED, true);
    }

    @Bean
    public Binding bindingSelectionSessionClosed(Queue selectionSessionClosedQueue, TopicExchange sessionExchange) {
        return BindingBuilder.bind(selectionSessionClosedQueue).to(sessionExchange).with(ROUTING_KEY_SESSION_CLOSED);
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
