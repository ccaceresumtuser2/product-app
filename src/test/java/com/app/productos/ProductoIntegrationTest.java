package com.app.productos;

import com.app.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración: levanta la aplicación completa (controller -> service -> repository -> H2)
 * sin mocks y recorre el CRUD por HTTP.
 */
@SpringBootTest          // contexto completo de Spring, igual que en producción
@AutoConfigureMockMvc    // permite usar MockMvc contra ese contexto
@ActiveProfiles("test")  // base H2 en memoria en lugar de PostgreSQL
class ProductoIntegrationTest {

    private static final String URL = "/api/productos";

    @Autowired
    private MockMvc mockMvc;

    /** Se usa para revisar directamente qué quedó guardado en la base. */
    @Autowired
    private ProductoRepository repository;

    /** A diferencia de @DataJpaTest, aquí no hay rollback automático: se limpia a mano. */
    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void crudCompleto() throws Exception {
        // Crear
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse","descripcion":"Inalámbrico","precio":15990,"stock":10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        // id generado por la base, leído directamente
        Long id = repository.findAll().getFirst().getId();

        // Obtener
        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mouse"));

        // Listar con filtro
        mockMvc.perform(get(URL).param("nombre", "mou"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Actualizar
        mockMvc.perform(put(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse Pro","precio":29990,"stock":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mouse Pro"));

        // el cambio quedó persistido en la base
        assertThat(repository.findById(id)).get()
                .satisfies(p -> assertThat(p.getStock()).isEqualTo(3));

        // Eliminar
        mockMvc.perform(delete(URL + "/" + id))
                .andExpect(status().isNoContent());

        // ya no existe ni por HTTP ni en la base
        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void crear_invalido_noGuardaNada() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","precio":10,"stock":1}
                                """))
                .andExpect(status().isBadRequest());

        // la validación falló antes de llegar a la base
        assertThat(repository.count()).isZero();
    }
}
