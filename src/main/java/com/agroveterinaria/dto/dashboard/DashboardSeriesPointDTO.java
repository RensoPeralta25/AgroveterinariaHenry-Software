package com.agroveterinaria.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSeriesPointDTO(
        String etiqueta,
        BigDecimal valor
) {
}
