package com.bidcast.auction_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_AUCTION = "auction.exchange";
    public static final String EXCHANGE_DEVICE = "device.exchange";
    public static final String EXCHANGE_BILLING = "billing.exchange";
    
    public static final String QUEUE_SESSION_STARTED = "auction.session.started.queue";
    public static final String QUEUE_SESSION_CLOSED = "auction.session.closed.queue";
    
    public static final String ROUTING_KEY_SESSION_STARTED = "session.started";
    public static final String ROUTING_KEY_SESSION_CLOSED = "session.closed";
    
    public static final String ROUTING_KEY_SETTLEMENT = "session.settlement";

    @Bean
    public TopicExchange auctionExchange() {
        return new TopicExchange(EXCHANGE_AUCTION);
    }

    @Bean
    public TopicExchange deviceExchange() {
        return new TopicExchange(EXCHANGE_DEVICE);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(EXCHANGE_BILLING);
    }

    @Bean
    public Queue sessionStartedQueue() {
        return new Queue(QUEUE_SESSION_STARTED, true);
    }

    @Bean
    public Queue sessionClosedQueue() {
        return new Queue(QUEUE_SESSION_CLOSED, true);
    }

    @Bean
    public Binding bindingSessionStarted(Queue sessionStartedQueue, TopicExchange deviceExchange) {
        return BindingBuilder.bind(sessionStartedQueue).to(deviceExchange).with(ROUTING_KEY_SESSION_STARTED);
    }

    @Bean
    public Binding bindingSessionClosed(Queue sessionClosedQueue, TopicExchange deviceExchange) {
        return BindingBuilder.bind(sessionClosedQueue).to(deviceExchange).with(ROUTING_KEY_SESSION_CLOSED);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }
}
