package com.agroveterinaria.enums;

public enum PeriodoNomina {
    MES("Mensual"),
    QUINCENA("Quincenal"),
    SEMANAL("Semanal");

    private final String descripcion;

    PeriodoNomina(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
