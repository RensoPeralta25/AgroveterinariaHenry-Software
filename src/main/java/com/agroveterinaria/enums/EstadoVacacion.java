package com.agroveterinaria.enums;

public enum EstadoVacacion {
    PENDIENTE("Pendiente de Aprobación"),
    APROBADA("Aprobada (No Pagada)"),
    PAGADA("Aprobada y Pagada");

    private final String descripcion;

    EstadoVacacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}