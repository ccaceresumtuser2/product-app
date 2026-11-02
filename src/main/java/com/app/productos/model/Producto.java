package com.app.productos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA mapeada a la tabla {@code productos}.
 * Las anotaciones de {@code jakarta.validation} se evalúan en el controller con {@code @Valid};
 * las de {@code jakarta.persistence} describen las columnas.
 * Lombok genera getters, setters, constructores y el builder.
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor  // requerido por JPA
@AllArgsConstructor
@Builder
public class Producto {

    /** Clave primaria generada por la base (BIGSERIAL / IDENTITY). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Opcional. */
    @Size(max = 255)
    private String descripcion;

    /** BigDecimal evita errores de redondeo con montos. */
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Column(nullable = false)
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock;

    /**
     * Fecha y hora de creación. La asigna Hibernate al insertar ({@code @CreationTimestamp})
     * y nunca se modifica ({@code updatable = false}).
     * READ_ONLY: sale en el JSON de respuesta pero se ignora si el cliente la envía.
     */
    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime creadoEn;
}
