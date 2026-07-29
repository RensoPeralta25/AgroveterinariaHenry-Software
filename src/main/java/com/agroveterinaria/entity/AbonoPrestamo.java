package com.agroveterinaria.entity;

import com.agroveterinaria.enums.MetodoPago;
import com.agroveterinaria.enums.TipoRecalculoPrestamo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "abono_prestamo")
public class AbonoPrestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El préstamo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prestamo")
    private PrestamoEmpleado prestamo;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto abonado debe ser mayor a cero")
    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    @NotNull(message = "La fecha del abono es obligatoria")
    private LocalDate fechaAbono;

    @NotNull(message = "El método de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @NotNull(message = "Debe especificar cómo recalcular el préstamo")
    @Enumerated(EnumType.STRING)
    private TipoRecalculoPrestamo tipoRecalculo;

    @PrePersist
    public void prePersist() {
        if (fechaAbono == null) fechaAbono = LocalDate.now();
    }
}