package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoTransferencia;
import com.agroveterinaria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransferenciaService {

    private final TransferenciaRepository transferenciaRepository;
    private final InventarioRepository inventarioRepository;
    private final TransporteRepository transporteRepository;
    private final GastoOperativoRepository gastoOperativoRepository;

    public TransferenciaService(TransferenciaRepository transferenciaRepository,
                                InventarioRepository inventarioRepository,
                                TransporteRepository transporteRepository,
                                GastoOperativoRepository gastoOperativoRepository) {
        this.transferenciaRepository = transferenciaRepository;
        this.inventarioRepository = inventarioRepository;
        this.transporteRepository = transporteRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
    }

    @Transactional
    public Transferencia registrarTransferencia(Transferencia transferencia, GastoOperativo gasto, boolean esInmediato) {
        for (DetalleTransferencia detalle : transferencia.getDetalles()) {
            Inventario invOrigen = inventarioRepository.findByAlmacenAndLote(transferencia.getAlmacenOrigen(), detalle.getLote())
                    .orElseThrow(() -> new RuntimeException("Lote no encontrado en el almacén de origen"));

            if (invOrigen.getCantidadActual().compareTo(detalle.getCantidad()) < 0) {
                throw new RuntimeException("La cantidad a transferir supera la existencia actual en origen para el producto: "
                        + detalle.getLote().getProducto().getNombre());
            }

            if (esInmediato) {
                invOrigen.setCantidadActual(invOrigen.getCantidadActual().subtract(detalle.getCantidad()));
                inventarioRepository.save(invOrigen);

                Inventario invDestino = inventarioRepository.findByAlmacenAndLote(transferencia.getAlmacenDestino(), detalle.getLote())
                        .orElse(null);

                if (invDestino == null) {
                    invDestino = new Inventario();
                    invDestino.setAlmacen(transferencia.getAlmacenDestino());
                    invDestino.setLote(detalle.getLote());
                    invDestino.setCantidadActual(detalle.getCantidad());
                } else {
                    invDestino.setCantidadActual(invDestino.getCantidadActual().add(detalle.getCantidad()));
                }
                inventarioRepository.save(invDestino);
            }
        }

        if (!esInmediato && transferencia.getTransporte() != null) {
            Transporte transporte = transferencia.getTransporte();
            transporte = transporteRepository.save(transporte);
            if (gasto != null) {
                gasto = gastoOperativoRepository.save(gasto);
                transporte.addGasto(gasto);
                transporte = transporteRepository.save(transporte);
            }
            transferencia.setTransporte(transporte);
        }

        if (transferencia.getFechaHoraSalidaProgramada() == null) {
            transferencia.setFechaHoraSalidaProgramada(LocalDateTime.now());
        }

        if (esInmediato) {
            transferencia.setEstado(EstadoTransferencia.COMPLETADA);
            transferencia.setFechaHoraLlegadaProgramada(LocalDateTime.now());
        } else {
            transferencia.setEstado(EstadoTransferencia.PENDIENTE_DESPACHO);
        }

        return transferenciaRepository.save(transferencia);
    }

    @Transactional(readOnly = true)
    public List<Transferencia> listarTodosConAlmacenes() {
        return transferenciaRepository.findAllConAlmacenes();
    }

    @Transactional(readOnly = true)
    public Transferencia obtenerTransferenciaConDetalles(Long id) {
        return transferenciaRepository.findByIdConDetalles(id)
                .orElseThrow(() -> new RuntimeException("Transferencia no encontrada"));
    }
}