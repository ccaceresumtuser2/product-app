package com.app.productos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de solo lectura mapeada a la vista {@code productos_ventas_view}
 * (ventas JOIN productos). La vista ya calcula {@code total = cantidad * precio}.
 * {@code @Immutable}: Hibernate nunca genera UPDATE sobre ella; tampoco hay setters.
 */
@Entity
@Immutable
@Table(name = "productos_ventas_view")
@Getter
@NoArgsConstructor  // requerido por JPA
@AllArgsConstructor
@Builder
public class ProductoVenta {

    /** Cada fila de la vista es una venta, así que su id sirve como clave. */
    @Id
    @Column(name = "id_venta")
    private Integer idVenta;

    /** Nombre del producto vendido. */
    private String nombre;

    private Integer cantidad;

    /** Precio unitario actual del producto. */
    private BigDecimal precio;

    /** cantidad * precio, calculado por la vista. */
    private BigDecimal total;

    /** Fecha y hora de la venta. */
    private LocalDateTime fecha;
}
