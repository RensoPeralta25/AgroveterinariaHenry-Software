package com.agroveterinaria.entity;

import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadEmpaque;
import com.agroveterinaria.enums.UnidadMedida;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @NotNull(message = "La categoría es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private CategoriaProducto categoria;

    @NotNull(message = "La unidad de empaque es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_empaque", nullable = false)
    private UnidadEmpaque unidadEmpaque;

    @NotNull(message = "El precio por empaque es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El precio de empaque solo puede tener hasta 2 decimales")
    @Column(name = "precio_empaque", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioEmpaque;

    @NotNull(message = "Debe especificar si el producto permite venta fraccionada")
    @Column(name = "permite_fraccionamiento", nullable = false)
    private Boolean permiteFraccionamiento = false;

    @Digits(integer = 10, fraction = 2, message = "El contenido solo puede tener hasta 2 decimales")
    @Column(name = "contenido_por_empaque", precision = 10, scale = 2)
    private BigDecimal contenidoPorEmpaque;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_fraccion")
    private UnidadMedida unidadFraccion;

    @Digits(integer = 10, fraction = 2, message = "El precio por fracción solo puede tener hasta 2 decimales")
    @Column(name = "precio_fraccion", precision = 10, scale = 2)
    private BigDecimal precioFraccion;

    @JdbcTypeCode(Types.BINARY)
    @Column(name = "foto")
    private byte[] foto;
}