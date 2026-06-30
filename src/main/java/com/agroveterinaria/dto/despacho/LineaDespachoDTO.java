package com.agroveterinaria.dto.despacho;

import com.agroveterinaria.entity.DetalleTransferencia;
import com.agroveterinaria.entity.DetalleVenta;
import com.agroveterinaria.entity.Lote;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LineaDespachoDTO {
    private DetalleTransferencia detalleTransferencia;
    private DetalleVenta detalleVenta;

    private String nombreProducto;
    private String numeroLote;
    private BigDecimal cantidadSolicitada;
    private BigDecimal cantidadYaDespachada;
    private BigDecimal cantidadPendiente;
    private BigDecimal cantidadADespacharActual = BigDecimal.ZERO;
    private Lote loteSeleccionadoFisicamente;

    public LineaDespachoDTO(DetalleTransferencia dt, BigDecimal yaDespachada) {
        this.detalleTransferencia = dt;
        this.nombreProducto = dt.getLote().getProducto().getNombre();
        this.numeroLote = dt.getLote().getNumeroLote();
        this.cantidadSolicitada = dt.getCantidad();
        this.cantidadYaDespachada = yaDespachada != null ? yaDespachada : BigDecimal.ZERO;
        this.cantidadPendiente = this.cantidadSolicitada.subtract(this.cantidadYaDespachada);
        this.cantidadADespacharActual = this.cantidadPendiente;
    }

    public LineaDespachoDTO(DetalleVenta dv, BigDecimal yaDespachada) {
        this.detalleVenta = dv;
        this.nombreProducto = dv.getProducto().getNombre();

        this.numeroLote = dv.getLote() != null ? dv.getLote().getNumeroLote() : "Asignado en picking";

        this.cantidadSolicitada = dv.getCantidad();
        this.cantidadYaDespachada = yaDespachada != null ? yaDespachada : BigDecimal.ZERO;
        this.cantidadPendiente = this.cantidadSolicitada.subtract(this.cantidadYaDespachada);
        this.cantidadADespacharActual = this.cantidadPendiente;
    }
}