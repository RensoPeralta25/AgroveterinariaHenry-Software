package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoDevolucion;
import com.agroveterinaria.repository.CobroRepository;
import com.agroveterinaria.repository.DetalleDevVentaRepository;
import com.agroveterinaria.repository.DevolucionVentaRepository;
import com.agroveterinaria.repository.NotaDeCreditoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DevolucionVentaService {

    private final DevolucionVentaRepository devolucionRepository;
    private final DetalleDevVentaRepository detalleDevRepository;
    private final InventarioService inventarioService;
    private final NotaDeCreditoRepository notaCreditoRepository;
    private final CobroRepository cobroRepository;

    public DevolucionVentaService(DevolucionVentaRepository devolucionRepository,
                                  DetalleDevVentaRepository detalleDevRepository,
                                  InventarioService inventarioService,
                                  NotaDeCreditoRepository notaCreditoRepository,
                                  CobroRepository cobroRepository) {
        this.devolucionRepository = devolucionRepository;
        this.detalleDevRepository = detalleDevRepository;
        this.inventarioService = inventarioService;
        this.notaCreditoRepository = notaCreditoRepository;
        this.cobroRepository = cobroRepository;
    }

    @Transactional(readOnly = true)
    public List<DevolucionVenta> listarTodas() {
        return devolucionRepository.findAllConRelaciones();
    }

    @Transactional(rollbackFor = Exception.class)
    public DevolucionVenta registrarDevolucion(DevolucionVenta devolucion, boolean generarNotaCredito) {

        if (devolucion.getDetalles() == null || devolucion.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La devolución debe contener al menos un producto.");
        }

        BigDecimal montoCalculado = BigDecimal.ZERO;
        for (DetalleDevVenta detalle : devolucion.getDetalles()) {

            if (detalle.getCantidadDevuelta() == null || detalle.getCantidadDevuelta().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad a devolver debe ser mayor a 0.");
            }

            BigDecimal cantidadComprada = detalle.getDetalleVenta().getCantidad();
            BigDecimal cantidadYaDevuelta = detalleDevRepository.sumarCantidadesDevueltasPorDetalleVenta(
                    detalle.getDetalleVenta().getIdDetalleVenta()
            );

            BigDecimal cantidadDisponibleParaDevolver = cantidadComprada.subtract(cantidadYaDevuelta);

            if (detalle.getCantidadDevuelta().compareTo(cantidadDisponibleParaDevolver) > 0) {
                throw new IllegalArgumentException(
                        "Intento de fraude o error: No puede devolver " + detalle.getCantidadDevuelta() +
                                " del producto " + detalle.getDetalleVenta().getProducto().getNombre() +
                                ". Solo quedan " + cantidadDisponibleParaDevolver + " unidades elegibles para devolución de esa factura."
                );
            }

            inventarioService.sumarStock(
                    detalle.getAlmacenEntrada(),
                    detalle.getLote(),
                    detalle.getCantidadDevuelta()
            );

            detalle.setDevolucionVenta(devolucion);
            montoCalculado = montoCalculado.add(calcularMontoDetalle(
                    detalle.getDetalleVenta(),
                    detalle.getCantidadDevuelta()
            ));
        }
        devolucion.setMontoTotal(montoCalculado.setScale(2, RoundingMode.HALF_UP));

        if (generarNotaCredito) {
            if (devolucion.getMontoTotal() == null || devolucion.getMontoTotal().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto de la devolución debe ser mayor a 0 para generar una Nota de Crédito.");
            }

            NotaDeCredito nc = new NotaDeCredito();
            nc.setCliente(devolucion.getCliente());
            nc.setMonto(devolucion.getMontoTotal());
            nc.setSaldoDisponible(devolucion.getMontoTotal());
            nc.setFechaEmision(LocalDateTime.now());
            String motivo = "Devolución de venta: " + devolucion.getRazonDevolucion();
            nc.setMotivo(motivo.substring(0, Math.min(motivo.length(), 255)));

            NotaDeCredito ncGuardada = notaCreditoRepository.save(nc);

            devolucion.setNotaDeCredito(ncGuardada);
        }

        devolucion.setEstado(EstadoDevolucion.COMPLETADA);
        devolucion.setFechaHora(LocalDateTime.now());

        return devolucionRepository.save(devolucion);
    }

    public BigDecimal calcularMontoDetalle(DetalleVenta detalle, BigDecimal cantidadDevuelta) {
        if (detalle == null || cantidadDevuelta == null
                || cantidadDevuelta.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal cantidadVendida = detalle.getCantidad();
        if (cantidadVendida == null || cantidadVendida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad original de la venta no es válida.");
        }

        BigDecimal precio = detalle.getPrecioUnitarioVenta() != null
                ? detalle.getPrecioUnitarioVenta()
                : BigDecimal.ZERO;
        BigDecimal impuestoLinea = detalle.getImpuesto() != null
                ? detalle.getImpuesto()
                : BigDecimal.ZERO;
        BigDecimal proporcion = cantidadDevuelta.divide(cantidadVendida, 8, RoundingMode.HALF_UP);
        BigDecimal brutoDevuelto = precio.multiply(cantidadDevuelta)
                .add(impuestoLinea.multiply(proporcion));

        Venta venta = detalle.getVenta();
        if (venta == null) {
            return brutoDevuelto.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalDetalles = venta.calcularSubtotalDetalles();
        if (totalDetalles.compareTo(BigDecimal.ZERO) <= 0 || venta.getMontoTotal() == null) {
            return brutoDevuelto.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal envio = venta.getCostoEnvio() != null ? venta.getCostoEnvio() : BigDecimal.ZERO;
        BigDecimal descuentoGlobal = totalDetalles.add(envio)
                .subtract(venta.getMontoTotal())
                .max(BigDecimal.ZERO)
                .min(totalDetalles);
        BigDecimal factorNeto = totalDetalles.subtract(descuentoGlobal)
                .divide(totalDetalles, 8, RoundingMode.HALF_UP);

        return brutoDevuelto.multiply(factorNeto).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<DetalleDevVenta> obtenerDetallesDeDevolucion(Long idDevolucionVenta) {
        if (idDevolucionVenta == null) {
            return List.of();
        }
        return detalleDevRepository.findByDevolucionVentaIdDevolucionVenta(idDevolucionVenta);
    }

    @Transactional(rollbackFor = Exception.class)
    public void anularDevolucionManual(Long idDevolucionVenta) {
        DevolucionVenta devolucion = devolucionRepository.findById(idDevolucionVenta)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la devolución con ID: " + idDevolucionVenta));

        if (devolucion.getEstado() == EstadoDevolucion.ANULADA) {
            throw new IllegalStateException("Esta devolución ya se encuentra anulada.");
        }

        for (DetalleDevVenta detalle : devolucion.getDetalles()) {
            inventarioService.restarStock(
                    detalle.getAlmacenEntrada(),
                    detalle.getLote(),
                    detalle.getCantidadDevuelta()
            );
        }

        if (devolucion.getNotaDeCredito() != null) {
            NotaDeCredito nota = devolucion.getNotaDeCredito();
            if (cobroRepository.existsByNotaDeCredito(nota)) {
                throw new IllegalStateException(
                        "No se puede anular la devolución porque su nota de crédito ya fue utilizada en una venta."
                );
            }
            devolucion.setNotaDeCredito(null);
            notaCreditoRepository.delete(nota);
        }

        devolucion.setEstado(EstadoDevolucion.ANULADA);
        devolucionRepository.save(devolucion);
    }
}
