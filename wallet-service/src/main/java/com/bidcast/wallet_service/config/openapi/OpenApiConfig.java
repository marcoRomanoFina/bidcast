package com.bidcast.wallet_service.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI walletServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Service (Ledger) API")
                        .description("API del ledger financiero de Bidcast. Gestiona billeteras, fondos congelados, creditos idempotentes y settlement contable entre advertiser, publisher y plataforma.")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Bidcast")
                                .email("dev@bidcast.local"))
                        .license(new License()
                                .name("Uso academico / portfolio")));
    }
}
