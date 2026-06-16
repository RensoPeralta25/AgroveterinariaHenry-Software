package com.agroveterinaria.enums;

public enum EstadoTransferencia {
    BORRADOR("Borrador"),
    PENDIENTE_DESPACHO("Pendiente de Despacho"),
    DESPACHADA_PARCIAL("Despachada Parcialmente"),
    EN_TRANSITO("En Tránsito (En ruta)"),
    RECIBIDA_PARCIAL("Recibida Parcial"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoTransferencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}