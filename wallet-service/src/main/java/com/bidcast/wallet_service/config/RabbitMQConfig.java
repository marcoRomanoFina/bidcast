package com.bidcast.wallet_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_AUCTION = "auction.exchange";
    public static final String QUEUE_SESSION_SETTLEMENT = "wallet.session.settlement.queue";
    public static final String ROUTING_KEY_SETTLEMENT = "session.settlement";

    public static final String EXCHANGE_BILLING = "billing.exchange";
    public static final String QUEUE_CREDIT = "wallet.credit.queue";
    public static final String ROUTING_KEY_CREDIT = "payment.approved";

    @Bean
    public TopicExchange auctionExchange() {
        return new TopicExchange(EXCHANGE_AUCTION);
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
    public Binding bindingSessionSettlement(Queue sessionSettlementQueue, TopicExchange auctionExchange) {
        return BindingBuilder.bind(sessionSettlementQueue).to(auctionExchange).with(ROUTING_KEY_SETTLEMENT);
    }

    @Bean
    public Binding bindingCredit(Queue creditQueue, TopicExchange billingExchange) {
        return BindingBuilder.bind(creditQueue).to(billingExchange).with(ROUTING_KEY_CREDIT);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // En Spring Boot 4 se prefiere usar la interfaz MessageConverter
        return new JacksonJsonMessageConverter();
    }
}
