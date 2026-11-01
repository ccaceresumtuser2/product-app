package com.app.productos.service;

import com.app.productos.exception.RecursoNoEncontradoException;
import com.app.productos.model.Producto;
import com.app.productos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repository;

    @Transactional(readOnly = true)
    public List<Producto> listar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return repository.findAll();
        }
        //return repository.findByNombreContainingIgnoreCase(nombre);
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
    }

    @Transactional
    public Producto crear(Producto producto) {
        producto.setId(null);
        return repository.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, Producto datos) {
        Producto producto = obtener(id);
        producto.setNombre(datos.getNombre());
        producto.setDescripcion(datos.getDescripcion());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());
        return repository.save(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtener(id));
    }
}
