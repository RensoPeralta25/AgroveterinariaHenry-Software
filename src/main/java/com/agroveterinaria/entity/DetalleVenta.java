package com.agroveterinaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_venta")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    private Long idDetalleVenta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotNull
    @Digits(integer = 12, fraction = 2, message = "La cantidad solo puede tener hasta 2 decimales")
    @Column(name = "cantidad", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @NotNull
    @Digits(integer = 12, fraction = 2, message = "El precio unitario solo puede tener hasta 2 decimales")
    @Column(name = "precio_unitario_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioVenta;

    @NotNull
    @Digits(integer = 12, fraction = 4, message = "El impuesto solo puede tener hasta 4 decimales")
    @Column(name = "impuesto", nullable = false, precision = 12, scale = 4)
    private BigDecimal impuesto;
}
