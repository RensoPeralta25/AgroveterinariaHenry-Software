package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoAnticipo;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "anticipos_salario")
public class AnticipoSalario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El empleado es obligatorio.")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @NotNull(message = "La fecha de registro es obligatoria.")
    private LocalDate fechaRegistro;

    @NotNull(message = "El monto es obligatorio.")
    @DecimalMin(value = "0.01")
    @Column(name = "monto_original", precision = 10, scale = 2)
    private BigDecimal montoOriginal;

    @NotNull(message = "La cuota es obligatoria.")
    @DecimalMin(value = "0.01")
    @Column(name = "cuota_descuento", precision = 10, scale = 2)
    private BigDecimal cuotaDescuento;

    @Column(name = "monto_descontado", precision = 10, scale = 2)
    private BigDecimal montoDescontado = BigDecimal.ZERO;

    @Column(name = "saldo_pendiente", precision = 10, scale = 2)
    private BigDecimal saldoPendiente;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    private EstadoAnticipo estado = EstadoAnticipo.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gasto")
    private GastoOperativo gastoAsociado;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) fechaRegistro = LocalDate.now();
        if (saldoPendiente == null) saldoPendiente = montoOriginal;
        if (montoDescontado == null) montoDescontado = BigDecimal.ZERO;
    }
}
