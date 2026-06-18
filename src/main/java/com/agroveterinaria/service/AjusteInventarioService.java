package com.agroveterinaria.service;

import com.agroveterinaria.entity.AjusteInventario;
import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Inventario;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.enums.TipoAjuste;
import com.agroveterinaria.repository.AjusteInventarioRepository;
import com.agroveterinaria.repository.InventarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AjusteInventarioService {

    private final AjusteInventarioRepository ajusteRepository;
    private final InventarioRepository inventarioRepository;

    public AjusteInventarioService(AjusteInventarioRepository ajusteRepository, InventarioRepository inventarioRepository) {
        this.ajusteRepository = ajusteRepository;
        this.inventarioRepository = inventarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AjusteInventario> listarHistorial() {
        List<AjusteInventario> historial = ajusteRepository.findAll();
        for (AjusteInventario ajuste : historial) {
            ajuste.getAlmacen().getNombre();
            ajuste.getLote().getNumeroLote();
            ajuste.getLote().getProducto().getNombre();
            ajuste.getEmpleado().getPersona().getNombre();
        }
        historial.sort((a1, a2) -> a2.getFechaHora().compareTo(a1.getFechaHora()));
        return historial;
    }

    @Transactional
    public void registrarAjusteManual(Almacen almacen, Lote lote, TipoAjuste tipo,
                                      BigDecimal cantidad, String justificacion, Empleado empleadoLogueado) {

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad a ajustar debe ser mayor a cero.");
        }

        Inventario inventario = inventarioRepository.findByAlmacenAndLote(almacen, lote).orElse(null);

        if (tipo == TipoAjuste.SALIDA) {
            if (inventario == null || inventario.getCantidadActual().compareTo(cantidad) < 0) {
                throw new IllegalArgumentException("No hay suficiente stock en este almacén/lote para retirar " + cantidad + " unidades.");
            }
            inventario.setCantidadActual(inventario.getCantidadActual().subtract(cantidad));
        } else {
            if (inventario == null) {
                inventario = new Inventario();
                inventario.setAlmacen(almacen);
                inventario.setLote(lote);
                inventario.setCantidadActual(cantidad);
            } else {
                inventario.setCantidadActual(inventario.getCantidadActual().add(cantidad));
            }
        }

        inventarioRepository.save(inventario);

        AjusteInventario ajuste = new AjusteInventario();
        ajuste.setAlmacen(almacen);
        ajuste.setLote(lote);
        ajuste.setEmpleado(empleadoLogueado);
        ajuste.setTipoAjuste(tipo);
        ajuste.setCantidad(cantidad);
        ajuste.setJustificacion(justificacion.trim());
        ajuste.setFechaHora(LocalDateTime.now());

        ajusteRepository.save(ajuste);
    }
}