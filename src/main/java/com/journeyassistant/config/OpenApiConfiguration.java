package com.journeyassistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI journeyAssistantOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Journey Assistant API")
                .version("v1")
                .description("Journey lifecycle, location updates, provider callbacks, and local task endpoints. "
                        + "Use the provider-references endpoint to fetch the latest call and message IDs."));
    }
}
