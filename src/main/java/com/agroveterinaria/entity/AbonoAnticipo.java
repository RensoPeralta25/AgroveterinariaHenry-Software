package com.agroveterinaria.entity;

import com.agroveterinaria.enums.MetodoPago;
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
@Table(name = "abono_anticipo")
public class AbonoAnticipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El anticipo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_anticipo")
    private AnticipoSalario anticipoSalario;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto abonado debe ser mayor a cero")
    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    @NotNull(message = "La fecha del abono es obligatoria")
    private LocalDate fechaAbono;

    @NotNull(message = "El método de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @PrePersist
    public void prePersist() {
        if (fechaAbono == null) fechaAbono = LocalDate.now();
    }
}