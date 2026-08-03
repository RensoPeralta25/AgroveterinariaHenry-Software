package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoPrestamo;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "prestamo_empleado")
public class PrestamoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPrestamo;

    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @NotNull(message = "El monto de capital es obligatorio")
    @Positive(message = "El monto de capital debe ser mayor a cero")
    private BigDecimal montoCapital;

    @NotNull(message = "La tasa de interés es obligatoria")
    @DecimalMin(value = "0.00", message = "La tasa de interés no puede ser negativa")
    private BigDecimal tasaInteres;

    @NotNull(message = "El plazo en meses es obligatorio")
    @Min(value = 1, message = "El plazo mínimo es de 1 mes")
    private Integer plazoMeses;

    @NotNull(message = "La cuota periódica es obligatoria")
    @Positive(message = "La cuota a descontar debe ser mayor a cero")
    private BigDecimal cuotaPeriodica;

    @NotNull(message = "El balance pendiente es obligatorio")
    @PositiveOrZero(message = "El balance pendiente no puede ser negativo")
    private BigDecimal balanceCapitalPendiente;

    @NotNull(message = "La fecha de aprobación es obligatoria")
    private LocalDate fechaAprobacion;

    @NotBlank(message = "Debe especificar un concepto para el préstamo")
    @Size(max = 255, message = "El concepto no puede exceder los 255 caracteres")
    private String concepto;

    @NotNull(message = "El estado del préstamo es obligatorio")
    @Enumerated(EnumType.STRING)
    private EstadoPrestamo estado = EstadoPrestamo.PENDIENTE;

    @NotNull(message = "Las cuotas pagadas son obligatorias")
    @Min(value = 0, message = "El minimo de cuotas pagadas es cero")
    private Integer cuotasPagadas = 0;

    @PrePersist
    public void prePersist() {
        if (fechaAprobacion == null) fechaAprobacion = LocalDate.now();
        if (balanceCapitalPendiente == null) balanceCapitalPendiente = montoCapital;
    }
}
