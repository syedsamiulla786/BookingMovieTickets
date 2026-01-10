package com.showtime.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .openapi("3.0.3")  // THIS IS THE CRITICAL LINE - Sets OpenAPI version
            .info(new Info()
                .title("ShowTime Movie Booking API")
                .version("1.0.0")
                .description("Complete REST API for Movie Ticket Booking System (BookMyShow Clone)")
                .termsOfService("https://showtime.com/terms")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@showtime.com")
                    .url("https://showtime.com/contact"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api")
                    .description("Development Server"),
                new Server()
                    .url("https://api.showtime.com")
                    .description("Production Server")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token (without 'Bearer ' prefix)")));
    }
}