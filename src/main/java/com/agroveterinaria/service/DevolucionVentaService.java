package com.agroveterinaria.service;

import com.agroveterinaria.entity.DetalleDevVenta;
import com.agroveterinaria.entity.DevolucionVenta;
import com.agroveterinaria.entity.NotaDeCredito;
import com.agroveterinaria.entity.NotaDeCredito;
import com.agroveterinaria.enums.EstadoDevolucion;
import com.agroveterinaria.repository.DetalleDevVentaRepository;
import com.agroveterinaria.repository.DevolucionVentaRepository;
import com.agroveterinaria.repository.NotaDeCreditoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class DevolucionVentaService {

    private final DevolucionVentaRepository devolucionRepository;
    private final DetalleDevVentaRepository detalleDevRepository;
    private final InventarioService inventarioService;
    private final NotaDeCreditoRepository notaCreditoRepository;

    public DevolucionVentaService(DevolucionVentaRepository devolucionRepository,
                                  DetalleDevVentaRepository detalleDevRepository,
                                  InventarioService inventarioService,
                                  NotaDeCreditoRepository notaCreditoRepository) {
        this.devolucionRepository = devolucionRepository;
        this.detalleDevRepository = detalleDevRepository;
        this.inventarioService = inventarioService;
        this.notaCreditoRepository = notaCreditoRepository;
    }


    @Transactional(rollbackFor = Exception.class)
    public DevolucionVenta registrarDevolucion(DevolucionVenta devolucion, boolean generarNotaCredito) {

        if (devolucion.getDetalles() == null || devolucion.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La devolución debe contener al menos un producto.");
        }

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
        }

        if (generarNotaCredito) {
            if (devolucion.getMontoTotal() == null || devolucion.getMontoTotal().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto de la devolución debe ser mayor a 0 para generar una Nota de Crédito.");
            }

            NotaDeCredito nc = new NotaDeCredito();
            nc.setCliente(devolucion.getCliente());
            nc.setMonto(devolucion.getMontoTotal());

            NotaDeCredito ncGuardada = notaCreditoRepository.save(nc);

            devolucion.setNotaDeCredito(ncGuardada);
        }

        devolucion.setEstado(EstadoDevolucion.COMPLETADA);
        devolucion.setFechaHora(LocalDateTime.now());

        return devolucionRepository.save(devolucion);
    }
}