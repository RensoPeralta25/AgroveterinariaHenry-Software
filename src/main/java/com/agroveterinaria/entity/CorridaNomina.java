package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.PeriodoNomina;
import com.agroveterinaria.enums.TipoCorrida;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "El período es obligatorio")
    @Enumerated(EnumType.STRING)
    private PeriodoNomina periodo;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCorrida estado;

    @OneToMany(mappedBy = "corrida", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Nomina> nominas;

    @NotNull(message = "El tipo de corrida es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoCorrida tipo = TipoCorrida.ORDINARIA;

    @ManyToOne
    @JoinColumn(name = "periodo_fiscal_id")
    private PeriodoFiscal periodoFiscal;

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
