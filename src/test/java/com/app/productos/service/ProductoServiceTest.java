package com.app.productos.service;

import com.app.productos.exception.RecursoNoEncontradoException;
import com.app.productos.model.Producto;
import com.app.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario de la capa de servicio.
 * No levanta Spring: el repositorio es un mock de Mockito.
 *
 * Patrón de cada test:
 *   1. Preparar: con when(...).thenReturn(...) se define qué responde el mock.
 *   2. Ejecutar: se llama al método del servicio.
 *   3. Verificar: assertThat(...) revisa el resultado y verify(...) cómo se usó el mock.
 */
@ExtendWith(MockitoExtension.class) // habilita @Mock e @InjectMocks en JUnit 5
class ProductoServiceTest {

    /** Repositorio falso: no toca ninguna base de datos. */
    @Mock
    private ProductoRepository repository;

    /** Servicio real; Mockito le inyecta el mock por constructor. */
    @InjectMocks
    private ProductoService service;

    private Producto mouse;

    /** Se ejecuta antes de cada test: producto de ejemplo "ya guardado" (con id). */
    @BeforeEach
    void setUp() {
        mouse = Producto.builder()
                .id(1L).nombre("Mouse").descripcion("Inalámbrico")
                .precio(new BigDecimal("15990")).stock(10)
                .build();
    }

    // ---------- listar ----------

    @Test
    void listar_sinFiltro_usaFindAll() {
        when(repository.findAll()).thenReturn(List.of(mouse));

        List<Producto> resultado = service.listar(null);

        assertThat(resultado).containsExactly(mouse);
        verify(repository).findAll();
        // sin filtro no debe usarse la búsqueda por nombre
        verify(repository, never()).findByNombreContainingIgnoreCase(any());
    }

    @Test
    void listar_conFiltroEnBlanco_usaFindAll() {
        when(repository.findAll()).thenReturn(List.of(mouse));

        service.listar("   ");

        // un filtro con solo espacios se trata como "sin filtro"
        verify(repository).findAll();
    }

    @Test
    void listar_conFiltro_buscaPorNombre() {
        when(repository.findByNombreContainingIgnoreCase("mou")).thenReturn(List.of(mouse));

        List<Producto> resultado = service.listar("mou");

        assertThat(resultado).containsExactly(mouse);
        verify(repository, never()).findAll();
    }

    // ---------- obtener ----------

    @Test
    void obtener_existente_retornaProducto() {
        when(repository.findById(1L)).thenReturn(Optional.of(mouse));

        assertThat(service.obtener(1L)).isSameAs(mouse);
    }

    @Test
    void obtener_inexistente_lanzaExcepcion() {
        // Optional.empty() simula que el id no existe en la base
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Producto 99 no encontrado");
    }

    /**
     * TEST QUE FALLA A PROPÓSITO (demo).
     * El producto se llama "Mouse", pero se espera "Teclado".
     * Para que pase, cambiar "Teclado" por "Mouse" o borrar este test.
     */
    @Test
    void obtener_demoTestQueFalla() {
        when(repository.findById(1L)).thenReturn(Optional.of(mouse));

        Producto resultado = service.obtener(1L);

        assertThat(resultado.getNombre()).isEqualTo("Teclado");
    }

    // ---------- crear ----------

    @Test
    void crear_ignoraIdRecibido_yGuarda() {
        Producto nuevo = Producto.builder()
                .id(50L).nombre("Teclado").precio(BigDecimal.TEN).stock(5)
                .build();
        // el mock devuelve el mismo objeto que recibe
        when(repository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(nuevo);

        // ArgumentCaptor captura lo que se le pasó a save() para inspeccionarlo
        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull(); // el id 50 se descartó
        assertThat(captor.getValue().getNombre()).isEqualTo("Teclado");
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_existente_copiaDatosYGuarda() {
        Producto datos = Producto.builder()
                .nombre("Mouse Pro").descripcion("Gamer")
                .precio(new BigDecimal("29990")).stock(3)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(mouse));
        when(repository.save(mouse)).thenReturn(mouse);

        Producto resultado = service.actualizar(1L, datos);

        // conserva el id original y toma el resto de los campos nuevos
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Mouse Pro");
        assertThat(resultado.getDescripcion()).isEqualTo("Gamer");
        assertThat(resultado.getPrecio()).isEqualByComparingTo("29990");
        assertThat(resultado.getStock()).isEqualTo(3);
    }

    @Test
    void actualizar_inexistente_noGuarda() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, mouse))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_existente_borra() {
        when(repository.findById(1L)).thenReturn(Optional.of(mouse));

        service.eliminar(1L);

        verify(repository).delete(mouse);
    }

    @Test
    void eliminar_inexistente_noBorra() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).delete(any());
    }
}
