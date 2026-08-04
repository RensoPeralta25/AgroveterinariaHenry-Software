package com.agroveterinaria.service;

import com.agroveterinaria.dto.dashboard.DashboardAlertDTO;
import com.agroveterinaria.dto.dashboard.DashboardCategoryDTO;
import com.agroveterinaria.dto.dashboard.DashboardDataDTO;
import com.agroveterinaria.dto.dashboard.DashboardMetricDTO;
import com.agroveterinaria.dto.dashboard.DashboardSeriesPointDTO;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.EstadoDevolucion;
import com.agroveterinaria.enums.EstadoRecepcion;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.repository.*;
import com.agroveterinaria.util.FormatoNumeroCompactoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private static final List<EstadoVenta> ESTADOS_VENTA_CONTABILIZADOS = List.of(
            EstadoVenta.PENDIENTE,
            EstadoVenta.CERRADA
    );

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final DevolucionVentaRepository devolucionVentaRepository;
    private final CitaRepository citaRepository;
    private final InventarioRepository inventarioRepository;
    private final LoteRepository loteRepository;
    private final DespachoRepository despachoRepository;
    private final GastoOperativoRepository gastoOperativoRepository;
    private final AbonoPrestamoRepository abonoPrestamoRepository;
    private final AbonoAnticipoRepository abonoAnticipoRepository;

    public DashboardService(
            VentaRepository ventaRepository,
            CompraRepository compraRepository,
            DevolucionVentaRepository devolucionVentaRepository,
            CitaRepository citaRepository,
            InventarioRepository inventarioRepository,
            LoteRepository loteRepository,
            DespachoRepository despachoRepository,
            GastoOperativoRepository gastoOperativoRepository, AbonoPrestamoRepository abonoPrestamoRepository, AbonoAnticipoRepository abonoAnticipoRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.compraRepository = compraRepository;
        this.devolucionVentaRepository = devolucionVentaRepository;
        this.citaRepository = citaRepository;
        this.inventarioRepository = inventarioRepository;
        this.loteRepository = loteRepository;
        this.despachoRepository = despachoRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
        this.abonoPrestamoRepository = abonoPrestamoRepository;
        this.abonoAnticipoRepository = abonoAnticipoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDataDTO obtenerResumenPrincipal() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDate inicioSerie = hoy.minusDays(DIAS_SERIE - 1);
        LocalDate hastaVencimiento = hoy.plusDays(DIAS_VENCIMIENTO);

        BigDecimal ventasHoy = calcularVentasNetas(inicioHoy, inicioManana);
        BigDecimal ventasMes = calcularVentasNetas(inicioMes, inicioManana);

        BigDecimal recuperacionMes = calcularRecuperacionDirecta(inicioMes, inicioManana);

        BigDecimal comprasMes = compraRepository.sumarTotalEntre(
                inicioMes,
                inicioManana,
                EstadoRecepcion.BORRADOR
        );
        BigDecimal gastosYDesembolsosMes = gastoOperativoRepository.sumarMontoEntre(
                inicioMes.toLocalDate(),
                inicioManana.toLocalDate()
        );

        BigDecimal entradasTotalesMes = ventasMes.add(recuperacionMes);
        BigDecimal flujoCajaNetoMes = valorSeguro(entradasTotalesMes).subtract(valorSeguro(gastosYDesembolsosMes));

        long ventasPendientes = ventaRepository.countByEstado(EstadoVenta.PENDIENTE);
        BigDecimal balancePendiente = calcularBalancePendiente();
        long citasPendientesHoy = citaRepository.countByFechaHoraBetweenAndRealizadoFalse(inicioHoy, inicioManana);
        long comprasPendientes = compraRepository.countByEstadoRecepcionIn(estadosCompraPendiente());
        long despachosPendientes = despachoRepository.countByFechaHoraEntregaIsNull();
        long productosStockBajo = inventarioRepository.contarProductosActivosConStockBajo(STOCK_MINIMO);
        long lotesPorVencer = loteRepository.countByFechaVencimientoBetween(hoy, hastaVencimiento);

        List<DashboardMetricDTO> metricas = List.of(
                new DashboardMetricDTO("ventasHoy", "Ventas netas de hoy", moneda(ventasHoy), "Ventas válidas menos devoluciones completadas", tonoResultado(ventasHoy)),
                new DashboardMetricDTO("ventasMes", "Ventas netas del mes", moneda(ventasMes), "Acumulado neto desde inicio de mes", tonoResultado(ventasMes)),

                new DashboardMetricDTO("recuperacionMes", "Recuperación de Préstamos", moneda(recuperacionMes), "Abonos directos de empleados en el mes", "positivo"),

                new DashboardMetricDTO("comprasMes", "Compras confirmadas del mes", moneda(comprasMes), "No incluye borradores", "neutral"),
                new DashboardMetricDTO(
                        "gastosOperativosMes",
                        "Total Salidas de Caja",
                        moneda(gastosYDesembolsosMes),
                        "Incluye nómina, gastos regulares y desembolsos a empleados",
                        "riesgo"
                ),
                new DashboardMetricDTO(
                        "resultadoOperativoMes",
                        "Flujo de Caja Neto",
                        moneda(flujoCajaNetoMes),
                        "Efectivo total generado (Entradas menos Salidas)",
                        tonoResultado(flujoCajaNetoMes)
                ),
                new DashboardMetricDTO("porCobrar", "Por cobrar", moneda(balancePendiente), ventasPendientes + " ventas pendientes", "alerta"),
                new DashboardMetricDTO("citasHoy", "Citas pendientes hoy", numero(citasPendientesHoy), "Servicios veterinarios por atender", "neutral"),
                new DashboardMetricDTO("stockBajo", "Productos con stock bajo", numero(productosStockBajo), "Umbral: " + STOCK_MINIMO.stripTrailingZeros().toPlainString(), "riesgo"),
                new DashboardMetricDTO("lotesVencen", "Lotes por vencer", numero(lotesPorVencer), "Proximos " + DIAS_VENCIMIENTO + " dias", "riesgo"),
                new DashboardMetricDTO("despachosPendientes", "Despachos pendientes", numero(despachosPendientes), "Sin fecha de entrega", "neutral")
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

    private BigDecimal calcularRecuperacionDirecta(LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal abonosPrestamo = abonoPrestamoRepository.sumarAbonosEntre(inicio.toLocalDate(), fin.toLocalDate());
        BigDecimal abonosAnticipo = abonoAnticipoRepository.sumarAbonosEntre(inicio.toLocalDate(), fin.toLocalDate());
        return valorSeguro(abonosPrestamo).add(valorSeguro(abonosAnticipo));
    }

    private BigDecimal calcularBalancePendiente() {
        return ventaRepository.findConCobrosByEstado(EstadoVenta.PENDIENTE).stream()
                .map(this::calcularBalanceVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularVentasNetas(LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal ventas = ventaRepository.sumarMontoEntre(inicio, fin, ESTADOS_VENTA_CONTABILIZADOS);
        BigDecimal devoluciones = devolucionVentaRepository.sumarMontoEntre(
                inicio,
                fin,
                EstadoDevolucion.COMPLETADA
        );
        return valorSeguro(ventas).subtract(valorSeguro(devoluciones));
    }

    private BigDecimal calcularBalanceVenta(Venta venta) {
        BigDecimal cobrado = venta.getCobros().stream()
                .map(cobro -> valorSeguro(cobro.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return valorSeguro(venta.getMontoTotal()).subtract(cobrado).max(BigDecimal.ZERO);
    }

    private List<DashboardSeriesPointDTO> construirSerieVentas(LocalDate inicioSerie) {
        Map<LocalDate, BigDecimal> acumulado = inicializarSerie(inicioSerie);
        ventaRepository.findByFechaHoraVentaGreaterThanEqualAndEstadoInOrderByFechaHoraVentaAsc(
                        inicioSerie.atStartOfDay(),
                        ESTADOS_VENTA_CONTABILIZADOS
                )
                .forEach(venta -> acumular(acumulado, venta.getFechaHoraVenta().toLocalDate(), venta.getMontoTotal()));
        devolucionVentaRepository.findByFechaHoraGreaterThanEqualAndEstadoOrderByFechaHoraAsc(
                        inicioSerie.atStartOfDay(),
                        EstadoDevolucion.COMPLETADA
                )
                .forEach(devolucion -> acumular(
                        acumulado,
                        devolucion.getFechaHora().toLocalDate(),
                        valorSeguro(devolucion.getMontoTotal()).negate()
                ));
        return convertirSerie(acumulado);
    }

    private List<DashboardSeriesPointDTO> construirSerieCompras(LocalDate inicioSerie) {
        Map<LocalDate, BigDecimal> acumulado = inicializarSerie(inicioSerie);
        compraRepository.findByFechaHoraCompraGreaterThanEqualAndEstadoRecepcionNotOrderByFechaHoraCompraAsc(
                        inicioSerie.atStartOfDay(),
                        EstadoRecepcion.BORRADOR
                )
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
        return inventarioRepository.contarProductosConStockPorCategoria().stream()
                .map(row -> new DashboardCategoryDTO(
                        etiquetaCategoria((CategoriaProducto) row[0]),
                        BigDecimal.valueOf(((Number) row[1]).longValue())
                ))
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
        return FormatoNumeroCompactoUtil.formatearMoneda(valorSeguro(valor));
    }

    private String numero(long valor) {
        return FormatoNumeroCompactoUtil.formatear(valor);
    }

    private String tonoResultado(BigDecimal valor) {
        return valorSeguro(valor).compareTo(BigDecimal.ZERO) >= 0 ? "positivo" : "riesgo";
    }
}
