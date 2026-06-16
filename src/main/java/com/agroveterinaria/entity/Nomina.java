package com.agroveterinaria.entity;

import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_corrida")
    private CorridaNomina corrida;

    private BigDecimal totalDevengado;
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
