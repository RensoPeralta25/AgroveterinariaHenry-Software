package com.agroveterinaria.enums;

public enum EstadoDevolucion {
    COMPLETADA("Completada"),
    ANULADA("Anulada");

    private final String etiqueta;

    EstadoDevolucion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}