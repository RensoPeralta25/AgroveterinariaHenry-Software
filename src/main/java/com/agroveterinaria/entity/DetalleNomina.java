package com.agroveterinaria.entity;


import com.agroveterinaria.enums.TipoConcepto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "detalle_nomina")
public class DetalleNomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalleNomina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nomina", nullable = false)
    private Nomina nomina;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoConcepto tipo;

    private BigDecimal monto;
    private BigDecimal cantidad;

}
