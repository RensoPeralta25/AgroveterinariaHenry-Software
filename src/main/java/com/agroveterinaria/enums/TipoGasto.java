package com.agroveterinaria.enums;

import lombok.Getter;

@Getter
public enum TipoGasto {
    FIJO("Fijo"),
    VARIABLE("Variable"),
    NOMINA("Nómina"),
    PRESTAMO_EMPLEADO("Préstamo a Empleado"),
    ANTICIPO_SALARIO("Anticipo de Salario");

    private final String etiqueta;

    TipoGasto(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
