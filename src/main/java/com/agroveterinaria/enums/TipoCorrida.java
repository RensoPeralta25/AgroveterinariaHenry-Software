package com.agroveterinaria.enums;

public enum TipoCorrida {
    ORDINARIA("Nómina Ordinaria"),
    REGALIA_PASCUAL("Sueldo 13 / Regalía"),
    BONIFICACION("Bonificación Anual"),
    VACACIONES_ANTICIPADAS("Pago de Vacaciones Anticipado");

    private final String descripcion;

    TipoCorrida(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
