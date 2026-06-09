package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoRecepcion;
import com.agroveterinaria.enums.EstadoTransporte;
import com.agroveterinaria.enums.TipoGasto;
import com.agroveterinaria.repository.*;
import com.agroveterinaria.dto.recepcion.GastoOperativoUI;
import com.agroveterinaria.dto.recepcion.RecepcionItemUI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public RecepcionService(RecepcionRepository recepcionRepository, CompraRepository compraRepository,
                            LoteRepository loteRepository, InventarioRepository inventarioRepository,
                            TransporteRepository transporteRepository, GastoOperativoRepository gastoOperativoRepository,
                            ProductoRepository productoRepository, DetalleRecepcionRepository detalleRecepcionRepository) {
        this.recepcionRepository = recepcionRepository;
        this.compraRepository = compraRepository;
        this.loteRepository = loteRepository;
        this.inventarioRepository = inventarioRepository;
        this.transporteRepository = transporteRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
        this.productoRepository = productoRepository;
        this.detalleRecepcionRepository = detalleRecepcionRepository;
    }

    @Transactional
    public void procesarRecepcionTransaccional(
            Long idCompra,
            List<RecepcionItemUI> itemsFisicos,
            String tipoLogistica,
            BigDecimal costoFleteExterno,
            Vehiculo vehiculo,
            Empleado conductor,
            Ruta ruta,
            List<GastoOperativoUI> gastosInternos) {

        Compra compra = compraRepository.findById(idCompra)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada con ID: " + idCompra));

        Recepcion recepcion = new Recepcion();
        recepcion.setFechaHoraLlegadaProgramada(LocalDateTime.now());
        recepcion.setFechaHoraRecepcion(LocalDateTime.now());
        recepcion.getCompras().add(compra);

        Transporte transporteGuardado = null;

        if ("Transporte Interno (Vehículo propio)".equals(tipoLogistica) || "Mixto (Flete parcial + Transporte propio)".equals(tipoLogistica)) {
            if (vehiculo == null || conductor == null || ruta == null) {
                throw new IllegalArgumentException("Para transporte propio o mixto debe especificar Vehículo, Conductor y Ruta.");
            }

            Transporte transporte = new Transporte();
            transporte.setVehiculo(vehiculo);
            transporte.setConductor(conductor);
            transporte.setRuta(ruta);
            transporte.setFechaHoraSalida(LocalDateTime.now().minusHours(2));
            transporte.setFechaHoraLlegada(LocalDateTime.now());
            transporte.setEstado(EstadoTransporte.COMPLETADO);

            for (GastoOperativoUI ui : gastosInternos) {
                if (ui.getMonto() != null && ui.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                    GastoOperativo go = new GastoOperativo();
                    go.setTipoGasto(TipoGasto.VARIABLE);
                    go.setFecha(LocalDate.now());
                    go.setMonto(ui.getMonto());
                    go.setNotas(ui.getNotas() != null ? ui.getNotas().trim() : "Gasto de transporte interno");
                    GastoOperativo goGuardado = gastoOperativoRepository.save(go);
                    transporte.addGasto(goGuardado);
                }
            }

            if ("Mixto (Flete parcial + Transporte propio)".equals(tipoLogistica) && costoFleteExterno != null && costoFleteExterno.compareTo(BigDecimal.ZERO) > 0) {
                GastoOperativo goFlete = new GastoOperativo();
                goFlete.setTipoGasto(TipoGasto.VARIABLE);
                goFlete.setFecha(LocalDate.now());
                goFlete.setMonto(costoFleteExterno);
                goFlete.setNotas("Pago de flete externo hasta punto de encuentro");
                GastoOperativo goFleteGuardado = gastoOperativoRepository.save(goFlete);
                transporte.addGasto(goFleteGuardado);
            }

            transporteGuardado = transporteRepository.save(transporte);
            recepcion.setTransporte(transporteGuardado);
        }
        else if ("Flete / Delivery Externo".equals(tipoLogistica)) {
            if (costoFleteExterno != null && costoFleteExterno.compareTo(BigDecimal.ZERO) > 0) {
                GastoOperativo goFletePuro = new GastoOperativo();
                goFletePuro.setTipoGasto(TipoGasto.VARIABLE);
                goFletePuro.setFecha(LocalDate.now());
                goFletePuro.setMonto(costoFleteExterno);
                goFletePuro.setNotas("Servicio de Flete / Delivery Externo del Proveedor");
                GastoOperativo savedGasto = gastoOperativoRepository.save(goFletePuro);

                compra.setGastoAsociado(savedGasto);
            }
        }

        for (RecepcionItemUI item : itemsFisicos) {
            if (item.getCantidadRecibida() == null || item.getCantidadRecibida().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (item.getAlmacenDestino() == null) {
                throw new IllegalArgumentException("Debe especificar un Almacén Destino para el producto: " + item.getDetalle().getProducto().getNombre());
            }
            BigDecimal recibidoAnteriormente = detalleRecepcionRepository.sumCantidadRecibidaByDetalleCompra(item.getDetalle());
            if (recibidoAnteriormente == null) recibidoAnteriormente = BigDecimal.ZERO;
            BigDecimal pendienteReal = item.getDetalle().getCantidad().subtract(recibidoAnteriormente);
            if (item.getCantidadRecibida().compareTo(pendienteReal) > 0) {
                throw new IllegalArgumentException("Intento de sobre-recepción bloqueado. Solo quedan "
                        + pendienteReal + " unidades pendientes de " + item.getDetalle().getProducto().getNombre());
            }

            Lote lote = null;
            String numLoteStr = item.getNumeroLote() != null ? item.getNumeroLote().trim() : "";

            if (!numLoteStr.isEmpty()) {
                lote = loteRepository.findByProducto(item.getDetalle().getProducto()).stream()
                        .filter(l -> numLoteStr.equalsIgnoreCase(l.getNumeroLote()))
                        .findFirst().orElse(null);
            }

            if (lote == null) {
                lote = new Lote();
                lote.setProducto(item.getDetalle().getProducto());
                lote.setNumeroLote(numLoteStr.isEmpty() ? "GENERICO-" + LocalDate.now() : numLoteStr);
                lote.setFechaVencimiento(item.getFechaVencimiento());
                lote = loteRepository.save(lote);
            }

            Inventario inventario = inventarioRepository.findByAlmacenAndLote(item.getAlmacenDestino(), lote)
                    .orElse(null);

            if (inventario == null) {
                inventario = new Inventario();
                inventario.setAlmacen(item.getAlmacenDestino());
                inventario.setLote(lote);
                inventario.setCantidadActual(item.getCantidadRecibida());
            } else {
                inventario.setCantidadActual(inventario.getCantidadActual().add(item.getCantidadRecibida()));
            }
            inventarioRepository.save(inventario);

            DetalleRecepcion dr = new DetalleRecepcion();
            dr.setDetalleCompra(item.getDetalle());
            dr.setAlmacen(item.getAlmacenDestino());
            dr.setLote(lote);
            dr.setCantidad(item.getCantidadRecibida());
            recepcion.addDetalle(dr);
        }

        recepcionRepository.save(recepcion);

        boolean todoCompletado = true;
        List<DetalleCompra> todosLosDetalles = compra.getDetalles();

        for (DetalleCompra dc : todosLosDetalles) {
            BigDecimal recibidoAnteriormente = dc.getCompra().getDetalles().stream()
                    .flatMap(d -> recepcionRepository.findAll().stream())
                    .flatMap(r -> r.getDetalles().stream())
                    .filter(dr -> dr.getDetalleCompra().getIdDetalleCompra().equals(dc.getIdDetalleCompra()))
                    .map(DetalleRecepcion::getCantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (recibidoAnteriormente.compareTo(dc.getCantidad()) < 0) {
                todoCompletado = false;
                break;
            }
        }

        compra.setEstadoRecepcion(todoCompletado ? EstadoRecepcion.RECIBIDA : EstadoRecepcion.PARCIAL);
        compraRepository.save(compra);
    }
}