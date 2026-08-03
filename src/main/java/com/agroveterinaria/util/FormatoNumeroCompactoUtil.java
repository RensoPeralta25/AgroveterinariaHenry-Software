package com.agroveterinaria.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class FormatoNumeroCompactoUtil {

    private static final BigDecimal MIL = new BigDecimal("1000");
    private static final BigDecimal MILLON = new BigDecimal("1000000");
    private static final BigDecimal MIL_MILLONES = new BigDecimal("1000000000");
    private static final BigDecimal BILLON = new BigDecimal("1000000000000");

    private static final BigDecimal[] ESCALAS = {
            BigDecimal.ONE,
            MIL,
            MILLON,
            MIL_MILLONES,
            BILLON
    };

    private static final String[] SUFIJOS = {"", " mil", " M", " mil M", " B"};

    private FormatoNumeroCompactoUtil() {
    }

    public static String formatear(BigDecimal valor) {
        BigDecimal valorSeguro = valor == null ? BigDecimal.ZERO : valor;
        int escala = seleccionarEscala(valorSeguro.abs());
        BigDecimal reducido = valorSeguro.divide(ESCALAS[escala], 2, RoundingMode.HALF_UP);

        if (escala < ESCALAS.length - 1 && reducido.abs().compareTo(MIL) >= 0) {
            escala++;
            reducido = valorSeguro.divide(ESCALAS[escala], 2, RoundingMode.HALF_UP);
        }

        DecimalFormat formato = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(reducido) + SUFIJOS[escala];
    }

    public static String formatear(long valor) {
        return formatear(BigDecimal.valueOf(valor));
    }

    public static String formatearMoneda(BigDecimal valor) {
        BigDecimal valorSeguro = valor == null ? BigDecimal.ZERO : valor;
        if (valorSeguro.abs().compareTo(MIL) < 0) {
            DecimalFormat formato = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
            formato.setRoundingMode(RoundingMode.HALF_UP);
            return "RD$ " + formato.format(valorSeguro);
        }
        return "RD$ " + formatear(valorSeguro);
    }

    private static int seleccionarEscala(BigDecimal valorAbsoluto) {
        for (int i = ESCALAS.length - 1; i > 0; i--) {
            if (valorAbsoluto.compareTo(ESCALAS[i]) >= 0) {
                return i;
            }
        }
        return 0;
    }
}
