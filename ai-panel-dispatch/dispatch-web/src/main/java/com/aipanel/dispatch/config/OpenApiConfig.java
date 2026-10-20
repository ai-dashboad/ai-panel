package com.aipanel.dispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger Configuration
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:dispatch-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI Panel Dispatch API")
                .description("AI Model Dispatch and Routing Service - OpenAI Compatible API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("AI Panel Team")
                    .email("support@aipanel.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("/")
                    .description("Current Server"),
                new Server()
                    .url("http://localhost:8082")
                    .description("Local Development")
            ));
    }
}
