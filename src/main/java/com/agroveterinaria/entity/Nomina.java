package com.agroveterinaria.entity;

import com.agroveterinaria.enums.PeriodoNomina;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    private PeriodoNomina periodo;

    private BigDecimal totalDevengado;
    private BigDecimal totalDeducciones;

    @OneToMany(mappedBy = "nomina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleNomina> detalles = new ArrayList<>();

    public Nomina(Empleado empleado, LocalDate fechaEmision, PeriodoNomina periodo) {
        this.empleado = empleado;
        this.fechaEmision = fechaEmision;
        this.periodo = periodo;
        this.totalDevengado = BigDecimal.ZERO;
        this.totalDeducciones = BigDecimal.ZERO;
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
