package com.app.productos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación.
 * {@code @SpringBootApplication} activa la autoconfiguración y el escaneo de componentes
 * en este paquete y sus subpaquetes (controller, service, repository, etc.).
 */
@SpringBootApplication
public class ProductosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductosApplication.class, args);
	}

}
