package com.agroveterinaria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleTransferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_transferencia")
    private Long idDetalleTransferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transferencia", nullable = false)
    private Transferencia transferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;
}
