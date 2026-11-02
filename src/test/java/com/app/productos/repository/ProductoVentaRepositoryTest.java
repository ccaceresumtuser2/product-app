package com.app.productos.repository;

import com.app.productos.model.ProductoVenta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test del repositorio de ventas contra H2.
 * En H2 no existe la vista real: Hibernate crea una tabla {@code productos_ventas_view}
 * desde la entidad (create-drop), así que se prueba el mapeo de columnas y el orden.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProductoVentaRepositoryTest {

    @Autowired
    private ProductoVentaRepository repository;

    /** Para cargar filas de prueba: el repositorio es de solo lectura y no tiene save. */
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findAllByOrderByIdVentaAsc_mapeaColumnas_yOrdenaPorIdVenta() {
        // se insertan desordenadas para comprobar el ORDER BY
        entityManager.persist(venta(5, "Cable HDMI 2m", 10, "25000", "250000"));
        entityManager.persist(venta(2, "Teclado", 3, "90000", "270000"));
        entityManager.flush();
        entityManager.clear(); // obliga a releer desde la base

        List<ProductoVenta> ventas = repository.findAllByOrderByIdVentaAsc();

        assertThat(ventas).extracting(ProductoVenta::getIdVenta).containsExactly(2, 5);
        ProductoVenta teclado = ventas.getFirst();
        assertThat(teclado.getNombre()).isEqualTo("Teclado");
        assertThat(teclado.getCantidad()).isEqualTo(3);
        assertThat(teclado.getTotal()).isEqualByComparingTo("270000");
        assertThat(teclado.getFecha()).isEqualTo(LocalDateTime.of(2026, 10, 24, 9, 59, 30));
    }

    @Test
    void findAllByOrderByIdVentaAsc_sinVentas_retornaVacio() {
        assertThat(repository.findAllByOrderByIdVentaAsc()).isEmpty();
    }

    private static ProductoVenta venta(int id, String nombre, int cantidad, String precio, String total) {
        return ProductoVenta.builder()
                .idVenta(id).nombre(nombre).cantidad(cantidad)
                .precio(new BigDecimal(precio)).total(new BigDecimal(total))
                .fecha(LocalDateTime.of(2026, 10, 24, 9, 59, 30))
                .build();
    }
}
