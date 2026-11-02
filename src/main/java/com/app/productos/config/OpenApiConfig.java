package com.app.productos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger).
 * Define el título, descripción y versión que se muestran en Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    /** Metadatos de la API publicados en {@code /v3/api-docs} y {@code /swagger-ui.html}. */
    @Bean
    public OpenAPI productosOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API Productos")
                .description("API REST para la gestión de productos - trigg_db")
                .version("1.0.0"));
    }
}
