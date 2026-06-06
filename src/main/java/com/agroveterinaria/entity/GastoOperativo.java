package com.agroveterinaria.entity;

import com.agroveterinaria.enums.TipoGasto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gasto_operativo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GastoOperativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Long idGasto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gasto", nullable = false, length = 20)
    private TipoGasto tipoGasto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "comprobante_fiscal", length = 50)
    private String comprobanteFiscal;

    @Column(name = "notas", length = 255)
    private String notas;
}