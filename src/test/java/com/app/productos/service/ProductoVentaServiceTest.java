package com.app.productos.service;

import com.app.productos.model.ProductoVenta;
import com.app.productos.repository.ProductoVentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Test unitario de la capa de servicio de ventas; el repositorio es un mock de Mockito. */
@ExtendWith(MockitoExtension.class)
class ProductoVentaServiceTest {

    @Mock
    private ProductoVentaRepository repository;

    @InjectMocks
    private ProductoVentaService service;

    @Test
    void listar_retornaLasVentasDelRepositorio() {
        ProductoVenta venta = ProductoVenta.builder()
                .idVenta(2).nombre("Teclado").cantidad(3)
                .precio(new BigDecimal("90000")).total(new BigDecimal("270000"))
                .fecha(LocalDateTime.of(2026, 10, 24, 9, 59, 30))
                .build();
        when(repository.findAllByOrderByIdVentaAsc()).thenReturn(List.of(venta));

        assertThat(service.listar()).containsExactly(venta);
    }

    @Test
    void listar_sinVentas_retornaListaVacia() {
        when(repository.findAllByOrderByIdVentaAsc()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
