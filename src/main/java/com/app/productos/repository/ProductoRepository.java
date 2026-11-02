package com.app.productos.repository;

import com.app.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Capa de acceso a datos.
 * {@link JpaRepository} ya provee findAll, findById, save, delete, count, etc.;
 * Spring Data genera la implementación en tiempo de ejecución.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Busca productos cuyo nombre contenga el texto, sin distinguir mayúsculas.
     * Spring Data deriva la consulta del nombre del método:
     * {@code WHERE UPPER(nombre) LIKE UPPER('%texto%')}.
     */
    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Busca productos cuyo nombre sea exactamente el texto, sin distinguir mayúsculas:
     * {@code WHERE UPPER(nombre) = UPPER('texto')}. Devuelve lista porque la columna no es única.
     */
    List<Producto> findByNombreIgnoreCase(String nombre);

    /**
     * Busca productos con precio dentro del rango (ambos extremos incluidos), del más barato al más caro.
     * Consulta derivada: {@code WHERE precio BETWEEN min AND max ORDER BY precio ASC}.
     */
    List<Producto> findByPrecioBetweenOrderByPrecioAsc(BigDecimal min, BigDecimal max);

    /**
     * Busca productos creados en {@code [desde, hasta)}, del más antiguo al más reciente.
     * Se usa {@code < hasta} (y no BETWEEN) para que el servicio pueda pasar el inicio del día
     * siguiente y así incluir completo el último día.
     */
    @Query("SELECT p FROM Producto p WHERE p.creadoEn >= :desde AND p.creadoEn < :hasta ORDER BY p.creadoEn")
    List<Producto> buscarCreadosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
