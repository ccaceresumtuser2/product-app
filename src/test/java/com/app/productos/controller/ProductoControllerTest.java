package com.app.productos.controller;

import com.app.productos.exception.ConflictoException;
import com.app.productos.exception.RecursoNoEncontradoException;
import com.app.productos.model.Producto;
import com.app.productos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de la capa web (controller + GlobalExceptionHandler + validaciones).
 * Levanta solo la capa MVC; el servicio es un mock de Mockito (@MockitoBean).
 *
 * Se verifica lo que le importa al cliente HTTP: código de estado y JSON de respuesta.
 * Éxito:  {"status": "success", "data": ..., "message": ...}
 * Error:  {"status": "error", "code": ..., "message": ..., "errors": [...]}
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
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].nombre").value("Mouse"))
                .andExpect(jsonPath("$.message").doesNotExist()); // message: null
    }

    @Test
    void listar_conFiltro_pasaNombreAlServicio() throws Exception {
        when(service.listar("mou")).thenReturn(List.of(mouse));

        mockMvc.perform(get(URL).param("nombre", "mou"))
                .andExpect(status().isOk());

        // el query param ?nombre= llega tal cual al servicio
        verify(service).listar("mou");
    }

    // ---------- GET /api/productos/precio ----------

    @Test
    void listarPorRangoPrecio_retorna200ConLista() throws Exception {
        when(service.listarPorRangoPrecio(new BigDecimal("10000"), new BigDecimal("20000")))
                .thenReturn(List.of(mouse));

        mockMvc.perform(get(URL + "/precio").param("min", "10000").param("max", "20000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].nombre").value("Mouse"));
    }

    @Test
    void listarPorRangoPrecio_minMayorQueMax_retorna400() throws Exception {
        // el servicio rechaza el rango; GlobalExceptionHandler lo convierte en 400
        when(service.listarPorRangoPrecio(any(), any()))
                .thenThrow(new IllegalArgumentException("El precio mínimo no puede ser mayor que el máximo"));

        mockMvc.perform(get(URL + "/precio").param("min", "500").param("max", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("El precio mínimo no puede ser mayor que el máximo"))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    void listarPorRangoPrecio_sinParametros_retorna400_yNoLlamaAlServicio() throws Exception {
        mockMvc.perform(get(URL + "/precio").param("min", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Falta el parámetro 'max'"));

        verifyNoInteractions(service);
    }

    @Test
    void listarPorRangoPrecio_valorNoNumerico_retorna400() throws Exception {
        mockMvc.perform(get(URL + "/precio").param("min", "abc").param("max", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para el parámetro 'min'"));

        verifyNoInteractions(service);
    }

    // ---------- GET /api/productos/fecha ----------

    @Test
    void listarPorFechaCreacion_retorna200ConCreadoEn() throws Exception {
        Producto conFecha = Producto.builder()
                .id(1L).nombre("Mouse").precio(new BigDecimal("15990")).stock(10)
                .creadoEn(LocalDateTime.of(2026, 10, 24, 9, 55, 44))
                .build();
        when(service.listarPorFechaCreacion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(conFecha));

        mockMvc.perform(get(URL + "/fecha").param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                // LocalDateTime se serializa en formato ISO
                .andExpect(jsonPath("$.data[0].creadoEn").value("2026-10-24T09:55:44"));
    }

    @Test
    void listarPorFechaCreacion_desdePosteriorAHasta_retorna400() throws Exception {
        when(service.listarPorFechaCreacion(any(), any()))
                .thenThrow(new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'"));

        mockMvc.perform(get(URL + "/fecha").param("desde", "2026-11-01").param("hasta", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("La fecha 'desde' no puede ser posterior a 'hasta'"));
    }

    @Test
    void listarPorFechaCreacion_fechaInvalida_retorna400_yNoLlamaAlServicio() throws Exception {
        // formato distinto de yyyy-MM-dd
        mockMvc.perform(get(URL + "/fecha").param("desde", "01/10/2026").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para el parámetro 'desde'"));

        verifyNoInteractions(service);
    }

    @Test
    void listarPorFechaCreacion_sinHasta_retorna400() throws Exception {
        mockMvc.perform(get(URL + "/fecha").param("desde", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro 'hasta'"));

        verifyNoInteractions(service);
    }

    @Test
    void crear_ignoraCreadoEnEnviadoPorElCliente() throws Exception {
        when(service.crear(any(Producto.class))).thenReturn(mouse);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse","precio":15990,"stock":10,"creadoEn":"2000-01-01T00:00:00"}
                                """))
                .andExpect(status().isCreated());

        // READ_ONLY: Jackson descarta creadoEn del cuerpo, el servicio lo recibe en null
        verify(service).crear(argThat(p -> p.getCreadoEn() == null));
    }

    // ---------- GET /api/productos/{id} ----------

    @Test
    void obtener_existente_retorna200() throws Exception {
        when(service.obtener(1L)).thenReturn(mouse);

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.precio").value(15990));
    }

    @Test
    void obtener_inexistente_retorna404ConFormatoError() throws Exception {
        // el mock lanza la excepción; GlobalExceptionHandler debe convertirla en 404
        when(service.obtener(99L)).thenThrow(new RecursoNoEncontradoException("Producto 99 no encontrado"));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Producto 99 no encontrado"))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    void obtener_sinConexionABaseDeDatos_retorna503() throws Exception {
        // es lo que lanza Spring cuando vence el connection-timeout de Hikari
        when(service.obtener(1L)).thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager"));

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("Base de datos no disponible, intente más tarde"))
                .andExpect(jsonPath("$.errors", hasSize(0)));
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
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.message").value("Producto creado"));
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
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                // el filtro [?(...)] busca el elemento de la lista con ese campo
                .andExpect(jsonPath("$.errors[?(@.campo == 'nombre')].mensaje").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errors[?(@.campo == 'precio')].mensaje").value("El precio no puede ser negativo"))
                .andExpect(jsonPath("$.errors[?(@.campo == 'stock')].mensaje").value("El stock no puede ser negativo"));

        // @Valid corta la petición antes de llegar al servicio
        verifyNoInteractions(service);
    }

    @Test
    void crear_jsonMalFormado_retorna400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la petición no es un JSON válido"));

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
                .andExpect(jsonPath("$.data.nombre").value("Mouse"))
                .andExpect(jsonPath("$.message").value("Producto actualizado"));
    }

    // ---------- PUT /api/productos?nombre= ----------

    @Test
    void actualizarPorNombre_valido_retorna200() throws Exception {
        when(service.actualizarPorNombre(eq("Monitor 24\""), any(Producto.class))).thenReturn(mouse);

        // nombre con espacio y comillas: MockMvc lo codifica en la URL como haría el cliente
        mockMvc.perform(put(URL).param("nombre", "Monitor 24\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse","precio":15990,"stock":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Producto actualizado"));

        verify(service).actualizarPorNombre(eq("Monitor 24\""), any(Producto.class));
    }

    @Test
    void actualizarPorNombre_inexistente_retorna404() throws Exception {
        when(service.actualizarPorNombre(eq("Silla"), any(Producto.class)))
                .thenThrow(new RecursoNoEncontradoException("Producto 'Silla' no encontrado"));

        mockMvc.perform(put(URL).param("nombre", "Silla")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Silla","precio":1,"stock":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto 'Silla' no encontrado"));
    }

    @Test
    void actualizarPorNombre_nombreRepetido_retorna409() throws Exception {
        when(service.actualizarPorNombre(eq("Mouse"), any(Producto.class)))
                .thenThrow(new ConflictoException("Hay 2 productos llamados 'Mouse'; use PUT /api/productos/{id}"));

        mockMvc.perform(put(URL).param("nombre", "Mouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse","precio":1,"stock":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Hay 2 productos llamados 'Mouse'; use PUT /api/productos/{id}"));
    }

    @Test
    void actualizarPorNombre_cuerpoInvalido_retorna400_yNoLlamaAlServicio() throws Exception {
        mockMvc.perform(put(URL).param("nombre", "Mouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","precio":-1,"stock":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Datos inválidos"));

        verifyNoInteractions(service);
    }

    // ---------- DELETE /api/productos/{id} ----------

    @Test
    void eliminar_retorna200ConMensaje() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist()) // data: null
                .andExpect(jsonPath("$.message").value("Producto eliminado"));

        verify(service).eliminar(1L);
    }

    @Test
    void eliminar_inexistente_retorna404() throws Exception {
        // para métodos void se usa doThrow(...).when(mock).metodo()
        doThrow(new RecursoNoEncontradoException("Producto 99 no encontrado"))
                .when(service).eliminar(99L);

        mockMvc.perform(delete(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
