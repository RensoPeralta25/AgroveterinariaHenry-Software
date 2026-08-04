package com.agroveterinaria.service;

import com.agroveterinaria.dto.dashboard.DashboardDataDTO;
import com.agroveterinaria.dto.dashboard.DashboardMetricDTO;
import com.agroveterinaria.entity.DevolucionVenta;
import com.agroveterinaria.entity.GastoOperativo;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.EstadoDevolucion;
import com.agroveterinaria.enums.EstadoRecepcion;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.repository.*;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private CompraRepository compraRepository;
    @Mock
    private DevolucionVentaRepository devolucionVentaRepository;
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
    @Mock
    private AbonoPrestamoRepository abonoPrestamoRepository;
    @Mock
    private AbonoAnticipoRepository abonoAnticipoRepository;

    @Test
    void dashboardIncluyeGastosOperativosYResultadoDelMes() {
        DashboardService service = new DashboardService(
                ventaRepository,
                compraRepository,
                devolucionVentaRepository,
                citaRepository,
                inventarioRepository,
                loteRepository,
                despachoRepository,
                gastoOperativoRepository,
                abonoPrestamoRepository,
                abonoAnticipoRepository
        );

        when(ventaRepository.sumarMontoEntre(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyCollection()
        ))
                .thenReturn(new BigDecimal("100.00"), new BigDecimal("1000.00"));
        when(devolucionVentaRepository.sumarMontoEntre(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(EstadoDevolucion.class)
        )).thenReturn(BigDecimal.ZERO);
        when(compraRepository.sumarTotalEntre(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(EstadoRecepcion.class)
        ))
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

    @Test
    void dashboardCalculaVentasNetasYCuentaProductosComparablesPorCategoria() {
        DashboardService service = new DashboardService(
                ventaRepository,
                compraRepository,
                devolucionVentaRepository,
                citaRepository,
                inventarioRepository,
                loteRepository,
                despachoRepository,
                gastoOperativoRepository,
                abonoPrestamoRepository,
                abonoAnticipoRepository
        );

        when(ventaRepository.sumarMontoEntre(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyCollection()
        )).thenReturn(new BigDecimal("1500.00"));
        when(devolucionVentaRepository.sumarMontoEntre(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(EstadoDevolucion.class)
        )).thenReturn(new BigDecimal("250.00"));
        when(compraRepository.sumarTotalEntre(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(EstadoRecepcion.class)
        )).thenReturn(BigDecimal.ZERO);
        when(gastoOperativoRepository.sumarMontoEntre(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        Venta venta = new Venta();
        venta.setFechaHoraVenta(LocalDateTime.now());
        venta.setMontoTotal(new BigDecimal("1500.00"));
        venta.setEstado(EstadoVenta.CERRADA);
        when(ventaRepository.findByFechaHoraVentaGreaterThanEqualAndEstadoInOrderByFechaHoraVentaAsc(
                any(LocalDateTime.class),
                anyCollection()
        )).thenReturn(List.of(venta));

        DevolucionVenta devolucion = new DevolucionVenta();
        devolucion.setFechaHora(LocalDateTime.now());
        devolucion.setMontoTotal(new BigDecimal("250.00"));
        devolucion.setEstado(EstadoDevolucion.COMPLETADA);
        when(devolucionVentaRepository.findByFechaHoraGreaterThanEqualAndEstadoOrderByFechaHoraAsc(
                any(LocalDateTime.class),
                any(EstadoDevolucion.class)
        )).thenReturn(List.of(devolucion));
        when(inventarioRepository.contarProductosConStockPorCategoria())
                .thenReturn(List.<Object[]>of(new Object[]{CategoriaProducto.ALIMENTO, 3L}));

        DashboardDataDTO dashboard = service.obtenerResumenPrincipal();

        assertEquals("RD$ 1.25 mil", metrica(dashboard, "ventasHoy").valor());
        assertEquals(new BigDecimal("1250.00"), dashboard.ventasUltimosDias().getLast().valor());
        assertEquals("Alimento", dashboard.inventarioPorCategoria().getFirst().categoria());
        assertEquals(new BigDecimal("3"), dashboard.inventarioPorCategoria().getFirst().valor());
    }

    private DashboardMetricDTO metrica(DashboardDataDTO dashboard, String clave) {
        return dashboard.metricas().stream()
                .filter(metrica -> clave.equals(metrica.clave()))
                .findFirst()
                .orElseThrow();
    }
}
