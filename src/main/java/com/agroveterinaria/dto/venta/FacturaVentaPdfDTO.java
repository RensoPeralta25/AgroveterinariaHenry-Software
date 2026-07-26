package com.agroveterinaria.dto.venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaVentaPdfDTO(
        Long idVenta,
        LocalDateTime fechaHoraVenta,
        String clienteNombre,
        String clienteCedula,
        String clienteTelefono,
        String clienteDireccion,
        String vendedorNombre,
        String comprobanteFiscal,
        String estado,
        boolean llevaDespacho,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal ajustes,
        BigDecimal montoTotal,
        BigDecimal montoCobrado,
        BigDecimal balancePendiente,
        List<LineaFacturaVentaPdfDTO> lineas,
        BigDecimal descuento,
        BigDecimal costoEnvio
) {
}
