package com.app.productos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productosOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API Productos")
                .description("API REST para la gestión de productos - trigg_db")
                .version("1.0.0"));
    }
}
