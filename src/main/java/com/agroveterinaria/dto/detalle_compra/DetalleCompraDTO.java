package com.agroveterinaria.dto.detalle_compra;

import com.agroveterinaria.entity.Producto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DetalleCompraDTO {

    private final Producto producto;
    private BigDecimal cantidad;
    private BigDecimal costoActual;

    public DetalleCompraDTO(Producto producto) {
        this.producto = producto;
        this.cantidad = BigDecimal.ONE;
        this.costoActual = BigDecimal.ZERO;
    }

    public BigDecimal getSubtotal() {
        if (cantidad == null || costoActual == null) return BigDecimal.ZERO;
        return cantidad.multiply(costoActual);
    }
}