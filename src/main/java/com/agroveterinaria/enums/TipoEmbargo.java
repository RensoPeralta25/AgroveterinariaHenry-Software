package com.agroveterinaria.enums;

public enum TipoEmbargo {
    PENSION_ALIMENTICIA(1, "Pensión Alimenticia");

    private final int prioridad;
    private final String descripcion;

    TipoEmbargo(int prioridad, String descripcion) {
        this.prioridad = prioridad;
        this.descripcion = descripcion;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public String getDescripcion() {
        return descripcion;
    }
}