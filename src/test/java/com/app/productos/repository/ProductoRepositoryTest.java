package com.app.productos.repository;

import com.app.productos.model.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /** Para ejecutar SQL directo en la misma transacción del test. */
    @PersistenceContext
    private EntityManager entityManager;

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

    @Test
    void findByPrecioBetween_incluyeExtremos_yOrdenaPorPrecio() {
        repository.deleteAll();
        repository.saveAll(List.of(
                producto("Monitor", "150000"),
                producto("Cable", "5000"),
                producto("Mouse", "15990"),
                producto("Teclado", "30000")));

        List<Producto> resultado = repository.findByPrecioBetweenOrderByPrecioAsc(
                new BigDecimal("5000"), new BigDecimal("30000"));

        // 5000 y 30000 son los extremos: BETWEEN los incluye; el Monitor queda fuera
        assertThat(resultado)
                .extracting(Producto::getNombre)
                .containsExactly("Cable", "Mouse", "Teclado");
    }

    @Test
    void save_asignaCreadoEn() {
        Producto guardado = repository.save(producto("Monitor"));

        // @CreationTimestamp lo completa al insertar
        assertThat(guardado.getCreadoEn()).isNotNull();
    }

    @Test
    void buscarCreadosEntre_incluyeDesde_excluyeHasta_yOrdenaPorFecha() {
        repository.deleteAll();
        Producto septiembre = repository.save(producto("Septiembre"));
        Producto inicioOctubre = repository.save(producto("Inicio octubre"));
        Producto finOctubre = repository.save(producto("Fin octubre"));
        Producto noviembre = repository.save(producto("Noviembre"));
        // creado_en no es actualizable desde la entidad: se fija con SQL directo
        fijarCreadoEn(septiembre, "2026-09-30T23:59:59");
        fijarCreadoEn(inicioOctubre, "2026-10-01T00:00:00");
        fijarCreadoEn(finOctubre, "2026-10-31T23:59:59");
        fijarCreadoEn(noviembre, "2026-11-01T00:00:00");

        List<Producto> resultado = repository.buscarCreadosEntre(
                LocalDateTime.parse("2026-10-01T00:00:00"), LocalDateTime.parse("2026-11-01T00:00:00"));

        // 1 oct 00:00 entra (>=), 1 nov 00:00 queda fuera (<)
        assertThat(resultado)
                .extracting(Producto::getNombre)
                .containsExactly("Inicio octubre", "Fin octubre");
    }

    /** Cambia creado_en directo en la tabla y limpia la caché de JPA para releer el valor real. */
    private void fijarCreadoEn(Producto p, String fecha) {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE productos SET creado_en = :fecha WHERE id = :id")
                .setParameter("fecha", LocalDateTime.parse(fecha))
                .setParameter("id", p.getId())
                .executeUpdate();
        entityManager.clear();
    }

    /** Crea un producto válido con el nombre indicado. */
    private static Producto producto(String nombre) {
        return producto(nombre, "1000");
    }

    /** Crea un producto válido con el nombre y precio indicados. */
    private static Producto producto(String nombre, String precio) {
        return Producto.builder()
                .nombre(nombre).precio(new BigDecimal(precio)).stock(1)
                .build();
    }
}
