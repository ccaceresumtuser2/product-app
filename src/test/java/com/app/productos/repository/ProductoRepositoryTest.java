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
 */
@DataJpaTest
@ActiveProfiles("test")
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository repository;

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

        assertThat(guardado.getId()).isNotNull();
        assertThat(repository.findById(guardado.getId())).isPresent();
    }

    @Test
    void findByNombreContainingIgnoreCase_ignoraMayusculas() {
        List<Producto> resultado = repository.findByNombreContainingIgnoreCase("mouse");

        assertThat(resultado)
                .extracting(Producto::getNombre)
                .containsExactlyInAnyOrder("Mouse inalámbrico", "MOUSE gamer");
    }

    @Test
    void findByNombreContainingIgnoreCase_sinCoincidencias_retornaVacio() {
        assertThat(repository.findByNombreContainingIgnoreCase("silla")).isEmpty();
    }

    private static Producto producto(String nombre) {
        return Producto.builder()
                .nombre(nombre).precio(new BigDecimal("1000")).stock(1)
                .build();
    }
}
