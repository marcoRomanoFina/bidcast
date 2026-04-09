package com.bidcast.selection_service.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI selectionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Selection Service API")
                        .description("API to register session offers, select already reserved paid plays, and confirm Proof of Play.")
                        .version("v0.0.1"));
    }
}
