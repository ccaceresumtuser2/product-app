package com.app.productos.repository;

import com.app.productos.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de la capa de persistencia contra una base H2 en memoria.
 * Levanta solo JPA; cada test corre en una transacción que se revierte al final.
 *
 * Aquí no se usan mocks: lo que se quiere probar es la consulta SQL real.
 */
@DataJpaTest                // solo entidades, repositorios y una base embebida
@ActiveProfiles("test")     // usa application-test.properties (H2, create-drop)
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository repository;

    /** Datos de prueba; se descartan al terminar cada test (rollback). */
    @BeforeEach
    void setUp() {
        repository.saveAll(List.of(
                producto("Mouse inalámbrico"),
                producto("MOUSE gamer"),
                producto("Teclado")));
    }

    @Test
    void save_generaId() {
        Producto guardado = repository.save(producto("Monitor"));

        // el id lo asigna la base (IDENTITY)
        assertThat(guardado.getId()).isNotNull();
        assertThat(repository.findById(guardado.getId())).isPresent();
    }

    @Test
    void findByNombreContainingIgnoreCase_ignoraMayusculas() {
        List<Producto> resultado = repository.findByNombreContainingIgnoreCase("mouse");

        // encuentra "Mouse..." y "MOUSE..." pero no "Teclado"
        assertThat(resultado)
                .extracting(Producto::getNombre)
                .containsExactlyInAnyOrder("Mouse inalámbrico", "MOUSE gamer");
    }

    @Test
    void findByNombreContainingIgnoreCase_sinCoincidencias_retornaVacio() {
        assertThat(repository.findByNombreContainingIgnoreCase("silla")).isEmpty();
    }

    /** Crea un producto válido con el nombre indicado. */
    private static Producto producto(String nombre) {
        return Producto.builder()
                .nombre(nombre).precio(new BigDecimal("1000")).stock(1)
                .build();
    }
}
