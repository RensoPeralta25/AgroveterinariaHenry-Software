package com.agroveterinaria.enums;

public enum TipoAjuste {
    ENTRADA("Entrada"),
    SALIDA("Salida");

    private final String etiqueta;
    TipoAjuste(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
