package com.agroveterinaria.enums;

public enum RolEmpleado {
    ADMINISTRADOR("Administrador"),
    CAJERO("Cajero"),
    VETERINARIO("Veterinario"),
    ASISTENTE("Asistente"),
    CONDUCTOR("Conductor"),
    RECURSOS_HUMANOS("Recursos Humanos"),;

    private final String descripcion;

    RolEmpleado(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}