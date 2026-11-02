package com.app.productos.service;

import com.app.productos.exception.ConflictoException;
import com.app.productos.exception.RecursoNoEncontradoException;
import com.app.productos.model.Producto;
import com.app.productos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * Lista los productos con precio entre {@code min} y {@code max}, ambos incluidos.
     *
     * @throws IllegalArgumentException si {@code min} es mayor que {@code max} (se responde 400)
     */
    @Transactional(readOnly = true)
    public List<Producto> listarPorRangoPrecio(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("El precio mínimo no puede ser mayor que el máximo");
        }
        return repository.findByPrecioBetweenOrderByPrecioAsc(min, max);
    }

    /**
     * Lista los productos creados entre {@code desde} y {@code hasta}, ambos días completos incluidos.
     *
     * @throws IllegalArgumentException si {@code desde} es posterior a {@code hasta} (se responde 400)
     */
    @Transactional(readOnly = true)
    public List<Producto> listarPorFechaCreacion(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        // [desde 00:00, día siguiente a hasta 00:00) cubre el día "hasta" completo
        return repository.buscarCreadosEntre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
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
        return guardarCambios(obtener(id), datos);
    }

    /**
     * Actualiza el producto cuyo nombre coincide exactamente con {@code nombre} (sin distinguir mayúsculas).
     * El cuerpo puede traer un nombre nuevo, así que también sirve para renombrar.
     *
     * @throws RecursoNoEncontradoException si ningún producto tiene ese nombre (se responde 404)
     * @throws ConflictoException si hay más de uno con ese nombre (se responde 409): editar sería ambiguo
     */
    @Transactional
    public Producto actualizarPorNombre(String nombre, Producto datos) {
        List<Producto> encontrados = repository.findByNombreIgnoreCase(nombre.trim());
        if (encontrados.isEmpty()) {
            throw new RecursoNoEncontradoException("Producto '" + nombre + "' no encontrado");
        }
        if (encontrados.size() > 1) {
            throw new ConflictoException("Hay " + encontrados.size() + " productos llamados '" + nombre
                    + "'; use PUT /api/productos/{id}");
        }
        return guardarCambios(encontrados.getFirst(), datos);
    }

    /** Copia nombre, descripción, precio y stock; id y creadoEn no se tocan. */
    private Producto guardarCambios(Producto producto, Producto datos) {
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
