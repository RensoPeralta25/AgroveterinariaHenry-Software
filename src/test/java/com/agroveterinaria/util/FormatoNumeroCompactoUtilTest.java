package com.agroveterinaria.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatoNumeroCompactoUtilTest {

    @Test
    void abreviaMilesMillonesYCantidadesSuperiores() {
        assertEquals("999", FormatoNumeroCompactoUtil.formatear(new BigDecimal("999")));
        assertEquals("1.25 mil", FormatoNumeroCompactoUtil.formatear(new BigDecimal("1250")));
        assertEquals("1 M", FormatoNumeroCompactoUtil.formatear(new BigDecimal("1000000")));
        assertEquals("2.5 mil M", FormatoNumeroCompactoUtil.formatear(new BigDecimal("2500000000")));
        assertEquals("1 B", FormatoNumeroCompactoUtil.formatear(new BigDecimal("1000000000000")));
    }

    @Test
    void evitaMostrarMilMilesAlRedondear() {
        assertEquals("1 M", FormatoNumeroCompactoUtil.formatear(new BigDecimal("999999")));
    }

    @Test
    void conservaDosDecimalesEnMontosPequenosYAbreviaLosGrandes() {
        assertEquals("RD$ 200.00", FormatoNumeroCompactoUtil.formatearMoneda(new BigDecimal("200")));
        assertEquals("RD$ 1.5 mil", FormatoNumeroCompactoUtil.formatearMoneda(new BigDecimal("1500")));
        assertEquals("RD$ -2.75 M", FormatoNumeroCompactoUtil.formatearMoneda(new BigDecimal("-2750000")));
    }
}
