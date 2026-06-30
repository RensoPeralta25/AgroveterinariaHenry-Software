package com.agroveterinaria.enums;

public enum PeriodoNomina {
    MES("Mensual"),
    QUINCENA("Quincenal");

    private final String descripcion;

    PeriodoNomina(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
