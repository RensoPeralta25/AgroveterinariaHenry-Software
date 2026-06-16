package com.agroveterinaria.dto.despacho;

import com.agroveterinaria.entity.DetalleTransferencia;
import com.agroveterinaria.entity.DetalleVenta;
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

    public LineaDespachoDTO(DetalleTransferencia dt, BigDecimal yaDespachada) {
        this.detalleTransferencia = dt;
        this.nombreProducto = dt.getLote().getProducto().getNombre();
        this.numeroLote = dt.getLote().getNumeroLote();
        this.cantidadSolicitada = dt.getCantidad();
        this.cantidadYaDespachada = yaDespachada != null ? yaDespachada : BigDecimal.ZERO;
        this.cantidadPendiente = this.cantidadSolicitada.subtract(this.cantidadYaDespachada);

        this.cantidadADespacharActual = this.cantidadPendiente;
    }
}