package com.agroveterinaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_dev_venta")
@Getter
@Setter
@NoArgsConstructor
public class DetalleDevVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_dev_venta")
    private Long idDetalleDevVenta;

    @NotNull(message = "La cantidad devuelta es obligatoria")
    @Digits(integer = 14, fraction = 4, message = "La cantidad debe corresponder a un formato decimal válido")
    @Column(name = "cantidad_devuelta", nullable = false)
    private BigDecimal cantidadDevuelta;

    @NotNull(message = "La cabecera de devolución es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_devolucion_venta", nullable = false)
    private DevolucionVenta devolucionVenta;

    @NotNull(message = "El ítem de la venta original es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_detalle_venta", nullable = false)
    private DetalleVenta detalleVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote")
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen_entrada")
    private Almacen almacenEntrada;
}