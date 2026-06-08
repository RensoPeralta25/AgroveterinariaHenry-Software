package com.agroveterinaria.enums;

public enum EstadoRecepcion {
    PENDIENTE("Pendiente"),
    PARCIAL("Recepción Parcial"),
    RECIBIDA("Completada");

    private final String etiqueta;

    EstadoRecepcion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}