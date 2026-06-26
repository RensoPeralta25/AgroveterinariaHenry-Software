package com.agroveterinaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "nomina")
public class Nomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNomina;

    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @NotNull(message = "La corrida de nómina es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_corrida")
    private CorridaNomina corrida;

    @NotNull(message = "El total devengado es obligatorio")
    @PositiveOrZero(message = "El total devengado no puede ser negativo")
    private BigDecimal totalDevengado;

    @NotNull(message = "El total de deducciones es obligatorio")
    @PositiveOrZero(message = "El total de deducciones no puede ser negativo")
    private BigDecimal totalDeducciones;

    @OneToMany(mappedBy = "nomina", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DetalleNomina> detalles;

    public Nomina(Empleado empleado, CorridaNomina corrida) {
        this.empleado = empleado;
        this.corrida = corrida;
        this.totalDevengado = BigDecimal.ZERO;
        this.totalDeducciones = BigDecimal.ZERO;
        this.detalles = new LinkedHashSet<>();
    }

    public BigDecimal calcularSueldoNeto() {
        this.totalDevengado = detalles.stream()
                .filter(d -> d.getTipo().esIngreso())
                .map(DetalleNomina::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalDeducciones = detalles.stream()
                .filter(d -> d.getTipo().esDeduccion())
                .map(DetalleNomina::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return this.totalDevengado.subtract(this.totalDeducciones);
    }
}
