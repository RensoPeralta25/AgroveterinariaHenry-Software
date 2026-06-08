package com.agroveterinaria.dto.recepcion;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class GastoOperativoUI {
    private String notas;
    private BigDecimal monto = BigDecimal.ZERO;
}