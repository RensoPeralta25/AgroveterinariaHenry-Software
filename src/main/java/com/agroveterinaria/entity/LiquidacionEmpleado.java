package com.agroveterinaria.entity;

import com.agroveterinaria.enums.MotivoSalida;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "liquidacion_empleado")
public class LiquidacionEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El empleado no puede ser nulo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @NotNull(message = "La fecha de liquidación no puede ser nula")
    private LocalDate fechaLiquidacion;

    @NotNull(message = "El motivo no puede ser nulo")
    @Enumerated(EnumType.STRING)
    private MotivoSalida motivoSalida;

    @NotNull(message = "El monto de la regalía es obligatorio")
    @PositiveOrZero(message = "El monto de la regalía no puede ser negativo")
    private BigDecimal montoRegalia = BigDecimal.ZERO;

    @NotNull(message = "El monto de vacaciones es obligatorio")
    @PositiveOrZero(message = "El monto de vacaciones no puede ser negativo")
    private BigDecimal montoVacaciones = BigDecimal.ZERO;

    @NotNull(message = "El monto del preaviso es obligatorio")
    @PositiveOrZero(message = "El monto del preaviso no puede ser negativo")
    private BigDecimal montoPreaviso = BigDecimal.ZERO;

    @NotNull(message = "El monto de la cesantía es obligatorio")
    @PositiveOrZero(message = "El monto de la cesantía no puede ser negativo")
    private BigDecimal montoCesantia = BigDecimal.ZERO;

    @NotNull(message = "El descuento de préstamos es obligatorio")
    @PositiveOrZero(message = "El descuento de préstamos no puede ser negativo")
    private BigDecimal descuentoPrestamos = BigDecimal.ZERO;

    @NotNull(message = "El descuento de anticipos es obligatorio")
    @PositiveOrZero(message = "El descuento de anticipos no puede ser negativo")
    private BigDecimal descuentoAnticipos = BigDecimal.ZERO;

    @NotNull(message = "El descuento de embargos es obligatorio")
    @PositiveOrZero(message = "El descuento de embargos no puede ser negativo")
    private BigDecimal descuentoEmbargos = BigDecimal.ZERO;

    @NotNull(message = "El total de ingresos es obligatorio")
    @PositiveOrZero(message = "El total de ingresos no puede ser negativo")
    private BigDecimal totalIngresos = BigDecimal.ZERO;

    @NotNull(message = "El total de deducciones es obligatorio")
    @PositiveOrZero(message = "El total de deducciones no puede ser negativo")
    private BigDecimal totalDeducciones = BigDecimal.ZERO;

    @NotNull(message = "El monto neto es obligatorio")
    @PositiveOrZero(message = "El monto neto no puede ser negativo")
    private BigDecimal montoNeto = BigDecimal.ZERO;

    @NotNull(message = "La fecha de registro es obligatoria")
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PositiveOrZero(message = "Los dias de preaviso trabajados no pueden ser negativos")
    private Integer diasPreavisoTrabajados = 0;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "id_gasto_neto")
    private GastoOperativo gastoNetoAsociado;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "id_gasto_embargo")
    private GastoOperativo gastoEmbargoAsociado;
}