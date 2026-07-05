package com.agroveterinaria.util;

import java.math.BigDecimal;

public class FormatoInventarioUtil {

    private FormatoInventarioUtil() {
        throw new IllegalStateException("Clase de utilidad");
    }

    public static String formatearCantidad(BigDecimal cantidad, BigDecimal factor, boolean permiteFraccionamiento, boolean esGranel) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) == 0) return "0";

        if (esGranel) return cantidad.toString();
        if (!permiteFraccionamiento || factor == null || factor.compareTo(BigDecimal.ONE) <= 0) {
            return cantidad.intValue() + " Unids";
        }

        BigDecimal[] division = cantidad.divideAndRemainder(factor);
        int cajas = division[0].intValue();
        BigDecimal unidades = division[1];

        StringBuilder resultado = new StringBuilder();
        if (cajas > 0) {
            resultado.append(cajas).append(cajas == 1 ? " Caja" : " Cajas");
        }
        if (unidades.compareTo(BigDecimal.ZERO) > 0) {
            if (!resultado.isEmpty()) resultado.append(", ");
            resultado.append(unidades.stripTrailingZeros().toPlainString()).append(" Unids");
        }

        return !resultado.isEmpty() ? resultado.toString() : "0 Cajas";
    }
}