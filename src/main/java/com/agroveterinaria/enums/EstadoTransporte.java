package com.agroveterinaria.enums;

public enum EstadoTransporte {
    PROGRAMADO("Programado"),
    EN_TRANSITO("En tránsito"),
    COMPLETADO("Completado"),
    FALLIDO("Fallido"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoTransporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}