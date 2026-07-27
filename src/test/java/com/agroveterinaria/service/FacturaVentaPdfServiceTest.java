package com.agroveterinaria.service;

import com.agroveterinaria.dto.venta.FacturaVentaPdfDTO;
import com.agroveterinaria.dto.venta.LineaFacturaVentaPdfDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaVentaPdfServiceTest {

    private final FacturaVentaPdfService service = new FacturaVentaPdfService(null, null);

    @Test
    void facturaA4IncluyeDesgloseEnvioCondicionesYResponsables() {
        FacturaVentaPdfDTO factura = facturaPrueba();

        String html = service.crearHtml(factura);
        byte[] pdf = service.renderizar(factura);

        assertTrue(html.contains("2 Cajas x"));
        assertTrue(html.contains("Costo de envío"));
        assertTrue(html.contains("Crédito - vence el 15/08/2026"));
        assertTrue(html.contains("Despachado por"));
        assertTrue(html.contains("Vendedor de prueba"));
        assertTrue(html.contains("Recibido por"));
        assertTrue(html.contains("Cliente de prueba"));
        assertFalse(html.contains("class=\"signature\""));
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));
    }

    private FacturaVentaPdfDTO facturaPrueba() {
        return new FacturaVentaPdfDTO(
                125L,
                LocalDateTime.of(2026, 7, 27, 10, 30),
                "Cliente de prueba",
                "001-0000000-1",
                "809-555-0101",
                "Santiago, República Dominicana",
                "Vendedor de prueba",
                "B0100000125",
                "Pendiente",
                true,
                "Crédito - vence el 15/08/2026",
                new BigDecimal("200.00"),
                new BigDecimal("36.00"),
                new BigDecimal("15.00"),
                new BigDecimal("251.00"),
                new BigDecimal("100.00"),
                new BigDecimal("151.00"),
                List.of(new LineaFacturaVentaPdfDTO(
                        "Alimento premium",
                        "2 Cajas",
                        new BigDecimal("100.00"),
                        new BigDecimal("36.00"),
                        new BigDecimal("236.00"),
                        "2 Cajas x RD$100.00 (p. empaque)"
                )),
                BigDecimal.ZERO,
                new BigDecimal("15.00")
        );
    }
}
