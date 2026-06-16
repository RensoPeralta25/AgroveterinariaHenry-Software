package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.PeriodoNomina;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "corrida_nomina")
public class CorridaNomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCorrida;

    @Enumerated(EnumType.STRING)
    private PeriodoNomina periodo;

    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCorrida estado;

    @OneToMany(mappedBy = "corrida", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Nomina> nominas;

    public CorridaNomina(PeriodoNomina periodo, LocalDate fechaEmision) {
        this.periodo = periodo;
        this.fechaEmision = fechaEmision;
        this.estado = EstadoCorrida.PENDIENTE;
        this.nominas = new LinkedHashSet<>();
    }

    public BigDecimal getTotalGeneral() {
        return nominas.stream().map(n -> n.getTotalDevengado().subtract(n.getTotalDeducciones()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getCantidadEmpleados() {
        return nominas.size();
    }
}
