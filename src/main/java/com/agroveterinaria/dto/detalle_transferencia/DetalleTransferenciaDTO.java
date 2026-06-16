package com.agroveterinaria.dto.detalle_transferencia;

import com.agroveterinaria.entity.Lote;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DetalleTransferenciaDTO {
    private Lote lote;
    private BigDecimal cantidad;
    private BigDecimal existenciaMaxima;

    public DetalleTransferenciaDTO(Lote lote, BigDecimal existenciaMaxima) {
        this.lote = lote;
        this.existenciaMaxima = existenciaMaxima;
        this.cantidad = BigDecimal.ONE;
    }
}
