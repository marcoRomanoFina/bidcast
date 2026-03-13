package com.bidcast.auction_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_AUCTION = "auction.exchange";
    public static final String EXCHANGE_DEVICE = "device.exchange";
    
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
}
