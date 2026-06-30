package com.agroveterinaria.enums;

public enum EstadoPrestamo {
    ACTIVO("Activo"),
    SALDADO("Saldado"),
    CANCELADO("Cancelado");

    private final String descripcion;

    EstadoPrestamo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
