package com.agroveterinaria.dto.venta;

import java.math.BigDecimal;

public record LineaFacturaVentaPdfDTO(
        String productoNombre,
        String cantidad,
        BigDecimal precioUnitario,
        BigDecimal impuesto,
        BigDecimal subtotal
) {
}
