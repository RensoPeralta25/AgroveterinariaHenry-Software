package com.agroveterinaria.enums;

public enum EstadoEmbargo {
    ACTIVO("Activo"),
    SUSPENDIDO("Suspendido"),
    INACTIVO("Inactivo"),
    CERRADO_POR_LIQUIDACION("Cerrado (Liq.)");

    private final String descripcion;

    EstadoEmbargo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}