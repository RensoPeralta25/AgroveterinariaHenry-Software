package com.agroveterinaria.service;

import com.agroveterinaria.dto.venta.FacturaVentaPdfDTO;
import com.agroveterinaria.dto.venta.LineaFacturaVentaPdfDTO;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.DetalleVenta;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.EstrategiaPrecioVenta;
import com.agroveterinaria.util.FormatoInventarioUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FacturaVentaPdfMapper {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "DO"));

    public FacturaVentaPdfDTO toDto(Venta venta, BigDecimal montoCobrado, BigDecimal balancePendiente) {
        Cliente cliente = venta.getCliente();
        Persona personaCliente = cliente != null ? cliente.getPersona() : null;
        Empleado vendedor = venta.getVendedor();
        Persona personaVendedor = vendedor != null ? vendedor.getPersona() : null;

        List<LineaFacturaVentaPdfDTO> lineas = venta.getDetallesVentas().stream()
                .map(this::toLineaDto)
                .toList();
        BigDecimal impuestos = lineas.stream()
                .map(LineaFacturaVentaPdfDTO::impuesto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = lineas.stream()
                .map(linea -> linea.subtotal().subtract(linea.impuesto()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = redondearMoneda(venta.getMontoTotal());
        BigDecimal ajustes = total.subtract(subtotal).subtract(impuestos)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal costoEnvio = venta.getCostoEnvio();
        BigDecimal descuento = ajustes.subtract(costoEnvio);

        return new FacturaVentaPdfDTO(
                venta.getIdVenta(),
                venta.getFechaHoraVenta(),
                valorOrDefault(personaCliente != null ? personaCliente.getNombre() : null, "Cliente sin nombre"),
                valorOrDefault(personaCliente != null ? personaCliente.getCedula() : null, "Sin cedula"),
                valorOrDefault(personaCliente != null ? personaCliente.getTelefono() : null, "Sin telefono"),
                valorOrDefault(personaCliente != null ? personaCliente.getDireccion() : null, "Sin direccion"),
                valorOrDefault(personaVendedor != null ? personaVendedor.getNombre() : null, "Sin vendedor"),
                valorOrDefault(venta.getComprobanteFiscal(), "Sin comprobante"),
                venta.getEstado() != null ? venta.getEstado().getEtiqueta() : "",
                Boolean.TRUE.equals(venta.getLlevaDespacho()),
                subtotal,
                impuestos,
                ajustes,
                total,
                montoSeguro(montoCobrado),
                montoSeguro(balancePendiente),
                lineas,
                montoSeguro(descuento),
                montoSeguro(costoEnvio)
        );
    }

    private LineaFacturaVentaPdfDTO toLineaDto(DetalleVenta detalle) {
        Producto producto = detalle.getProducto();
        return new LineaFacturaVentaPdfDTO(
                valorOrDefault(producto != null ? producto.getNombre() : null, "Producto sin nombre"),
                formatearCantidad(detalle, producto),
                redondearMoneda(detalle.getPrecioUnitarioVenta()),
                redondearMoneda(detalle.getImpuesto()),
                redondearMoneda(detalle.calcularSubtotal()),
                generarDesglosePreciosHistorico(detalle, producto)
        );
    }

    private String generarDesglosePreciosHistorico(DetalleVenta detalle, Producto producto) {
        BigDecimal cantidad = montoSeguro(detalle.getCantidad());
        BigDecimal precioMezclado = montoSeguro(detalle.getPrecioUnitarioVenta());

        EstrategiaPrecioVenta estrategia = detalle.getEstrategiaPrecio();
        BigDecimal precioEmpaqueHist = detalle.getPrecioEmpaqueHistorico();
        BigDecimal precioFraccionHist = detalle.getPrecioFraccionHistorico();

        if (estrategia == null || precioEmpaqueHist == null || producto == null || !Boolean.TRUE.equals(producto.getPermiteFraccionamiento())) {
            return FormatoInventarioUtil.formatearCantidad(cantidad, null, false, false) + " x " + formatMoney(precioMezclado);
        }

        BigDecimal factor = producto.getContenidoPorEmpaque() != null ? producto.getContenidoPorEmpaque() : BigDecimal.ONE;

        if (estrategia == EstrategiaPrecioVenta.TODO_PRECIO_EMPAQUE) {
            return FormatoInventarioUtil.formatearCantidad(cantidad, factor, true, false) + " x " + formatMoney(precioEmpaqueHist);
        }

        if (estrategia == EstrategiaPrecioVenta.TODO_PRECIO_FRACCION) {
            BigDecimal fraccionAUsar = precioFraccionHist != null ? precioFraccionHist : precioEmpaqueHist.divide(factor, 4, RoundingMode.HALF_UP);
            return FormatoInventarioUtil.formatearCantidad(cantidad, factor, true, false) + " x " + formatMoney(fraccionAUsar);
        }

        BigDecimal[] division = cantidad.divideAndRemainder(factor);
        BigDecimal cajas = division[0];
        BigDecimal unidades = division[1];
        BigDecimal fraccionAUsar = precioFraccionHist != null ? precioFraccionHist : precioEmpaqueHist.divide(factor, 4, RoundingMode.HALF_UP);

        List<String> partes = new java.util.ArrayList<>();
        if (cajas.compareTo(BigDecimal.ZERO) > 0) {
            partes.add(cajas.stripTrailingZeros().toPlainString() + " Cajas x " + formatMoney(precioEmpaqueHist));
        }
        if (unidades.compareTo(BigDecimal.ZERO) > 0) {
            partes.add(unidades.stripTrailingZeros().toPlainString() + " Unids x " + formatMoney(fraccionAUsar));
        }

        return String.join(" + ", partes);
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value != null ? value : BigDecimal.ZERO);
    }

    private String formatearCantidad(DetalleVenta detalle, Producto producto) {
        BigDecimal cantidad = montoSeguro(detalle.getCantidad());
        if (producto == null) {
            return FormatoInventarioUtil.formatearCantidad(cantidad, null, false, false);
        }
        return FormatoInventarioUtil.formatearCantidad(
                cantidad,
                producto.getContenidoPorEmpaque(),
                Boolean.TRUE.equals(producto.getPermiteFraccionamiento()),
                false
        );
    }

    private BigDecimal precioComercial(DetalleVenta detalle, Producto producto) {
        BigDecimal precioCalculado = montoSeguro(detalle.getPrecioUnitarioVenta());
        if (producto == null || !Boolean.TRUE.equals(producto.getPermiteFraccionamiento())) {
            return redondearMoneda(precioCalculado);
        }

        BigDecimal factor = producto.getContenidoPorEmpaque();
        BigDecimal precioEmpaque = producto.getPrecioEmpaque();
        if (factor == null || factor.compareTo(BigDecimal.ONE) <= 0 || precioEmpaque == null) {
            return redondearMoneda(precioCalculado);
        }

        BigDecimal precioFraccion = producto.getPrecioFraccion() != null
                ? producto.getPrecioFraccion()
                : precioEmpaque.divide(factor, 4, RoundingMode.HALF_UP);
        BigDecimal precioEmpaqueProporcional = precioEmpaque.divide(factor, 6, RoundingMode.HALF_UP);

        if (mismoPrecio(precioCalculado, precioFraccion)) {
            return redondearMoneda(precioFraccion);
        }
        if (mismoPrecio(precioCalculado, precioEmpaqueProporcional)) {
            return redondearMoneda(precioEmpaque);
        }

        // En la estrategia NORMAL, un precio promedio distinto al de la fracción
        // indica que la venta original incluyó al menos un empaque. Esto también
        // funciona cuando una misma línea fue dividida entre varios lotes.
        return redondearMoneda(precioEmpaque);
    }

    private boolean mismoPrecio(BigDecimal primero, BigDecimal segundo) {
        return primero.setScale(6, RoundingMode.HALF_UP)
                .compareTo(segundo.setScale(6, RoundingMode.HALF_UP)) == 0;
    }

    private BigDecimal redondearMoneda(BigDecimal value) {
        return montoSeguro(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal montoSeguro(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String valorOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
