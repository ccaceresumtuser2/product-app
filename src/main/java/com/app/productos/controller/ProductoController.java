package com.app.productos.controller;

import com.app.productos.model.Producto;
import com.app.productos.response.ApiResponse;
import com.app.productos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Capa web: expone el CRUD de productos en {@code /api/productos}.
 * Solo recibe la petición, valida el cuerpo y delega en {@link ProductoService}.
 * Toda respuesta exitosa va envuelta en {@link ApiResponse};
 * los errores (404, 400) los transforma {@code GlobalExceptionHandler}.
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
    public ApiResponse<List<Producto>> listar(@RequestParam(required = false) String nombre) {
        return ApiResponse.ok(service.listar(nombre));
    }

    /**
     * GET /api/productos/precio?min=&max= — productos con precio en el rango (incluido).
     * Responde 400 si falta un parámetro o si min es mayor que max.
     */
    @GetMapping("/precio")
    @Operation(summary = "Listar productos por rango de precio")
    public ApiResponse<List<Producto>> listarPorRangoPrecio(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        return ApiResponse.ok(service.listarPorRangoPrecio(min, max));
    }

    /** GET /api/productos/{id} — responde 404 si no existe. */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por id")
    public ApiResponse<Producto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(service.obtener(id));
    }

    /** POST /api/productos — {@code @Valid} aplica las validaciones de la entidad (400 si fallan). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear producto")
    public ApiResponse<Producto> crear(@Valid @RequestBody Producto producto) {
        return ApiResponse.ok(service.crear(producto), "Producto creado");
    }

    /** PUT /api/productos/{id} — reemplaza los datos del producto. */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public ApiResponse<Producto> actualizar(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        return ApiResponse.ok(service.actualizar(id, producto), "Producto actualizado");
    }

    /** DELETE /api/productos/{id} — responde 200 con {@code data: null} para mantener el formato estándar. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ApiResponse.ok(null, "Producto eliminado");
    }
}
