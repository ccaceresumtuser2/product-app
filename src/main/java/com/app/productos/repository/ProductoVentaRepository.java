package com.app.productos.repository;

import com.app.productos.model.ProductoVenta;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Acceso de solo lectura a {@code productos_ventas_view}.
 * Extiende {@link Repository} (no JpaRepository) para no exponer save ni delete sobre una vista.
 */
public interface ProductoVentaRepository extends Repository<ProductoVenta, Integer> {

    /** Todas las ventas, de la más antigua a la más reciente: {@code ORDER BY id_venta}. */
    List<ProductoVenta> findAllByOrderByIdVentaAsc();
}
