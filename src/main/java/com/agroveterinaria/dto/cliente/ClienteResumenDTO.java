package com.agroveterinaria.dto.cliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResumenDTO(
        Long idCliente,
        String nombre,
        String cedula,
        String telefono,
        String direccion,
        String tipoCliente,
        int cantidadMascotas,
        int citasPendientes,
        LocalDateTime proximaCita,
        int cantidadVentas,
        BigDecimal totalVendido,
        BigDecimal balancePendiente,
        BigDecimal totalNotasCredito
) {
}
