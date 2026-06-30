package com.agroveterinaria.enums;

public enum TipoEmbargo {
    PENSION_ALIMENTICIA("Pensión Alimenticia");

    private final String descripcion;

    TipoEmbargo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
