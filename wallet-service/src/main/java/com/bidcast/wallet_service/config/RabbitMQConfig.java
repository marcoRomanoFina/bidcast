package com.bidcast.wallet_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_AUCTION = "auction.exchange";
    
    public static final String QUEUE_SESSION_SETTLEMENT = "wallet.session.settlement.queue";
    public static final String ROUTING_KEY_SETTLEMENT = "session.settlement";

    @Bean
    public TopicExchange auctionExchange() {
        return new TopicExchange(EXCHANGE_AUCTION);
    }

    @Bean
    public Queue sessionSettlementQueue() {
        return new Queue(QUEUE_SESSION_SETTLEMENT, true);
    }

    @Bean
    public Binding bindingSessionSettlement(Queue sessionSettlementQueue, TopicExchange auctionExchange) {
        return BindingBuilder.bind(sessionSettlementQueue).to(auctionExchange).with(ROUTING_KEY_SETTLEMENT);
    }
}
