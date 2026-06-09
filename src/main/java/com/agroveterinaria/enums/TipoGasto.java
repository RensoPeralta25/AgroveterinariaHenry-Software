package com.agroveterinaria.enums;

import lombok.Getter;

@Getter
public enum TipoGasto {
    FIJO("Fijo"),
    VARIABLE("Variable");

    private final String etiqueta;

    TipoGasto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

}
