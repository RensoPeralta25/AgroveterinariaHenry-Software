package com.agroveterinaria.service;

import com.agroveterinaria.dto.venta.LineaFacturaVentaPdfDTO;
import com.agroveterinaria.entity.DetalleVenta;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.EstrategiaPrecioVenta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaVentaPdfMapperTest {

    private final FacturaVentaPdfMapper mapper = new FacturaVentaPdfMapper();

    @Test
    void traduceCantidadMixtaYMuestraPrecioDeEmpaque() {
        Producto producto = productoFraccionable("Alimento", "12", "100.00", "10.00");
        DetalleVenta detalle = detalle(producto, "17", "8.823529", "0");

        LineaFacturaVentaPdfDTO linea = mapear(detalle);

        assertEquals("1 Caja, 5 Unids", linea.cantidad());
        assertEquals(new BigDecimal("100.00"), linea.precioUnitario());
        assertEquals(new BigDecimal("150.00"), linea.subtotal());
    }

    @Test
    void muestraPrecioDeFraccionCuandoTodaLaCantidadSeCobraSuelta() {
        Producto producto = productoFraccionable("Tabletas", "12", "100.00", "10.00");
        DetalleVenta detalle = detalle(producto, "5", "10.000000", "0");

        LineaFacturaVentaPdfDTO linea = mapear(detalle);

        assertEquals("5 Unids", linea.cantidad());
        assertEquals(new BigDecimal("10.00"), linea.precioUnitario());
        assertEquals(new BigDecimal("50.00"), linea.subtotal());
    }

    @Test
    void muestraPrecioCompletoCuandoSeUsaPrecioProporcionalDeEmpaque() {
        Producto producto = productoFraccionable("Tabletas", "12", "100.00", "10.00");
        DetalleVenta detalle = detalle(producto, "5", "8.333333", "0");

        LineaFacturaVentaPdfDTO linea = mapear(detalle);

        assertEquals("5 Unids", linea.cantidad());
        assertEquals(new BigDecimal("100.00"), linea.precioUnitario());
        assertEquals(new BigDecimal("41.67"), linea.subtotal());
    }

    @Test
    void conservaReferenciaDeEmpaqueSiLaVentaMixtaFueDivididaEntreLotes() {
        Producto producto = productoFraccionable("Alimento", "12", "100.00", "10.00");
        DetalleVenta fragmentoDeLote = detalle(producto, "5", "8.823529", "0");

        LineaFacturaVentaPdfDTO linea = mapear(fragmentoDeLote);

        assertEquals("5 Unids", linea.cantidad());
        assertEquals(new BigDecimal("100.00"), linea.precioUnitario());
    }

    @Test
    void conservaFormatoSimpleYRedondeaImportesParaProductoNoFraccionable() {
        Producto producto = new Producto();
        producto.setNombre("Collar");
        producto.setPermiteFraccionamiento(false);
        producto.setPrecioEmpaque(new BigDecimal("25.00"));
        DetalleVenta detalle = detalle(producto, "2", "12.345678", "1.236");

        LineaFacturaVentaPdfDTO linea = mapear(detalle);

        assertEquals("2 Cajas", linea.cantidad());
        assertEquals(new BigDecimal("12.35"), linea.precioUnitario());
        assertEquals(new BigDecimal("1.24"), linea.impuesto());
        assertEquals(new BigDecimal("25.93"), linea.subtotal());
    }

    @Test
    void subtotalMasImpuestosMasAjustesCoincideConTotalVisible() {
        Producto producto = productoFraccionable("Tabletas", "12", "100.00", "10.00");
        DetalleVenta detalle = detalle(producto, "5", "10.000000", "9.005");
        Venta venta = new Venta();
        venta.setDetallesVentas(List.of(detalle));
        venta.setMontoTotal(new BigDecimal("62.00"));

        var factura = mapper.toDto(venta, BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(new BigDecimal("50.00"), factura.subtotal());
        assertEquals(new BigDecimal("9.01"), factura.impuestos());
        assertEquals(new BigDecimal("2.99"), factura.ajustes());
        assertEquals(factura.montoTotal(),
                factura.subtotal().add(factura.impuestos()).add(factura.ajustes()));
    }

    @Test
    void usaPreciosHistoricosYEstrategiaParaDesglosarLaLinea() {
        Producto producto = productoFraccionable("Alimento", "12", "120.00", "12.00");
        DetalleVenta detalle = detalle(producto, "17", "8.823529", "0");
        detalle.setEstrategiaPrecio(EstrategiaPrecioVenta.NORMAL);
        detalle.setPrecioEmpaqueHistorico(new BigDecimal("100.00"));
        detalle.setPrecioFraccionHistorico(new BigDecimal("10.00"));

        LineaFacturaVentaPdfDTO linea = mapear(detalle);
        String desglose = linea.desglosePrecios();

        assertEquals(new BigDecimal("100.00"), linea.precioUnitario());
        assertTrue(desglose.contains("1 Caja"));
        assertTrue(desglose.contains("5 Unids"));
        assertTrue(desglose.contains("100"));
        assertTrue(desglose.contains("10"));
    }

    @Test
    void describeCondicionesDeCreditoYCostoDeEnvio() {
        Venta venta = new Venta();
        venta.setDetallesVentas(List.of());
        venta.setMontoTotal(new BigDecimal("125.00"));
        venta.setCostoEnvio(new BigDecimal("25.00"));
        venta.setFechaVencimientoPago(LocalDateTime.of(2026, 8, 15, 0, 0));

        var factura = mapper.toDto(venta, BigDecimal.ZERO, new BigDecimal("125.00"));

        assertEquals("Crédito - vence el 15/08/2026", factura.condicionesCredito());
        assertEquals(new BigDecimal("25.00"), factura.costoEnvio());
    }

    private LineaFacturaVentaPdfDTO mapear(DetalleVenta detalle) {
        Venta venta = new Venta();
        venta.setDetallesVentas(List.of(detalle));
        return mapper.toDto(venta, BigDecimal.ZERO, BigDecimal.ZERO).lineas().getFirst();
    }

    private Producto productoFraccionable(String nombre, String factor, String precioEmpaque, String precioFraccion) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPermiteFraccionamiento(true);
        producto.setContenidoPorEmpaque(new BigDecimal(factor));
        producto.setPrecioEmpaque(new BigDecimal(precioEmpaque));
        producto.setPrecioFraccion(new BigDecimal(precioFraccion));
        return producto;
    }

    private DetalleVenta detalle(Producto producto, String cantidad, String precio, String impuesto) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(new BigDecimal(cantidad));
        detalle.setPrecioUnitarioVenta(new BigDecimal(precio));
        detalle.setImpuesto(new BigDecimal(impuesto));
        return detalle;
    }
}
