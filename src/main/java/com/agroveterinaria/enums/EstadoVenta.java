package com.agroveterinaria.enums;

public enum EstadoVenta {
    NEGOCIACION("Negociacion"),
    PENDIENTE("Pendiente"),
    CERRADA("Cerrada");

    private final String etiqueta;

    EstadoVenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
