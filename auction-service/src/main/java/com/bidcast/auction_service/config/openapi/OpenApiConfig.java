package com.bidcast.auction_service.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI auctionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auction Service (RTB Engine) API")
                        .description("API del motor de subastas en tiempo real de Bidcast")
                        .version("v0.0.1"));
    }
}
