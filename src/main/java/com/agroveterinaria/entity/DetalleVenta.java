package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstrategiaPrecioVenta;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    @ManyToOne
    @JoinColumn(name = "id_almacen")
    private Almacen almacen;

    @ManyToOne
    @JoinColumn(name = "id_lote")
    private Lote lote;

    @NotNull
    @Digits(integer = 10, fraction = 4, message = "La cantidad solo puede tener hasta 4 decimales")
    @Column(name = "cantidad", nullable = false, precision = 14, scale = 4)
    private BigDecimal cantidad;

    @NotNull
    @Digits(integer = 8, fraction = 6, message = "El precio unitario solo puede tener hasta 6 decimales")
    @Column(name = "precio_unitario_venta", nullable = false, precision = 14, scale = 6)
    private BigDecimal precioUnitarioVenta;

    @NotNull
    @Digits(integer = 12, fraction = 4, message = "El impuesto solo puede tener hasta 4 decimales")
    @Column(name = "impuesto", nullable = false, precision = 12, scale = 4)
    private BigDecimal impuesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estrategia_precio", length = 50)
    private EstrategiaPrecioVenta estrategiaPrecio;

    @Column(name = "precio_empaque_historico", precision = 14, scale = 6)
    private BigDecimal precioEmpaqueHistorico;

    @Column(name = "precio_fraccion_historico", precision = 14, scale = 6)
    private BigDecimal precioFraccionHistorico;

    public BigDecimal calcularSubtotal() {
        BigDecimal precio = precioUnitarioVenta != null ? precioUnitarioVenta : BigDecimal.ZERO;
        BigDecimal cantidadVendida = cantidad != null ? cantidad : BigDecimal.ZERO;
        BigDecimal impuestoAplicado = impuesto != null ? impuesto : BigDecimal.ZERO;

        return precio.multiply(cantidadVendida)
                .add(impuestoAplicado)
                .setScale(2, RoundingMode.HALF_UP);
    }
}