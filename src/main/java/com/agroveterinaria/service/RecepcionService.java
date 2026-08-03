package com.agroveterinaria.service;

import com.agroveterinaria.dto.recepcion.RecepcionResumenDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoRecepcion;
import com.agroveterinaria.enums.EstadoTransferencia;
import com.agroveterinaria.enums.EstadoTransporte;
import com.agroveterinaria.enums.TipoGasto;
import com.agroveterinaria.repository.*;
import com.agroveterinaria.dto.recepcion.GastoOperativoUI;
import com.agroveterinaria.dto.recepcion.RecepcionItemUI;
import com.agroveterinaria.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecepcionService {

    private final RecepcionRepository recepcionRepository;
    private final CompraRepository compraRepository;
    private final LoteRepository loteRepository;
    private final InventarioRepository inventarioRepository;
    private final TransporteRepository transporteRepository;
    private final GastoOperativoRepository gastoOperativoRepository;
    private final DetalleRecepcionRepository detalleRecepcionRepository;
    private final ProductoRepository productoRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final DespachoRepository despachoRepository;
    private final AjusteInventarioRepository ajusteInventarioRepository;
    private final SecurityService securityService;
    private final DetalleDespachoRepository detalleDespachoRepository;
    private final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    public RecepcionService(RecepcionRepository recepcionRepository, CompraRepository compraRepository,
                            LoteRepository loteRepository, InventarioRepository inventarioRepository,
                            TransporteRepository transporteRepository, GastoOperativoRepository gastoOperativoRepository,
                            ProductoRepository productoRepository, DetalleRecepcionRepository detalleRecepcionRepository,
                            TransferenciaRepository transferenciaRepository, DespachoRepository despachoRepository,
                            AjusteInventarioRepository ajusteInventarioRepository, SecurityService securityService,
                            DetalleDespachoRepository detalleDespachoRepository) {
        this.recepcionRepository = recepcionRepository;
        this.compraRepository = compraRepository;
        this.loteRepository = loteRepository;
        this.inventarioRepository = inventarioRepository;
        this.transporteRepository = transporteRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
        this.productoRepository = productoRepository;
        this.detalleRecepcionRepository = detalleRecepcionRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.despachoRepository = despachoRepository;
        this.ajusteInventarioRepository = ajusteInventarioRepository;
        this.securityService = securityService;
        this.detalleDespachoRepository = detalleDespachoRepository;
    }

    @Transactional
    public void procesarRecepcionTransaccional(
            String tipoDocumento, Long idDocumento,
            List<RecepcionItemUI> itemsFisicos,
            String tipoLogistica,
            BigDecimal costoFleteExterno,
            Vehiculo vehiculo,
            Empleado conductor,
            Ruta ruta,
            List<GastoOperativoUI> gastosInternos) {

        Recepcion recepcion = new Recepcion();
        recepcion.setFechaHoraLlegadaProgramada(LocalDateTime.now());
        recepcion.setFechaHoraRecepcion(LocalDateTime.now());

        Compra compra = null;
        Transferencia transferencia = null;

        if ("Compra".equals(tipoDocumento)) {
            compra = compraRepository.findById(idDocumento).orElseThrow();
            recepcion.getCompras().add(compra);
        } else {
            transferencia = transferenciaRepository.findById(idDocumento).orElseThrow();
            recepcion.setTransferencia(transferencia);
        }

        Transporte transporteGuardado = null;
        if ("Transporte Interno (Vehículo propio)".equals(tipoLogistica) || "Mixto (Flete parcial + Transporte propio)".equals(tipoLogistica)) {

            Transporte transporte;

            if ("Transferencia".equals(tipoDocumento)) {
                Despacho despachoAsociado = despachoRepository.findFirstByTransferenciaOrderByFechaHoraSalidaProgramadaDesc(transferencia);
                transporte = despachoAsociado.getTransporte();
                transporte.setFechaHoraLlegada(LocalDateTime.now());
                transporte.setEstado(com.agroveterinaria.enums.EstadoTransporte.COMPLETADO);
            } else {
                if (vehiculo == null || conductor == null) throw new IllegalArgumentException("Faltan datos de transporte.");
                transporte = new Transporte();
                transporte.setVehiculo(vehiculo);
                transporte.setConductor(conductor);
                transporte.setRuta(ruta);
                transporte.setFechaHoraSalida(LocalDateTime.now().minusHours(2));
                transporte.setFechaHoraLlegada(LocalDateTime.now());
                transporte.setEstado(com.agroveterinaria.enums.EstadoTransporte.COMPLETADO);
                transporte.setDescuento(BigDecimal.ZERO);
            }

            for (GastoOperativoUI ui : gastosInternos) {
                if (ui.getMonto() != null && ui.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                    GastoOperativo go = new GastoOperativo();
                    go.setTipoGasto(com.agroveterinaria.enums.TipoGasto.VARIABLE);
                    go.setFecha(LocalDate.now());
                    go.setMonto(ui.getMonto());
                    go.setNotas(ui.getNotas() != null ? ui.getNotas().trim() : "Liquidación de Viaje");
                    transporte.addGasto(gastoOperativoRepository.save(go));
                }
            }

            transporteGuardado = transporteRepository.save(transporte);
            recepcion.setTransporte(transporteGuardado);
        } else if ("Flete / Delivery Externo".equals(tipoLogistica)) {
            if (costoFleteExterno != null && costoFleteExterno.compareTo(BigDecimal.ZERO) > 0) {
                GastoOperativo goFletePuro = new GastoOperativo();
                goFletePuro.setTipoGasto(com.agroveterinaria.enums.TipoGasto.VARIABLE);
                goFletePuro.setFecha(LocalDate.now());
                goFletePuro.setMonto(costoFleteExterno);
                goFletePuro.setNotas("Flete Externo");
                GastoOperativo savedGasto = gastoOperativoRepository.save(goFletePuro);

                if (compra != null) compra.setGastoAsociado(savedGasto);
            }
        }

        for (RecepcionItemUI item : itemsFisicos) {
            if (item.getCantidadRecibida() == null || item.getCantidadRecibida().compareTo(BigDecimal.ZERO) <= 0) continue;
            if (item.getAlmacenDestino() == null) {
                throw new IllegalArgumentException("Debe especificar un Almacén Destino para: " + item.getProducto().getNombre());
            }

            DetalleRecepcion dr = new DetalleRecepcion();
            dr.setAlmacen(item.getAlmacenDestino());
            dr.setCantidad(item.getCantidadRecibida());

            dr.setCantidadMerma(item.getCantidadMerma() != null ? item.getCantidadMerma() : BigDecimal.ZERO);
            dr.setJustificacionMerma(item.getJustificacionMerma());

            Lote lote;

            if (item.getDetalleCompra() != null) {
                DetalleCompra dc = item.getDetalleCompra();
                BigDecimal recibido = detalleRecepcionRepository.sumCantidadProcesadaByDetalleCompra(dc);
                BigDecimal pendiente = dc.getCantidad().subtract(recibido != null ? recibido : BigDecimal.ZERO);

                if (item.getCantidadRecibida().compareTo(pendiente) > 0) {
                    throw new IllegalArgumentException("Sobre-recepción bloqueada para: " + item.getProducto().getNombre());
                }

                String numLoteStr = item.getNumeroLote() != null ? item.getNumeroLote().trim() : "";
                lote = numLoteStr.isEmpty() ? null : loteRepository.findByProducto(item.getProducto()).stream()
                                                     .filter(l -> numLoteStr.equalsIgnoreCase(l.getNumeroLote())).findFirst().orElse(null);

                if (lote == null) {
                    lote = new Lote();
                    lote.setProducto(item.getProducto());
                    lote.setNumeroLote(numLoteStr.isEmpty() ? "GENERICO-" + LocalDate.now() : numLoteStr);
                    lote.setFechaVencimiento(item.getFechaVencimiento());
                    lote = loteRepository.save(lote);
                }
                dr.setDetalleCompra(dc);
                dr.setLote(lote);

            } else {
                DetalleTransferencia dt = item.getDetalleTransferencia();
                BigDecimal recibido = detalleRecepcionRepository.sumCantidadProcesadaByDetalleTransferencia(dt);
                BigDecimal pendiente = dt.getCantidad().subtract(recibido != null ? recibido : BigDecimal.ZERO);

                if (item.getCantidadRecibida().compareTo(pendiente) > 0) {
                    throw new IllegalArgumentException("Sobre-recepción bloqueada para: " + item.getProducto().getNombre());
                }

                lote = dt.getLote();
                dr.setDetalleTransferencia(dt);
                dr.setLote(lote);
            }

            Inventario inventario = inventarioRepository.findByAlmacenAndLote(item.getAlmacenDestino(), lote).orElse(null);
            if (inventario == null) {
                inventario = new Inventario();
                inventario.setAlmacen(item.getAlmacenDestino());
                inventario.setLote(lote);
                inventario.setCantidadActual(item.getCantidadRecibida());
            } else {
                inventario.setCantidadActual(inventario.getCantidadActual().add(item.getCantidadRecibida()));
            }
            inventarioRepository.save(inventario);

            recepcion.addDetalle(dr);

            if (dr.getCantidadMerma().compareTo(BigDecimal.ZERO) > 0) {
                if (item.getJustificacionMerma() == null || item.getJustificacionMerma().trim().isEmpty()) {
                    throw new IllegalArgumentException("Debe justificar la merma de " + item.getProducto().getNombre());
                }

                AjusteInventario ajusteMerma = new AjusteInventario();
                ajusteMerma.setAlmacen(item.getAlmacenDestino());
                ajusteMerma.setLote(lote);
                ajusteMerma.setEmpleado(conductor != null ? conductor : securityService.obtenerEmpleadoAutenticado());
                ajusteMerma.setTipoAjuste(com.agroveterinaria.enums.TipoAjuste.SALIDA);
                ajusteMerma.setCantidad(dr.getCantidadMerma());
                ajusteMerma.setJustificacion("MERMA EN RECEPCIÓN (" + tipoDocumento + " " + idDocumento + "): " + dr.getJustificacionMerma());
                ajusteMerma.setFechaHora(LocalDateTime.now());

                ajusteInventarioRepository.save(ajusteMerma);
            }
        }

        recepcionRepository.save(recepcion);

        if ("Compra".equals(tipoDocumento)) {
            boolean completado = compra.getDetalles().stream().allMatch(dc -> {
                BigDecimal rec = detalleRecepcionRepository.sumCantidadProcesadaByDetalleCompra(dc);
                return (rec != null ? rec : BigDecimal.ZERO).compareTo(dc.getCantidad()) >= 0;
            });
            compra.setEstadoRecepcion(completado ? com.agroveterinaria.enums.EstadoRecepcion.RECIBIDA : com.agroveterinaria.enums.EstadoRecepcion.PARCIAL);
            compraRepository.save(compra);
        } else {
            boolean completado = transferencia.getDetalles().stream().allMatch(dt -> {
                BigDecimal rec = detalleRecepcionRepository.sumCantidadProcesadaByDetalleTransferencia(dt);
                return (rec != null ? rec : BigDecimal.ZERO).compareTo(dt.getCantidad()) >= 0;
            });
            transferencia.setEstado(completado ? com.agroveterinaria.enums.EstadoTransferencia.COMPLETADA : com.agroveterinaria.enums.EstadoTransferencia.RECIBIDA_PARCIAL);
            transferenciaRepository.save(transferencia);
        }
    }

    @Transactional(readOnly = true)
    public List<RecepcionResumenDTO> obtenerColaRecepciones() {
        List<RecepcionResumenDTO> cola = new ArrayList<>();

        List<Compra> compras = compraRepository.findByEstadoRecepcionIn(
                List.of(EstadoRecepcion.PENDIENTE, EstadoRecepcion.PARCIAL)
        );
        for (Compra c : compras) {
            RecepcionResumenDTO dto = new RecepcionResumenDTO();
            dto.setCodigo("ORD-" + c.getIdCompra());
            dto.setTipo("Compra");
            dto.setOrigen(c.getProveedor().getNombre());
            dto.setFechaRaw(c.getFechaHoraCompra());
            dto.setFechaFormateada(c.getFechaHoraCompra().format(formatter));
            dto.setEstado(c.getEstadoRecepcion().name());
            dto.setCompraOriginal(c);
            cola.add(dto);
        }

        List<Transferencia> transferencias = transferenciaRepository.findByEstadoIn(
                List.of(EstadoTransferencia.EN_TRANSITO,
                        EstadoTransferencia.RECIBIDA_PARCIAL,
                        EstadoTransferencia.DESPACHADA_PARCIAL)
        );

        for (Transferencia t : transferencias) {
            RecepcionResumenDTO dto = new RecepcionResumenDTO();
            dto.setCodigo("TRF-" + t.getIdTransferencia());
            dto.setTipo("Transferencia");
            dto.setOrigen("Sucursal: " + t.getAlmacenOrigen().getNombre());
            dto.setFechaRaw(t.getFechaHoraSalidaProgramada());
            dto.setFechaFormateada(t.getFechaHoraSalidaProgramada().format(formatter));
            dto.setEstado(t.getEstado().name());
            dto.setTransferenciaOriginal(t);

            Despacho ultimoDespacho = despachoRepository.findFirstByTransferenciaOrderByFechaHoraSalidaProgramadaDesc(t);

            if (ultimoDespacho != null && ultimoDespacho.getTransporte() != null) {
                Transporte transporte = ultimoDespacho.getTransporte();

                if (transporte.getVehiculo() != null) transporte.getVehiculo().getPlaca();
                if (transporte.getConductor() != null) transporte.getConductor().getPersona().getNombre();
                if (transporte.getRuta() != null) transporte.getRuta().getNombre();

                dto.setTransporteDespacho(transporte);
            }

            cola.add(dto);
        }

        cola.sort((d1, d2) -> d1.getFechaRaw().compareTo(d2.getFechaRaw()));
        return cola;
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularCantidadPendiente(DetalleCompra dc) {
        BigDecimal totalSolicitado = dc.getCantidad();
        BigDecimal totalRecibido = detalleRecepcionRepository.sumCantidadProcesadaByDetalleCompra(dc);
        if (totalRecibido == null) {
            totalRecibido = BigDecimal.ZERO;
        }
        return totalSolicitado.subtract(totalRecibido);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularCantidadPendiente(DetalleTransferencia dt) {
        BigDecimal totalDespachado = detalleDespachoRepository.sumCantidadByIdDetalleTransferencia(dt.getIdDetalleTransferencia());
        if (totalDespachado == null) {
            totalDespachado = BigDecimal.ZERO;
        }

        BigDecimal totalRecibido = detalleRecepcionRepository.sumCantidadProcesadaByDetalleTransferencia(dt);
        if (totalRecibido == null) {
            totalRecibido = BigDecimal.ZERO;
        }

        return totalDespachado.subtract(totalRecibido);
    }

    @Transactional(readOnly = true)
    public List<RecepcionItemUI> obtenerItemsPendientes(String tipoDocumento, Long idDocumento) {
        List<RecepcionItemUI> itemsFisicos = new ArrayList<>();

        if ("Compra".equals(tipoDocumento)) {
            Compra compra = compraRepository.findById(idDocumento)
                    .orElseThrow(() -> new RuntimeException("Compra no encontrada"));

            for (DetalleCompra dc : compra.getDetalles()) {
                dc.getProducto().getNombre();

                BigDecimal pendiente = calcularCantidadPendiente(dc);
                if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
                    itemsFisicos.add(new RecepcionItemUI(dc, pendiente));
                }
            }
        } else {
            Transferencia transferencia = transferenciaRepository.findById(idDocumento)
                    .orElseThrow(() -> new RuntimeException("Transferencia no encontrada"));

            Almacen destinoOriginal = transferencia.getAlmacenDestino();
            destinoOriginal.getNombre();

            for (DetalleTransferencia dt : transferencia.getDetalles()) {
                dt.getLote().getNumeroLote();
                dt.getLote().getProducto().getNombre();

                BigDecimal pendiente = calcularCantidadPendiente(dt);
                if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
                    RecepcionItemUI item = new RecepcionItemUI(dt, pendiente);
                    item.setAlmacenDestino(destinoOriginal);
                    itemsFisicos.add(item);
                }
            }
        }

        return itemsFisicos;
    }

    @Transactional(readOnly = true)
    public List<RecepcionResumenDTO> obtenerHistorialRecepciones() {
        List<RecepcionResumenDTO> historial = new ArrayList<>();

        List<Recepcion> recepcionesCompletadas = recepcionRepository.findAll();

        for (Recepcion r : recepcionesCompletadas) {
            RecepcionResumenDTO dto = new RecepcionResumenDTO();
            dto.setIdRecepcion(r.getIdRecepcion());
            dto.setCodigo("REC-" + r.getIdRecepcion());
            dto.setFechaRaw(r.getFechaHoraRecepcion());
            dto.setFechaFormateada(r.getFechaHoraRecepcion().format(formatter));
            dto.setEstado("COMPLETADA");

            if (r.getTransferencia() != null) {
                dto.setTipo("Transferencia");
                dto.setOrigen("Sucursal: " + r.getTransferencia().getAlmacenOrigen().getNombre());
                dto.setTransferenciaOriginal(r.getTransferencia());
            }
            else if (r.getCompras() != null && !r.getCompras().isEmpty()) {
                Compra compra = r.getCompras().iterator().next();
                dto.setTipo("Compra");
                dto.setOrigen(compra.getProveedor().getNombre());
                dto.setCompraOriginal(compra);
            }
            else {
                dto.setTipo("Desconocido");
                dto.setOrigen("-");
            }

            historial.add(dto);
        }

        historial.sort((d1, d2) -> {
            if (d1.getFechaRaw() == null && d2.getFechaRaw() == null) return 0;
            if (d1.getFechaRaw() == null) return 1;
            if (d2.getFechaRaw() == null) return -1;
            return d2.getFechaRaw().compareTo(d1.getFechaRaw());
        });

        return historial;
    }

    @Transactional(readOnly = true)
    public Recepcion obtenerRecepcionConDetalles(Long idRecepcion) {
        if (idRecepcion == null) {
            return null;
        }
        return recepcionRepository.findRecepcionConDetallesByIdRecepcion(idRecepcion).orElse(null);
    }
}
