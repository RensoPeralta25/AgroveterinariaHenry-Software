package com.agroveterinaria.enums;

public enum UnidadMedida {
    LIBRA("Libra"),
    QUINTAL("Quintal"),
    MILIGRAMO("Miligramo"),
    GRAMO("Gramo"),
    KILOGRAMO("Kilogramo"),
    CC("cc"),
    LITRO("Litro"),
    MILILITRO("ml"),
    UNIDAD("Unidad");

    private final String etiqueta;
    UnidadMedida(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
