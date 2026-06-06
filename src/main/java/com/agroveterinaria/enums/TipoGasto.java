package com.agroveterinaria.enums;

import lombok.Getter;

@Getter
public enum TipoGasto {
    FIJO("Fijo"),
    VARIALBE("Variable");

    private final String etiqueta;

    TipoGasto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

}
