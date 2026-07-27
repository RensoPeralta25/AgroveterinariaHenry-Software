package com.agroveterinaria.service;

import com.agroveterinaria.dto.despacho.DespachoResumenDTO;
import com.agroveterinaria.dto.despacho.LineaDespachoDTO;
import com.agroveterinaria.dto.recepcion.GastoOperativoUI;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoTransporte;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.enums.TipoGasto;
import com.agroveterinaria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final VentaRepository ventaRepository;
    private final TransporteRepository transporteRepository;
    private final DetalleDespachoRepository detalleDespachoRepository;
    private final InventarioRepository inventarioRepository;
    private final GastoOperativoRepository gastoOperativoRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DespachoService(DespachoRepository despachoRepository, TransferenciaRepository transferenciaRepository, VentaRepository ventaRepository, TransporteRepository transporteRepository, DetalleDespachoRepository detalleDespachoRepository, InventarioRepository inventarioRepository, GastoOperativoRepository gastoOperativoRepository) {
        this.despachoRepository = despachoRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.ventaRepository = ventaRepository;
        this.transporteRepository = transporteRepository;
        this.detalleDespachoRepository = detalleDespachoRepository;
        this.inventarioRepository = inventarioRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Despacho> buscarPorIdConTransporte(Long idDespacho) {
        return despachoRepository.findByIdWithFullTransporte(idDespacho);
    }

    @Transactional(readOnly = true)
    public List<DespachoResumenDTO> obtenerColaDespachos() {
        List<Despacho> despachos = despachoRepository.findAllConRelaciones();

        return despachos.stream().map(d -> {
            DespachoResumenDTO dto = new DespachoResumenDTO();
            dto.setIdDespacho(d.getIdDespacho());
            dto.setCodigo(String.format("DSP-%04d", d.getIdDespacho()));
            dto.setFechaProgramadaRaw(d.getFechaHoraSalidaProgramada());
            dto.setFechaProgramadaFormateada(d.getFechaHoraSalidaProgramada().format(formatter));

            if (d.getTransferencia() != null) {
                dto.setTipo("Transferencia");
                dto.setDestinatario(d.getTransferencia().getAlmacenDestino().getNombre());
                dto.setDireccionEntrega(d.getTransferencia().getAlmacenDestino().getDireccion());
                dto.setEstado(d.getTransferencia().getEstado().name());
            } else if (d.getVentas() != null && !d.getVentas().isEmpty()) {
                Venta venta = d.getVentas().iterator().next();
                dto.setTipo("Venta");
                dto.setDestinatario(venta.getCliente().getPersona().getNombre());
                dto.setDireccionEntrega(venta.getCliente().getPersona().getDireccion());
                dto.setEstado(venta.getEstado().getEtiqueta());
            } else {
                dto.setTipo("Desconocido");
                dto.setDestinatario("-");
                dto.setDireccionEntrega("-");
                dto.setEstado("ERROR");
            }
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Transferencia> obtenerTransferenciasPendientes() {
        return transferenciaRepository.findByEstadoIn(List.of(
                com.agroveterinaria.enums.EstadoTransferencia.PENDIENTE_DESPACHO,
                com.agroveterinaria.enums.EstadoTransferencia.DESPACHADA_PARCIAL
        ));
    }

    @Transactional(readOnly = true)
    public List<Venta> obtenerVentasPendientes() {
        return ventaRepository.findVentasConMercanciaPendienteDeDespacho();
    }

    @Transactional(readOnly = true)
    public List<LineaDespachoDTO> obtenerLineasPendientesTransferencia(Transferencia transferenciaUI) {
        Transferencia transferenciaConectada = transferenciaRepository
                .findById(transferenciaUI.getIdTransferencia())
                .orElseThrow(() -> new RuntimeException("Error al cargar datos de la transferencia"));
        return transferenciaConectada.getDetalles().stream().map(dt -> {
                    BigDecimal despachadoHistorico = detalleDespachoRepository.sumCantidadByIdDetalleTransferencia(dt.getIdDetalleTransferencia());
                    return new LineaDespachoDTO(dt, despachadoHistorico);
                }).filter(dto -> dto.getCantidadPendiente().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Transactional
    public Despacho procesarDespachoTransferencia(Transferencia transferencia, Vehiculo vehiculo, Empleado conductor, List<LineaDespachoDTO> lineasAProcesar) {
        Despacho despacho = new Despacho();

        Transporte transporte = (transferencia.getTransporte() != null) ? transferencia.getTransporte() : new Transporte();
        transporte.setVehiculo(vehiculo);
        transporte.setConductor(conductor);
        if(transporte.getIdTransporte() == null) {
            transporte.setEstado(EstadoTransporte.PROGRAMADO);
            transporte.setDescuento(BigDecimal.ZERO);
        }
        transporte = transporteRepository.save(transporte);

        despacho.setTransferencia(transferencia);
        despacho.setTransporte(transporte);
        despacho.setFechaHoraSalidaProgramada(LocalDateTime.now());

        boolean quedaMercanciaPendiente = false;

        for (LineaDespachoDTO linea : lineasAProcesar) {
            BigDecimal aDespachar = linea.getCantidadADespacharActual();

            if (aDespachar != null && aDespachar.compareTo(BigDecimal.ZERO) > 0) {
                Inventario invOrigen = inventarioRepository.findByAlmacenAndLote(transferencia.getAlmacenOrigen(), linea.getDetalleTransferencia().getLote())
                        .orElseThrow(() -> new RuntimeException("No hay inventario del lote " + linea.getNumeroLote()));

                if (invOrigen.getCantidadActual().compareTo(aDespachar) < 0) {
                    throw new RuntimeException("Stock físico insuficiente en almacén origen para el lote " + linea.getNumeroLote());
                }
                invOrigen.setCantidadActual(invOrigen.getCantidadActual().subtract(aDespachar));
                inventarioRepository.save(invOrigen);

                DetalleDespacho detalleCamion = new DetalleDespacho();
                detalleCamion.setDespacho(despacho);
                detalleCamion.setDetalleTransferencia(linea.getDetalleTransferencia());
                detalleCamion.setLote(linea.getDetalleTransferencia().getLote());
                detalleCamion.setAlmacen(transferencia.getAlmacenOrigen());
                detalleCamion.setCantidad(aDespachar);
                despacho.addDetalle(detalleCamion);
            }

            BigDecimal totalAcumulado = linea.getCantidadYaDespachada().add(aDespachar != null ? aDespachar : BigDecimal.ZERO);
            if (totalAcumulado.compareTo(linea.getCantidadSolicitada()) < 0) {
                quedaMercanciaPendiente = true;
            }
        }

        if (quedaMercanciaPendiente) {
            transferencia.setEstado(com.agroveterinaria.enums.EstadoTransferencia.DESPACHADA_PARCIAL);
        } else {
            transferencia.setEstado(com.agroveterinaria.enums.EstadoTransferencia.EN_TRANSITO);
        }

        transferenciaRepository.save(transferencia);
        return despachoRepository.save(despacho);
    }

    @Transactional(readOnly = true)
    public List<DespachoResumenDTO> obtenerDocumentosPendientesDespacho() {
        List<DespachoResumenDTO> pendientes = new java.util.ArrayList<>();

        List<Venta> ventasListas = obtenerVentasPendientes();
        for (Venta v : ventasListas) {
            DespachoResumenDTO dto = new DespachoResumenDTO();
            dto.setCodigo("VTA-" + v.getIdVenta());
            dto.setTipo("Venta");
            dto.setDestinatario(v.getCliente().getPersona().getNombre());
            dto.setVentaOriginal(v);
            pendientes.add(dto);
        }

        List<Transferencia> transferenciasListas = obtenerTransferenciasPendientes();
        for (Transferencia t : transferenciasListas) {
            DespachoResumenDTO dto = new DespachoResumenDTO();
            dto.setCodigo("TRF-" + t.getIdTransferencia());
            dto.setTipo("Transferencia");
            dto.setDestinatario(t.getAlmacenDestino().getNombre());
            dto.setTransferenciaOriginal(t);
            pendientes.add(dto);
        }

        return pendientes;
    }

    @Transactional(readOnly = true)
    public List<LineaDespachoDTO> obtenerLineasPendientesVenta(Long idVenta) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("Error al cargar datos de la venta"));

        return venta.getDetallesVentas().stream().map(dv -> {
            BigDecimal despachadoHistorico = detalleDespachoRepository.sumCantidadByIdDetalleVenta(dv.getIdDetalleVenta());
            return new LineaDespachoDTO(dv, despachadoHistorico);
        }).filter(dto -> dto.getCantidadPendiente().compareTo(BigDecimal.ZERO) > 0).toList();
    }

    @Transactional
    public Despacho procesarDespachoVenta(Long idVenta, Vehiculo vehiculo, Empleado conductor, List<LineaDespachoDTO> lineasAProcesar) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        Despacho despacho = new Despacho();

        Transporte transporte = new Transporte();
        transporte.setVehiculo(vehiculo);
        transporte.setConductor(conductor);
        transporte.setEstado(EstadoTransporte.PROGRAMADO);
        transporte.setDescuento(BigDecimal.ZERO);
        transporte = transporteRepository.save(transporte);

        despacho.setTransporte(transporte);
        despacho.setFechaHoraSalidaProgramada(LocalDateTime.now());
        despacho.getVentas().add(venta);

        boolean quedaMercanciaPendiente = false;

        for (LineaDespachoDTO linea : lineasAProcesar) {
            BigDecimal aDespachar = linea.getCantidadADespacharActual();
            if (aDespachar != null && aDespachar.compareTo(BigDecimal.ZERO) > 0) {

                com.agroveterinaria.entity.Lote loteFisico = linea.getDetalleVenta().getLote() != null ?
                        linea.getDetalleVenta().getLote() : linea.getLoteSeleccionadoFisicamente();

                if (loteFisico == null) {
                    throw new RuntimeException("No se ha determinado un lote válido para el producto " + linea.getNombreProducto());
                }

                Inventario invOrigen = inventarioRepository.findByAlmacenAndLote(
                        linea.getDetalleVenta().getAlmacen(),
                        loteFisico
                ).orElseThrow(() -> new RuntimeException("No se encontró inventario para el lote "
                        + loteFisico.getNumeroLote() + " en el almacén " + linea.getDetalleVenta().getAlmacen().getNombre()));

                if (invOrigen.getCantidadActual().compareTo(aDespachar) < 0) {
                    throw new RuntimeException("Stock físico insuficiente para el lote " + loteFisico.getNumeroLote());
                }

                invOrigen.setCantidadActual(invOrigen.getCantidadActual().subtract(aDespachar));
                inventarioRepository.save(invOrigen);

                DetalleDespacho detalleCamion = new DetalleDespacho();
                detalleCamion.setDespacho(despacho);
                detalleCamion.setDetalleVenta(linea.getDetalleVenta());
                detalleCamion.setLote(loteFisico);
                detalleCamion.setAlmacen(linea.getDetalleVenta().getAlmacen());
                detalleCamion.setCantidad(aDespachar);
                despacho.addDetalle(detalleCamion);
            }

            BigDecimal totalAcumulado = linea.getCantidadYaDespachada().add(aDespachar != null ? aDespachar : BigDecimal.ZERO);
            if (totalAcumulado.compareTo(linea.getCantidadSolicitada()) < 0) {
                quedaMercanciaPendiente = true;
            }
        }

        return despachoRepository.save(despacho);
    }

    @Transactional
    public void liquidarViaje(Long idTransporte, List<GastoOperativoUI> gastosUI) {
        Transporte transporte = transporteRepository.findById(idTransporte)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el registro de transporte."));

        for (GastoOperativoUI dto : gastosUI) {
            if (dto.getMonto() != null && dto.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                GastoOperativo gasto = new GastoOperativo();
                gasto.setTipoGasto(TipoGasto.VARIABLE);
                gasto.setFecha(LocalDate.now());
                gasto.setMonto(dto.getMonto());
                gasto.setNotas(dto.getNotas() != null && !dto.getNotas().isBlank()
                        ? "Ruta Transporte #" + transporte.getIdTransporte() + " - " + dto.getNotas()
                        : "Ruta Transporte #" + transporte.getIdTransporte());

                GastoOperativo gastoGuardado = gastoOperativoRepository.save(gasto);
                transporte.addGasto(gastoGuardado);
            }
        }

        transporte.setEstado(EstadoTransporte.COMPLETADO);
        transporte.setFechaHoraLlegada(LocalDateTime.now());

        transporteRepository.save(transporte);
    }

    @Transactional(readOnly = true)
    public Despacho obtenerDespachoConDetalles(Long idDespacho) {
        if (idDespacho == null) return null;
        return despachoRepository.findDespachoConDetallesByIdDespacho(idDespacho).orElse(null);
    }
}