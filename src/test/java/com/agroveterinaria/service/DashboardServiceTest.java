package com.agroveterinaria.service;

import com.agroveterinaria.dto.dashboard.DashboardDataDTO;
import com.agroveterinaria.dto.dashboard.DashboardMetricDTO;
import com.agroveterinaria.entity.GastoOperativo;
import com.agroveterinaria.repository.CitaRepository;
import com.agroveterinaria.repository.CompraRepository;
import com.agroveterinaria.repository.DespachoRepository;
import com.agroveterinaria.repository.GastoOperativoRepository;
import com.agroveterinaria.repository.InventarioRepository;
import com.agroveterinaria.repository.LoteRepository;
import com.agroveterinaria.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private CompraRepository compraRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private InventarioRepository inventarioRepository;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private DespachoRepository despachoRepository;
    @Mock
    private GastoOperativoRepository gastoOperativoRepository;

    @Test
    void dashboardIncluyeGastosOperativosYResultadoDelMes() {
        DashboardService service = new DashboardService(
                ventaRepository,
                compraRepository,
                citaRepository,
                inventarioRepository,
                loteRepository,
                despachoRepository,
                gastoOperativoRepository
        );

        when(ventaRepository.sumarMontoEntre(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("100.00"), new BigDecimal("1000.00"));
        when(compraRepository.sumarTotalEntre(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(gastoOperativoRepository.sumarMontoEntre(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("200.00"));

        GastoOperativo gastoHoy = new GastoOperativo();
        gastoHoy.setFecha(LocalDate.now());
        gastoHoy.setMonto(new BigDecimal("75.00"));
        when(gastoOperativoRepository.findByFechaGreaterThanEqualOrderByFechaAsc(any(LocalDate.class)))
                .thenReturn(List.of(gastoHoy));

        DashboardDataDTO dashboard = service.obtenerResumenPrincipal();

        assertEquals("RD$ 200.00", metrica(dashboard, "gastosOperativosMes").valor());
        assertEquals("RD$ 800.00", metrica(dashboard, "resultadoOperativoMes").valor());
        assertEquals("positivo", metrica(dashboard, "resultadoOperativoMes").tono());
        assertEquals(14, dashboard.gastosOperativosUltimosDias().size());
        assertEquals(
                new BigDecimal("75.00"),
                dashboard.gastosOperativosUltimosDias().getLast().valor()
        );
    }

    private DashboardMetricDTO metrica(DashboardDataDTO dashboard, String clave) {
        return dashboard.metricas().stream()
                .filter(metrica -> clave.equals(metrica.clave()))
                .findFirst()
                .orElseThrow();
    }
}
