package com.agroveterinaria.enums;

public enum StatusEntidad {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String etiqueta;

    StatusEntidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
