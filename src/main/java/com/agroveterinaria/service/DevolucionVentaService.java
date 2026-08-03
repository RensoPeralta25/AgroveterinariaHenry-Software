package com.agroveterinaria.service;

import com.agroveterinaria.dto.devolucion.LineaElegibleDevolucionDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.EstadoDevolucion;
import com.agroveterinaria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DevolucionVentaService {

    private final DevolucionVentaRepository devolucionRepository;
    private final DetalleDevVentaRepository detalleDevRepository;
    private final DetalleDespachoRepository detalleDespachoRepository; // NUEVO
    private final InventarioService inventarioService;
    private final NotaDeCreditoRepository notaCreditoRepository;
    private final CobroRepository cobroRepository;

    public DevolucionVentaService(DevolucionVentaRepository devolucionRepository,
                                  DetalleDevVentaRepository detalleDevRepository,
                                  DetalleDespachoRepository detalleDespachoRepository,
                                  InventarioService inventarioService,
                                  NotaDeCreditoRepository notaCreditoRepository,
                                  CobroRepository cobroRepository) {
        this.devolucionRepository = devolucionRepository;
        this.detalleDevRepository = detalleDevRepository;
        this.detalleDespachoRepository = detalleDespachoRepository;
        this.inventarioService = inventarioService;
        this.notaCreditoRepository = notaCreditoRepository;
        this.cobroRepository = cobroRepository;
    }

    @Transactional(readOnly = true)
    public List<DevolucionVenta> listarTodas() {
        return devolucionRepository.findAllConRelaciones();
    }

    @Transactional(readOnly = true)
    public List<LineaElegibleDevolucionDTO> obtenerLineasElegibles(Venta venta) {
        List<LineaElegibleDevolucionDTO> elegibles = new ArrayList<>();
        if (venta == null || venta.getDetallesVentas() == null) return elegibles;

        for (DetalleVenta dv : venta.getDetallesVentas()) {
            boolean esServicio = dv.getProducto().getCategoria() == CategoriaProducto.SERVICIO;

            if (esServicio) {
                BigDecimal devuelto = detalleDevRepository.sumarCantidadesDevueltasPorDetalleVentaSinLote(dv.getIdDetalleVenta());
                BigDecimal disponible = dv.getCantidad().subtract(devuelto != null ? devuelto : BigDecimal.ZERO);
                if (disponible.compareTo(BigDecimal.ZERO) > 0) {
                    elegibles.add(new LineaElegibleDevolucionDTO(dv, null, disponible));
                }
                continue;
            }

            List<DetalleDespacho> despachos = detalleDespachoRepository.findByDetalleVentaIdDetalleVenta(dv.getIdDetalleVenta());

            if (!despachos.isEmpty()) {
                Map<Long, LoteDespachadoTemporal> mapaLotes = new HashMap<>();
                for (DetalleDespacho d : despachos) {
                    if (d.getLote() != null) d.getLote().getNumeroLote();
                    mapaLotes.computeIfAbsent(d.getLote().getIdLote(), k -> new LoteDespachadoTemporal(d.getLote(), BigDecimal.ZERO))
                            .sumar(d.getCantidad());
                }

                for (LoteDespachadoTemporal temp : mapaLotes.values()) {
                    BigDecimal devuelto = detalleDevRepository.sumarCantidadesDevueltasPorDetalleVentaAndLote(dv.getIdDetalleVenta(), temp.lote.getIdLote());
                    BigDecimal disponible = temp.cantidad.subtract(devuelto != null ? devuelto : BigDecimal.ZERO);
                    if (disponible.compareTo(BigDecimal.ZERO) > 0) {
                        elegibles.add(new LineaElegibleDevolucionDTO(dv, temp.lote, disponible));
                    }
                }
            } else if (dv.getLote() != null && !Boolean.TRUE.equals(venta.getLlevaDespacho())) {
                dv.getLote().getNumeroLote();
                BigDecimal devuelto = detalleDevRepository.sumarCantidadesDevueltasPorDetalleVentaAndLote(dv.getIdDetalleVenta(), dv.getLote().getIdLote());
                BigDecimal disponible = dv.getCantidad().subtract(devuelto != null ? devuelto : BigDecimal.ZERO);
                if (disponible.compareTo(BigDecimal.ZERO) > 0) {
                    elegibles.add(new LineaElegibleDevolucionDTO(dv, dv.getLote(), disponible));
                }
            }
        }
        return elegibles;
    }

    private static class LoteDespachadoTemporal {
        Lote lote;
        BigDecimal cantidad;
        LoteDespachadoTemporal(Lote lote, BigDecimal cantidad) { this.lote = lote; this.cantidad = cantidad; }
        void sumar(BigDecimal suma) { this.cantidad = this.cantidad.add(suma); }
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

            boolean esServicio = detalle.getDetalleVenta().getProducto().getCategoria() == CategoriaProducto.SERVICIO;
            BigDecimal devueltoHistorico;
            BigDecimal cantidadDespachada = BigDecimal.ZERO;

            if (esServicio) {
                devueltoHistorico = detalleDevRepository.sumarCantidadesDevueltasPorDetalleVentaSinLote(detalle.getDetalleVenta().getIdDetalleVenta());
                cantidadDespachada = detalle.getDetalleVenta().getCantidad();
            } else {
                devueltoHistorico = detalleDevRepository.sumarCantidadesDevueltasPorDetalleVentaAndLote(detalle.getDetalleVenta().getIdDetalleVenta(), detalle.getLote().getIdLote());

                List<DetalleDespacho> despachos = detalleDespachoRepository.findByDetalleVentaIdDetalleVenta(detalle.getDetalleVenta().getIdDetalleVenta());
                if (!despachos.isEmpty()) {
                    for (DetalleDespacho d : despachos) {
                        if (d.getLote().getIdLote().equals(detalle.getLote().getIdLote())) {
                            cantidadDespachada = cantidadDespachada.add(d.getCantidad());
                        }
                    }
                } else if (detalle.getDetalleVenta().getLote() != null
                        && detalle.getDetalleVenta().getLote().getIdLote().equals(detalle.getLote().getIdLote())
                        && !Boolean.TRUE.equals(detalle.getDetalleVenta().getVenta().getLlevaDespacho())) {
                    cantidadDespachada = detalle.getDetalleVenta().getCantidad();
                }
            }

            if (devueltoHistorico == null) devueltoHistorico = BigDecimal.ZERO;
            BigDecimal cantidadDisponibleParaDevolver = cantidadDespachada.subtract(devueltoHistorico);

            if (detalle.getCantidadDevuelta().compareTo(cantidadDisponibleParaDevolver) > 0) {
                throw new IllegalArgumentException(
                        "Fraude/Error: No puede devolver " + detalle.getCantidadDevuelta() +
                                " del lote " + (esServicio ? "Servicio" : detalle.getLote().getNumeroLote()) +
                                ". Solo le quedan " + cantidadDisponibleParaDevolver + " unidades en su poder."
                );
            }

            if (!esServicio) {
                inventarioService.sumarStock(detalle.getAlmacenEntrada(), detalle.getLote(), detalle.getCantidadDevuelta());
            }

            detalle.setDevolucionVenta(devolucion);
            montoCalculado = montoCalculado.add(calcularMontoDetalle(detalle.getDetalleVenta(), detalle.getCantidadDevuelta()));
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
        if (detalle == null || cantidadDevuelta == null || cantidadDevuelta.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal cantidadVendida = detalle.getCantidad();
        if (cantidadVendida == null || cantidadVendida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad original de la venta no es válida.");
        }

        BigDecimal precio = detalle.getPrecioUnitarioVenta() != null ? detalle.getPrecioUnitarioVenta() : BigDecimal.ZERO;
        BigDecimal impuestoLinea = detalle.getImpuesto() != null ? detalle.getImpuesto() : BigDecimal.ZERO;
        BigDecimal proporcion = cantidadDevuelta.divide(cantidadVendida, 8, RoundingMode.HALF_UP);
        BigDecimal brutoDevuelto = precio.multiply(cantidadDevuelta).add(impuestoLinea.multiply(proporcion));

        Venta venta = detalle.getVenta();
        if (venta == null) {
            return brutoDevuelto.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalDetalles = venta.calcularSubtotalDetalles();
        if (totalDetalles.compareTo(BigDecimal.ZERO) <= 0 || venta.getMontoTotal() == null) {
            return brutoDevuelto.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal envio = venta.getCostoEnvio() != null ? venta.getCostoEnvio() : BigDecimal.ZERO;
        BigDecimal descuentoGlobal = totalDetalles.add(envio).subtract(venta.getMontoTotal()).max(BigDecimal.ZERO).min(totalDetalles);
        BigDecimal factorNeto = totalDetalles.subtract(descuentoGlobal).divide(totalDetalles, 8, RoundingMode.HALF_UP);

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
