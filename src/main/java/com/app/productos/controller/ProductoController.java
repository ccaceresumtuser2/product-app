package com.app.productos.controller;

import com.app.productos.model.Producto;
import com.app.productos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Capa web: expone el CRUD de productos en {@code /api/productos}.
 * Solo recibe la petición, valida el cuerpo y delega en {@link ProductoService}.
 * Los errores (404, 400) los transforma {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor // Lombok genera el constructor para inyectar el servicio
@Tag(name = "Productos", description = "CRUD de productos")
public class ProductoController {

    private final ProductoService service;

    /** GET /api/productos[?nombre=] — lista todos o filtra por nombre. */
    @GetMapping
    @Operation(summary = "Listar productos (filtro opcional por nombre)")
    public List<Producto> listar(@RequestParam(required = false) String nombre) {
        return service.listar(nombre);
    }

    /** GET /api/productos/{id} — responde 404 si no existe. */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por id")
    public Producto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** POST /api/productos — {@code @Valid} aplica las validaciones de la entidad (400 si fallan). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear producto")
    public Producto crear(@Valid @RequestBody Producto producto) {
        return service.crear(producto);
    }

    /** PUT /api/productos/{id} — reemplaza los datos del producto. */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        return service.actualizar(id, producto);
    }

    /** DELETE /api/productos/{id} — responde 204 sin cuerpo. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar producto")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
