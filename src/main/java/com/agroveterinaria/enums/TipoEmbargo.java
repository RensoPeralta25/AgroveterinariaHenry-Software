package com.agroveterinaria.enums;

public enum TipoEmbargo {
    PENSION_ALIMENTICIA(1);

    private final int prioridad;

    TipoEmbargo(int prioridad) {
        this.prioridad = prioridad;
    }

    public int getPrioridad() {
        return prioridad;
    }
}
