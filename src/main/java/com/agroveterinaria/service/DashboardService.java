package com.agroveterinaria.service;

import com.agroveterinaria.dto.dashboard.DashboardAlertDTO;
import com.agroveterinaria.dto.dashboard.DashboardCategoryDTO;
import com.agroveterinaria.dto.dashboard.DashboardDataDTO;
import com.agroveterinaria.dto.dashboard.DashboardMetricDTO;
import com.agroveterinaria.dto.dashboard.DashboardSeriesPointDTO;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.EstadoRecepcion;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.repository.CitaRepository;
import com.agroveterinaria.repository.CompraRepository;
import com.agroveterinaria.repository.DespachoRepository;
import com.agroveterinaria.repository.GastoOperativoRepository;
import com.agroveterinaria.repository.InventarioRepository;
import com.agroveterinaria.repository.LoteRepository;
import com.agroveterinaria.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int DIAS_SERIE = 14;
    private static final int DIAS_VENCIMIENTO = 30;
    private static final BigDecimal STOCK_MINIMO = BigDecimal.TEN;
    private static final DateTimeFormatter DIA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final CitaRepository citaRepository;
    private final InventarioRepository inventarioRepository;
    private final LoteRepository loteRepository;
    private final DespachoRepository despachoRepository;
    private final GastoOperativoRepository gastoOperativoRepository;

    public DashboardService(
            VentaRepository ventaRepository,
            CompraRepository compraRepository,
            CitaRepository citaRepository,
            InventarioRepository inventarioRepository,
            LoteRepository loteRepository,
            DespachoRepository despachoRepository,
            GastoOperativoRepository gastoOperativoRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.compraRepository = compraRepository;
        this.citaRepository = citaRepository;
        this.inventarioRepository = inventarioRepository;
        this.loteRepository = loteRepository;
        this.despachoRepository = despachoRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDataDTO obtenerResumenPrincipal() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDate inicioSerie = hoy.minusDays(DIAS_SERIE - 1);
        LocalDate hastaVencimiento = hoy.plusDays(DIAS_VENCIMIENTO);

        BigDecimal ventasHoy = ventaRepository.sumarMontoEntre(inicioHoy, inicioManana);
        BigDecimal ventasMes = ventaRepository.sumarMontoEntre(inicioMes, inicioManana);
        BigDecimal comprasMes = compraRepository.sumarTotalEntre(inicioMes, inicioManana);
        BigDecimal gastosOperativosMes = gastoOperativoRepository.sumarMontoEntre(
                inicioMes.toLocalDate(),
                inicioManana.toLocalDate()
        );
        BigDecimal resultadoOperativoMes = valorSeguro(ventasMes).subtract(valorSeguro(gastosOperativosMes));

        long ventasPendientes = ventaRepository.countByEstado(EstadoVenta.PENDIENTE);
        BigDecimal balancePendiente = calcularBalancePendiente();
        long citasPendientesHoy = citaRepository.countByFechaHoraBetweenAndRealizadoFalse(inicioHoy, inicioManana);
        long comprasPendientes = compraRepository.countByEstadoRecepcionIn(estadosCompraPendiente());
        long despachosPendientes = despachoRepository.countByFechaHoraEntregaIsNull();
        long productosStockBajo = inventarioRepository.contarProductosActivosConStockBajo(STOCK_MINIMO);
        long lotesPorVencer = loteRepository.countByFechaVencimientoBetween(hoy, hastaVencimiento);

        List<DashboardMetricDTO> metricas = List.of(
                new DashboardMetricDTO("ventasHoy", "Ventas de hoy", moneda(ventasHoy), "Ingresos registrados hoy", "positivo"),
                new DashboardMetricDTO("ventasMes", "Ventas del mes", moneda(ventasMes), "Acumulado desde inicio de mes", "positivo"),
                new DashboardMetricDTO("comprasMes", "Compras del mes", moneda(comprasMes), "Abastecimiento registrado", "neutral"),
                new DashboardMetricDTO(
                        "gastosOperativosMes",
                        "Gastos operativos del mes",
                        moneda(gastosOperativosMes),
                        "Egresos operativos registrados",
                        "riesgo"
                ),
                new DashboardMetricDTO(
                        "resultadoOperativoMes",
                        "Resultado operativo del mes",
                        moneda(resultadoOperativoMes),
                        "Ventas menos gastos operativos; no incluye costo de inventario",
                        resultadoOperativoMes.compareTo(BigDecimal.ZERO) >= 0 ? "positivo" : "riesgo"
                ),
                new DashboardMetricDTO("porCobrar", "Por cobrar", moneda(balancePendiente), ventasPendientes + " ventas pendientes", "alerta"),
                new DashboardMetricDTO("citasHoy", "Citas pendientes hoy", String.valueOf(citasPendientesHoy), "Servicios veterinarios por atender", "neutral"),
                new DashboardMetricDTO("stockBajo", "Productos con stock bajo", String.valueOf(productosStockBajo), "Umbral: " + STOCK_MINIMO.stripTrailingZeros().toPlainString(), "riesgo"),
                new DashboardMetricDTO("lotesVencen", "Lotes por vencer", String.valueOf(lotesPorVencer), "Proximos " + DIAS_VENCIMIENTO + " dias", "riesgo"),
                new DashboardMetricDTO("despachosPendientes", "Despachos pendientes", String.valueOf(despachosPendientes), "Sin fecha de entrega", "neutral")
        );

        return new DashboardDataDTO(
                metricas,
                construirSerieVentas(inicioSerie),
                construirSerieCompras(inicioSerie),
                construirSerieGastosOperativos(inicioSerie),
                construirInventarioPorCategoria(),
                construirAlertas(comprasPendientes, despachosPendientes, productosStockBajo, hoy, hastaVencimiento)
        );
    }

    private BigDecimal calcularBalancePendiente() {
        return ventaRepository.findConCobrosByEstado(EstadoVenta.PENDIENTE).stream()
                .map(this::calcularBalanceVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularBalanceVenta(Venta venta) {
        BigDecimal cobrado = venta.getCobros().stream()
                .map(cobro -> valorSeguro(cobro.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return valorSeguro(venta.getMontoTotal()).subtract(cobrado).max(BigDecimal.ZERO);
    }

    private List<DashboardSeriesPointDTO> construirSerieVentas(LocalDate inicioSerie) {
        Map<LocalDate, BigDecimal> acumulado = inicializarSerie(inicioSerie);
        ventaRepository.findByFechaHoraVentaGreaterThanEqualOrderByFechaHoraVentaAsc(inicioSerie.atStartOfDay())
                .forEach(venta -> acumular(acumulado, venta.getFechaHoraVenta().toLocalDate(), venta.getMontoTotal()));
        return convertirSerie(acumulado);
    }

    private List<DashboardSeriesPointDTO> construirSerieCompras(LocalDate inicioSerie) {
        Map<LocalDate, BigDecimal> acumulado = inicializarSerie(inicioSerie);
        compraRepository.findByFechaHoraCompraGreaterThanEqualOrderByFechaHoraCompraAsc(inicioSerie.atStartOfDay())
                .forEach(compra -> acumular(acumulado, compra.getFechaHoraCompra().toLocalDate(), compra.getTotal()));
        return convertirSerie(acumulado);
    }

    private List<DashboardSeriesPointDTO> construirSerieGastosOperativos(LocalDate inicioSerie) {
        Map<LocalDate, BigDecimal> acumulado = inicializarSerie(inicioSerie);
        gastoOperativoRepository.findByFechaGreaterThanEqualOrderByFechaAsc(inicioSerie)
                .forEach(gasto -> acumular(acumulado, gasto.getFecha(), gasto.getMonto()));
        return convertirSerie(acumulado);
    }

    private List<DashboardCategoryDTO> construirInventarioPorCategoria() {
        return inventarioRepository.sumarStockPorCategoria().stream()
                .map(row -> new DashboardCategoryDTO(etiquetaCategoria((CategoriaProducto) row[0]), valorSeguro((BigDecimal) row[1])))
                .toList();
    }

    private List<DashboardAlertDTO> construirAlertas(
            long comprasPendientes,
            long despachosPendientes,
            long productosStockBajo,
            LocalDate hoy,
            LocalDate hastaVencimiento
    ) {
        List<DashboardAlertDTO> alertas = new ArrayList<>();

        if (comprasPendientes > 0) {
            alertas.add(new DashboardAlertDTO(
                    "compras",
                    "Compras pendientes de recepcion",
                    comprasPendientes + " ordenes requieren seguimiento de entrada",
                    "media"
            ));
        }

        if (despachosPendientes > 0) {
            alertas.add(new DashboardAlertDTO(
                    "despachos",
                    "Despachos sin entrega confirmada",
                    despachosPendientes + " despachos siguen abiertos",
                    "media"
            ));
        }

        if (productosStockBajo > 0) {
            alertas.add(new DashboardAlertDTO(
                    "inventario",
                    "Stock bajo detectado",
                    productosStockBajo + " productos estan por debajo del minimo operativo",
                    "alta"
            ));
        }

        for (Lote lote : loteRepository.findTop5ByFechaVencimientoBetweenOrderByFechaVencimientoAsc(hoy, hastaVencimiento)) {
            String numeroLote = lote.getNumeroLote() == null || lote.getNumeroLote().isBlank()
                    ? "sin numero"
                    : lote.getNumeroLote();
            alertas.add(new DashboardAlertDTO(
                    "lotes",
                    "Lote proximo a vencer",
                    lote.getProducto().getNombre() + " - " + numeroLote + " vence el " + lote.getFechaVencimiento().format(FECHA_FORMATTER),
                    "alta"
            ));
        }

        return alertas;
    }

    private Map<LocalDate, BigDecimal> inicializarSerie(LocalDate inicioSerie) {
        Map<LocalDate, BigDecimal> serie = new LinkedHashMap<>();
        for (int i = 0; i < DIAS_SERIE; i++) {
            serie.put(inicioSerie.plusDays(i), BigDecimal.ZERO);
        }
        return serie;
    }

    private void acumular(Map<LocalDate, BigDecimal> serie, LocalDate fecha, BigDecimal monto) {
        if (serie.containsKey(fecha)) {
            serie.put(fecha, serie.get(fecha).add(valorSeguro(monto)));
        }
    }

    private List<DashboardSeriesPointDTO> convertirSerie(Map<LocalDate, BigDecimal> acumulado) {
        return acumulado.entrySet().stream()
                .map(entry -> new DashboardSeriesPointDTO(entry.getKey().format(DIA_FORMATTER), entry.getValue()))
                .toList();
    }

    private List<EstadoRecepcion> estadosCompraPendiente() {
        return List.of(EstadoRecepcion.PENDIENTE, EstadoRecepcion.PARCIAL);
    }

    private String etiquetaCategoria(CategoriaProducto categoria) {
        return categoria == null ? "Sin categoria" : categoria.getEtiqueta();
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String moneda(BigDecimal valor) {
        return "RD$ " + valorSeguro(valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
