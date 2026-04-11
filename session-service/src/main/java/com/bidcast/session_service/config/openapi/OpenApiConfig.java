package com.bidcast.session_service.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sessionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Session Service API")
                        .description("API to create venue sessions, manage device presence, and close inactive or completed sessions.")
                        .version("v0.0.1"));
    }
}
