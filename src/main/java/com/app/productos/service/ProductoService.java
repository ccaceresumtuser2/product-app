package com.app.productos.service;

import com.app.productos.exception.RecursoNoEncontradoException;
import com.app.productos.model.Producto;
import com.app.productos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Capa de negocio de productos.
 * Contiene las reglas (filtro, "no encontrado", qué campos se actualizan)
 * y maneja las transacciones; el acceso a datos lo hace {@link ProductoRepository}.
 */
@Service
@RequiredArgsConstructor // Lombok genera el constructor para inyectar el repositorio
public class ProductoService {

    private final ProductoRepository repository;

    /**
     * Lista todos los productos, o solo los que contienen {@code nombre} si se indica.
     * Un filtro nulo o en blanco equivale a no filtrar.
     */
    @Transactional(readOnly = true)
    public List<Producto> listar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return repository.findAll();
        }
        return repository.findByNombreContainingIgnoreCase(nombre);
    }

    /**
     * Obtiene un producto por id.
     *
     * @throws RecursoNoEncontradoException si no existe (se responde 404)
     */
    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
    }

    /** Crea un producto. El id siempre lo genera la base, por eso se ignora el recibido. */
    @Transactional
    public Producto crear(Producto producto) {
        producto.setId(null);
        return repository.save(producto);
    }

    /**
     * Actualiza nombre, descripción, precio y stock de un producto existente.
     *
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional
    public Producto actualizar(Long id, Producto datos) {
        Producto producto = obtener(id);
        producto.setNombre(datos.getNombre());
        producto.setDescripcion(datos.getDescripcion());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());
        return repository.save(producto);
    }

    /**
     * Elimina un producto existente.
     *
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtener(id));
    }
}
