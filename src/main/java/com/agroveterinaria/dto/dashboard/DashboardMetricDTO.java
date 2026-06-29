package com.agroveterinaria.dto.dashboard;

public record DashboardMetricDTO(
        String clave,
        String titulo,
        String valor,
        String detalle,
        String tono
) {
}
