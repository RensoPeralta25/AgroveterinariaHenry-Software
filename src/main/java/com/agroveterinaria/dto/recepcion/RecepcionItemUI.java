package com.agroveterinaria.dto.recepcion;

import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.DetalleCompra;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecepcionItemUI {
    private DetalleCompra detalle;
    private Almacen almacenDestino;
    private String numeroLote;
    private LocalDate fechaVencimiento;
    private BigDecimal cantidadRecibida;
    private BigDecimal cantidadMaximaPermitida;

    public RecepcionItemUI(DetalleCompra detalle, BigDecimal cantidadMaximaPermitida) {
        this.detalle = detalle;
        this.cantidadMaximaPermitida = cantidadMaximaPermitida;
        this.cantidadRecibida = cantidadMaximaPermitida;
    }
}
