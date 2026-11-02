package com.app.productos.controller;

import com.app.productos.model.ProductoVenta;
import com.app.productos.service.ProductoVentaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test de la capa web de ventas; el servicio es un mock de Mockito (@MockitoBean). */
@WebMvcTest(ProductoVentaController.class)
class ProductoVentaControllerTest {

    private static final String URL = "/api/ventas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoVentaService service;

    @Test
    void listar_retorna200ConVentas() throws Exception {
        ProductoVenta venta = ProductoVenta.builder()
                .idVenta(2).nombre("Teclado").cantidad(3)
                .precio(new BigDecimal("90000.00")).total(new BigDecimal("270000.00"))
                .fecha(LocalDateTime.of(2026, 10, 24, 9, 59, 30))
                .build();
        when(service.listar()).thenReturn(List.of(venta));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].idVenta").value(2))
                .andExpect(jsonPath("$.data[0].nombre").value("Teclado"))
                .andExpect(jsonPath("$.data[0].cantidad").value(3))
                .andExpect(jsonPath("$.data[0].total").value(270000.00))
                .andExpect(jsonPath("$.data[0].fecha").value("2026-10-24T09:59:30"));
    }

    @Test
    void listar_sinVentas_retornaDataVacia() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void listar_sinConexionABaseDeDatos_retorna503() throws Exception {
        // GlobalExceptionHandler aplica a todos los controllers, también a este
        when(service.listar()).thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager"));

        mockMvc.perform(get(URL))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503));
    }
}
