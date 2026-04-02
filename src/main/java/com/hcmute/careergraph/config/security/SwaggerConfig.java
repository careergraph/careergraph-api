package com.hcmute.careergraph.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

        @Value("${SWAGGER_SERVER_URL:}")
        private String swaggerServerUrl;

        @Bean
        public OpenAPI openAPI() {
                OpenAPI openAPI = new OpenAPI()
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new Components()
                                                .addSecuritySchemes(
                                                                "bearerAuth",
                                                                new SecurityScheme()
                                                                                .name("bearerAuth")
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")));

                if (swaggerServerUrl != null && !swaggerServerUrl.isBlank()) {
                        openAPI.addServersItem(new Server().url(swaggerServerUrl.trim()));
                }

                return openAPI;
        }
}