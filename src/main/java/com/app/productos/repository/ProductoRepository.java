package com.app.productos.repository;

import com.app.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
