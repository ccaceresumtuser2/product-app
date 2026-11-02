package com.app.productos.service;

import com.app.productos.model.ProductoVenta;
import com.app.productos.repository.ProductoVentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Capa de negocio de ventas: solo consulta la vista {@code productos_ventas_view}. */
@Service
@RequiredArgsConstructor // Lombok genera el constructor para inyectar el repositorio
public class ProductoVentaService {

    private final ProductoVentaRepository repository;

    /** Lista todas las ventas con el nombre del producto y el total, ordenadas por id de venta. */
    @Transactional(readOnly = true)
    public List<ProductoVenta> listar() {
        return repository.findAllByOrderByIdVentaAsc();
    }
}
