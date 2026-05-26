package com.agroveterinaria.enums;

public enum UnidadEmpaque {
    SACO("Saco"),
    CAJA("Caja"),
    FRASCO("Frasco"),
    BLISTER("Blíster"),
    UNIDAD_COMPLETA("Unidad Cerrada");

    private final String etiqueta;

    UnidadEmpaque(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}