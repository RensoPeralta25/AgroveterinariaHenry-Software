package com.agroveterinaria.dto.devolucion;

import com.agroveterinaria.entity.DetalleVenta;
import com.agroveterinaria.entity.Lote;
import java.math.BigDecimal;

public class LineaElegibleDevolucionDTO {
    private final DetalleVenta detalleVenta;
    private final Lote lote;
    private final BigDecimal cantidadDisponible;

    public LineaElegibleDevolucionDTO(DetalleVenta detalleVenta, Lote lote, BigDecimal cantidadDisponible) {
        this.detalleVenta = detalleVenta;
        this.lote = lote;
        this.cantidadDisponible = cantidadDisponible;
    }

    public DetalleVenta getDetalleVenta() { return detalleVenta; }
    public Lote getLote() { return lote; }
    public BigDecimal getCantidadDisponible() { return cantidadDisponible; }
}