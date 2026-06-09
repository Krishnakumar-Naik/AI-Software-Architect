package com.aisoftwarearchitect.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Software Architect API")
                        .version("1.0.0")
                        .description("API documentation for the AI Software Architect platform, including JWT cookie-based authentication, user profiles, and metrics.")
                        .contact(new Contact()
                                .name("AI Software Architect Team")
                                .email("support@aisoftwarearchitect.com")));
    }
}
