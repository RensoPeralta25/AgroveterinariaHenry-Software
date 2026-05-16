package com.agroveterinaria.enums;

public enum UnidadMedida {
    LIBRA("Libra (lb)"),
    QUINTAL("Quintal (q)"),
    MILIGRAMO("Miligramo (mg)"),
    GRAMO("Gramo (g)"),
    KILOGRAMO("Kilogramo (kg)"),
    CC("Centrímento cúbico (cc)"),
    LITRO("Litro (L)"),
    MILILITRO("Mililitro (ml)"),
    UNIDAD("Unidad (objeto)");

    private final String etiqueta;
    UnidadMedida(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
