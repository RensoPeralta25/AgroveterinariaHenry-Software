package com.agroveterinaria.dto.recepcion;

import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.DetalleCompra;
import com.agroveterinaria.entity.DetalleTransferencia;
import com.agroveterinaria.entity.Producto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecepcionItemUI {
    private DetalleCompra detalleCompra;
    private DetalleTransferencia detalleTransferencia;

    private Producto producto;
    private BigDecimal cantidadMaximaPermitida;
    private BigDecimal cantidadRecibida;
    private Almacen almacenDestino;
    private String numeroLote;
    private LocalDate fechaVencimiento;

    public RecepcionItemUI(DetalleCompra dc, BigDecimal maxPermitido) {
        this.detalleCompra = dc;
        this.producto = dc.getProducto();
        this.cantidadMaximaPermitida = maxPermitido;
        this.cantidadRecibida = maxPermitido;
    }

    public RecepcionItemUI(DetalleTransferencia dt, BigDecimal maxPermitido) {
        this.detalleTransferencia = dt;
        this.producto = dt.getLote().getProducto();
        this.cantidadMaximaPermitida = maxPermitido;
        this.cantidadRecibida = maxPermitido;
        this.numeroLote = dt.getLote().getNumeroLote();
        this.fechaVencimiento = dt.getLote().getFechaVencimiento();
    }
}