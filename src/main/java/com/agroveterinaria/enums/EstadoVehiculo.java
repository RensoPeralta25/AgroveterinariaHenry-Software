package com.agroveterinaria.enums;

public enum EstadoVehiculo {
    DISPONIBLE("Disponible"),
    EN_TRANSITO("En Tránsito"),
    EN_MANTENIMIENTO("En Mantenimiento"),
    FUERA_DE_SERVICIO("Fuera de Servicio");

    private final String etiqueta;

    EstadoVehiculo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}