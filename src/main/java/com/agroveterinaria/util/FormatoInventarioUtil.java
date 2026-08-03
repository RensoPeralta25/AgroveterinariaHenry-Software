package com.agroveterinaria.util;

import com.agroveterinaria.entity.Producto;

import java.math.BigDecimal;

public class FormatoInventarioUtil {

    private FormatoInventarioUtil() {
        throw new IllegalStateException("Clase de utilidad");
    }

    public static String formatearCantidad(BigDecimal cantidad, BigDecimal factor, boolean permiteFraccionamiento, boolean esGranel) {
        return formatearCantidad(cantidad, factor, permiteFraccionamiento, esGranel, "Caja", "Unid");
    }

    public static String formatearCantidad(BigDecimal cantidad, BigDecimal factor, boolean permiteFraccionamiento, boolean esGranel, String unidadEmpaque, String unidadFraccion) {
        String empSingular = (unidadEmpaque != null && !unidadEmpaque.isBlank()) ? unidadEmpaque.trim() : "Caja";
        String fracSingular = (unidadFraccion != null && !unidadFraccion.isBlank()) ? unidadFraccion.trim() : "Unid";

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) == 0) return "0 " + pluralizar(empSingular, 2);

        if (esGranel) return cantidad.stripTrailingZeros().toPlainString() + " " + pluralizar(fracSingular, 2);

        if (!permiteFraccionamiento || factor == null || factor.compareTo(BigDecimal.ONE) <= 0) {
            return cantidad.intValue() + " " + pluralizar(empSingular, cantidad.intValue());
        }

        BigDecimal[] division = cantidad.divideAndRemainder(factor);
        int empaques = division[0].intValue();
        BigDecimal fracciones = division[1];

        StringBuilder resultado = new StringBuilder();
        if (empaques > 0) {
            resultado.append(empaques).append(" ").append(pluralizar(empSingular, empaques));
        }
        if (fracciones.compareTo(BigDecimal.ZERO) > 0) {
            if (!resultado.isEmpty()) resultado.append(", ");
            int cantFracInt = fracciones.stripTrailingZeros().scale() <= 0 ? fracciones.intValue() : 2;
            resultado.append(fracciones.stripTrailingZeros().toPlainString()).append(" ").append(pluralizar(fracSingular, cantFracInt));
        }

        return !resultado.isEmpty() ? resultado.toString() : "0 " + pluralizar(empSingular, 2);
    }

    public static String pluralizar(String palabra, int cantidad) {
        if (cantidad == 1) return palabra;

        String baja = palabra.toLowerCase();
        if (baja.endsWith("s") || baja.equals("cc") || baja.equals("kg") || baja.equals("ml") || baja.equals("oz") || baja.equals("lb") || baja.equals("gr")) {
            return palabra;
        }

        return palabra.matches(".*[aeiouáéíóúAEIOUÁÉÍÓÚ]$") ? palabra + "s" : palabra + "es";
    }

    public static String getNombreUnidadEmpaqueSafe(Producto prod) {
        return (prod.getUnidadEmpaque() != null ? prod.getUnidadEmpaque().getEtiqueta() : "Caja");
    }

    public static String getNombreUnidadFraccionSafe(Producto prod) {
        return (prod.getUnidadFraccion() != null ? prod.getUnidadFraccion().getEtiqueta() : "Unid");
    }
}