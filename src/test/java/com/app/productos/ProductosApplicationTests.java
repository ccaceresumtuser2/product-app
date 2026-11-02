package com.app.productos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifica que el contexto de Spring arranca sin errores
 * (beans, configuración y conexión a la base H2 del perfil test).
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductosApplicationTests {

	/** Si el contexto no puede levantarse, este test falla. */
	@Test
	void contextLoads() {
	}

}
