package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoVenta;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long idVenta;

    @NotNull
    @Column(name = "fecha_hora_venta", nullable = false)
    private LocalDateTime fechaHoraVenta;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detallesVentas = new ArrayList<>();

    @NotNull
    @Digits(integer = 12, fraction = 2, message = "El monto total solo puede tener hasta 2 decimales")
    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVenta estado;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Empleado vendedor;

    @Column(name = "comprobante_fiscal", length = 50)
    private String comprobanteFiscal;

    @NotNull
    @Column(name = "aplica_descuento_venta", nullable = false)
    private Boolean aplicaDescuentoVenta = false;

    @NotNull
    @Column(name = "lleva_despacho", nullable = false)
    private Boolean llevaDespacho = false;

    @Column(name = "fecha_vencimiento_pago")
    private LocalDateTime fechaVencimientoPago;
}
