package com.agroveterinaria.dto.nomina;

import com.agroveterinaria.entity.Empleado;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class NovedadNominaDTO {
    private Empleado empleado;

    private Integer horasExtras = 0;
    private BigDecimal comisionesRegulares = BigDecimal.ZERO;
    private BigDecimal comisionesExtraordinarias = BigDecimal.ZERO;
    private BigDecimal dietasViaticos = BigDecimal.ZERO;
    private Integer ausenciasNoPagadasDias = 0;

    public NovedadNominaDTO(Empleado empleado) {
        this.empleado = empleado;
    }

    public BigDecimal getTotalIngresosFijos() {
        return comisionesRegulares != null ? comisionesRegulares : BigDecimal.ZERO
                .add(comisionesExtraordinarias != null ? comisionesExtraordinarias : BigDecimal.ZERO)
                .add(dietasViaticos != null ? dietasViaticos : BigDecimal.ZERO);
    }
}
