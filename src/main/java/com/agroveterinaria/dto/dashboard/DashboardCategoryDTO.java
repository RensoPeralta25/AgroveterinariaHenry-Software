package com.agroveterinaria.dto.dashboard;

import java.math.BigDecimal;

public record DashboardCategoryDTO(
        String categoria,
        BigDecimal valor
) {
}
