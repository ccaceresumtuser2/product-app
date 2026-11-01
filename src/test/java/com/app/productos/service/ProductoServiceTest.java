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
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    private Producto mouse;

    @BeforeEach
    void setUp() {
        mouse = Producto.builder()
                .id(1L).nombre("Mouse").descripcion("Inalámbrico")
                .precio(new BigDecimal("15990")).stock(10)
                .build();
    }

    @Test
    void listar_sinFiltro_usaFindAll() {
        when(repository.findAll()).thenReturn(List.of(mouse));

        List<Producto> resultado = service.listar(null);

        assertThat(resultado).containsExactly(mouse);
        verify(repository).findAll();
        verify(repository, never()).findByNombreContainingIgnoreCase(any());
    }

    @Test
    void listar_conFiltroEnBlanco_usaFindAll() {
        when(repository.findAll()).thenReturn(List.of(mouse));

        service.listar("   ");

        verify(repository).findAll();
    }

    @Test
    void listar_conFiltro_buscaPorNombre() {
        when(repository.findByNombreContainingIgnoreCase("mou")).thenReturn(List.of(mouse));

        List<Producto> resultado = service.listar("mou");

        assertThat(resultado).containsExactly(mouse);
        verify(repository, never()).findAll();
    }

    @Test
    void obtener_existente_retornaProducto() {
        when(repository.findById(1L)).thenReturn(Optional.of(mouse));

        assertThat(service.obtener(1L)).isSameAs(mouse);
    }

    @Test
    void obtener_inexistente_lanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Producto 99 no encontrado");
    }

    @Test
    void crear_ignoraIdRecibido_yGuarda() {
        Producto nuevo = Producto.builder()
                .id(50L).nombre("Teclado").precio(BigDecimal.TEN).stock(5)
                .build();
        when(repository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(nuevo);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getNombre()).isEqualTo("Teclado");
    }

    @Test
    void actualizar_existente_copiaDatosYGuarda() {
        Producto datos = Producto.builder()
                .nombre("Mouse Pro").descripcion("Gamer")
                .precio(new BigDecimal("29990")).stock(3)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(mouse));
        when(repository.save(mouse)).thenReturn(mouse);

        Producto resultado = service.actualizar(1L, datos);

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
