package com.agroveterinaria.dto.dashboard;

import java.util.List;

public record DashboardDataDTO(
        List<DashboardMetricDTO> metricas,
        List<DashboardSeriesPointDTO> ventasUltimosDias,
        List<DashboardSeriesPointDTO> comprasUltimosDias,
        List<DashboardSeriesPointDTO> gastosOperativosUltimosDias,
        List<DashboardCategoryDTO> inventarioPorCategoria,
        List<DashboardAlertDTO> alertas
) {
}
