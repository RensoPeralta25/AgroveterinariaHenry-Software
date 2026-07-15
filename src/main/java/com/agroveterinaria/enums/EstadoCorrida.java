package com.agroveterinaria.enums;

public enum EstadoCorrida {
    PENDIENTE("Pendiente"),
    APROBADA("Aprobada");

    private final String descripcion;

    EstadoCorrida(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}