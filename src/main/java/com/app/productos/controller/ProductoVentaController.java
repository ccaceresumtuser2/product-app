package com.app.productos.controller;

import com.app.productos.model.ProductoVenta;
import com.app.productos.response.ApiResponse;
import com.app.productos.service.ProductoVentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Capa web: expone en {@code /api/ventas} la vista {@code productos_ventas_view}.
 * Solo lectura; la respuesta va envuelta en {@link ApiResponse}.
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor // Lombok genera el constructor para inyectar el servicio
@Tag(name = "Ventas", description = "Consulta de ventas (vista productos_ventas_view)")
public class ProductoVentaController {

    private final ProductoVentaService service;

    /** GET /api/ventas — todas las ventas con producto, cantidad, precio, total y fecha. */
    @GetMapping
    @Operation(summary = "Listar ventas con su producto y total")
    public ApiResponse<List<ProductoVenta>> listar() {
        return ApiResponse.ok(service.listar());
    }
}
