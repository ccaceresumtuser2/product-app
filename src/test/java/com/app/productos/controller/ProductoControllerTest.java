package com.app.productos.controller;

import com.app.productos.exception.RecursoNoEncontradoException;
import com.app.productos.model.Producto;
import com.app.productos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de la capa web (controller + GlobalExceptionHandler + validaciones).
 * Levanta solo la capa MVC; el servicio es un mock de Mockito (@MockitoBean).
 *
 * Se verifica lo que le importa al cliente HTTP: código de estado y JSON de respuesta.
 */
@WebMvcTest(ProductoController.class) // carga solo este controller y la config web
class ProductoControllerTest {

    private static final String URL = "/api/productos";

    /** Simula peticiones HTTP sin levantar un servidor real. */
    @Autowired
    private MockMvc mockMvc;

    /** Spring registra este mock en el contexto en lugar del ProductoService real. */
    @MockitoBean
    private ProductoService service;

    private final Producto mouse = Producto.builder()
            .id(1L).nombre("Mouse").descripcion("Inalámbrico")
            .precio(new BigDecimal("15990")).stock(10)
            .build();

    // ---------- GET /api/productos ----------

    @Test
    void listar_retorna200ConLista() throws Exception {
        when(service.listar(null)).thenReturn(List.of(mouse));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                // jsonPath navega el JSON de respuesta: $ es la raíz
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Mouse"));
    }

    @Test
    void listar_conFiltro_pasaNombreAlServicio() throws Exception {
        when(service.listar("mou")).thenReturn(List.of(mouse));

        mockMvc.perform(get(URL).param("nombre", "mou"))
                .andExpect(status().isOk());

        // el query param ?nombre= llega tal cual al servicio
        verify(service).listar("mou");
    }

    // ---------- GET /api/productos/{id} ----------

    @Test
    void obtener_existente_retorna200() throws Exception {
        when(service.obtener(1L)).thenReturn(mouse);

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.precio").value(15990));
    }

    @Test
    void obtener_inexistente_retorna404ProblemDetail() throws Exception {
        // el mock lanza la excepción; GlobalExceptionHandler debe convertirla en 404
        when(service.obtener(99L)).thenThrow(new RecursoNoEncontradoException("Producto 99 no encontrado"));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Producto 99 no encontrado"));
    }

    // ---------- POST /api/productos ----------

    @Test
    void crear_valido_retorna201() throws Exception {
        when(service.crear(any(Producto.class))).thenReturn(mouse);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse","descripcion":"Inalámbrico","precio":15990,"stock":10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crear_invalido_retorna400ConErrores_yNoLlamaAlServicio() throws Exception {
        // nombre vacío, precio y stock negativos: fallan las validaciones de Producto
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","precio":-1,"stock":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errores.precio").value("El precio no puede ser negativo"))
                .andExpect(jsonPath("$.errores.stock").value("El stock no puede ser negativo"));

        // @Valid corta la petición antes de llegar al servicio
        verifyNoInteractions(service);
    }

    // ---------- PUT /api/productos/{id} ----------

    @Test
    void actualizar_valido_retorna200() throws Exception {
        // eq(1L) es necesario porque al usar un matcher (any) todos los argumentos deben serlo
        when(service.actualizar(eq(1L), any(Producto.class))).thenReturn(mouse);

        mockMvc.perform(put(URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse","precio":15990,"stock":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mouse"));
    }

    // ---------- DELETE /api/productos/{id} ----------

    @Test
    void eliminar_retorna204() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }

    @Test
    void eliminar_inexistente_retorna404() throws Exception {
        // para métodos void se usa doThrow(...).when(mock).metodo()
        doThrow(new RecursoNoEncontradoException("Producto 99 no encontrado"))
                .when(service).eliminar(99L);

        mockMvc.perform(delete(URL + "/99"))
                .andExpect(status().isNotFound());
    }
}
