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
    private BigDecimal reembolsoLicencias;

    public NovedadNominaDTO(Empleado empleado) {
        this.empleado = empleado;
    }

    public BigDecimal getTotalIngresosExtra() {
        BigDecimal total = BigDecimal.ZERO;

        if (comisionesRegulares != null) {
            total = total.add(comisionesRegulares);
        }
        if (comisionesExtraordinarias != null) {
            total = total.add(comisionesExtraordinarias);
        }
        if (dietasViaticos != null) {
            total = total.add(dietasViaticos);
        }
        if (reembolsoLicencias != null) {
            total = total.add(reembolsoLicencias);
        }

        return total;
    }
}
