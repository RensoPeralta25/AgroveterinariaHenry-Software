package com.agroveterinaria.enums;

public enum CategoriaProducto {
    ALIMENTO("Alimento"),
    MEDICAMENTO("Medicamento"),
    MATERIA_PRIMA("Materia prima"),
    PRODUCTOS_VARIOS("Productos varios"),
    SERVICIO("Servicio veterinario");

    private final String etiqueta;
    CategoriaProducto(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
