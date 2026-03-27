package com.bidcast.billing_service.config.mercadopago;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        log.info("Configuring Mercado Pago SDK v2.8.0...");
        
        MercadoPagoConfig.setAccessToken(accessToken);
        MercadoPagoConfig.setConnectionTimeout(5000); 
        MercadoPagoConfig.setConnectionRequestTimeout(5000);
    
        log.info("Mercado Pago SDK configured with timeout=5s");
    }

    @Bean
    public PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }
}
