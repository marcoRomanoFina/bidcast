package com.bidcast.user_service.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bidcast User Service API")
                        .description("""
                                API responsible for identity and authentication within Bidcast.

                                It exposes public endpoints to:
                                - register new users
                                - authenticate valid credentials
                                - issue signed JWTs for use by the gateway and other services

                                The `/api/auth/register` and `/api/auth/login` endpoints do not require prior authentication.
                                """)
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Bidcast")
                                .url("https://bidcast.local"))
                        .license(new License()
                                .name("Bidcast internal use")));
    }
}
