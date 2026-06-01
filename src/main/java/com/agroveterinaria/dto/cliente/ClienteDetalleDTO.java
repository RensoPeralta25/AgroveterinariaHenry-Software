package com.agroveterinaria.dto.cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ClienteDetalleDTO(
        Long idCliente,
        String nombre,
        String cedula,
        String telefono,
        String direccion,
        String tipoCliente,
        Double longitud,
        Double latitud,
        int cantidadMascotas,
        int citasPendientes,
        BigDecimal totalVendido,
        BigDecimal balancePendiente,
        BigDecimal totalNotasCredito,
        List<MascotaResumenDTO> mascotas,
        List<CitaResumenDTO> citas,
        List<VentaResumenDTO> ventas,
        List<CobroResumenDTO> cobros,
        List<NotaCreditoResumenDTO> notasCredito
) {
    public record MascotaResumenDTO(
            String nombre,
            String tipoAnimal,
            String raza,
            String sexo,
            LocalDate fechaNacimiento
    ) {
    }

    public record CitaResumenDTO(
            LocalDateTime fechaHora,
            String mascota,
            String veterinario,
            String servicio,
            Boolean realizado
    ) {
    }

    public record VentaResumenDTO(
            LocalDateTime fechaHoraVenta,
            String vendedor,
            String estado,
            BigDecimal montoTotal
    ) {
    }

    public record CobroResumenDTO(
            Long idCobro,
            BigDecimal montoTotal,
            String metodoPago
    ) {
    }

    public record NotaCreditoResumenDTO(
            Long idNotaCredito,
            BigDecimal monto
    ) {
    }
}
