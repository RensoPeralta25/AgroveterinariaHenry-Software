package com.agroveterinaria.enums;

public enum EstadoRecepcion {
    BORRADOR("Borrador"),
    PENDIENTE("Pendiente de Recibir"),
    PARCIAL("Recepción Parcial"),
    RECIBIDA("Recibida Completamente");

    private final String etiqueta;

    EstadoRecepcion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}