package com.agroveterinaria.dto.dashboard;

public record DashboardAlertDTO(
        String tipo,
        String titulo,
        String detalle,
        String severidad
) {
}
