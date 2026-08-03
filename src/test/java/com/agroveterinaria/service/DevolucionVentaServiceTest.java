package com.agroveterinaria.service;

import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.DetalleDevVenta;
import com.agroveterinaria.entity.DetalleVenta;
import com.agroveterinaria.entity.DevolucionVenta;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.NotaDeCredito;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevolucionVentaServiceTest {

    @Mock
    private DevolucionVentaRepository devolucionRepository;
    @Mock
    private DetalleDevVentaRepository detalleRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private NotaDeCreditoRepository notaRepository;
    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private DetalleDespachoRepository detalleDespachoRepository;

    @Test
    void devolverProductoReingresaInventarioYEmiteCreditoConImpuestoIncluido() {
        DevolucionVentaService service = new DevolucionVentaService(
                devolucionRepository,
                detalleRepository,
                detalleDespachoRepository,
                inventarioService,
                notaRepository,
                cobroRepository
        );

        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setCostoEnvio(BigDecimal.ZERO);
        venta.setMontoTotal(new BigDecimal("236.00"));

        Producto producto = new Producto();
        producto.setNombre("Alimento");
        DetalleVenta detalleVenta = new DetalleVenta();
        detalleVenta.setIdDetalleVenta(10L);
        detalleVenta.setProducto(producto);
        detalleVenta.setCantidad(new BigDecimal("2.0000"));
        detalleVenta.setPrecioUnitarioVenta(new BigDecimal("100.000000"));
        detalleVenta.setImpuesto(new BigDecimal("36.0000"));
        venta.agregarDetalle(detalleVenta);

        Almacen almacen = new Almacen();
        almacen.setIdAlmacen(3L);
        Lote lote = new Lote();
        lote.setIdLote(4L);
        detalleVenta.setLote(lote);

        DetalleDevVenta detalleDevuelto = new DetalleDevVenta();
        detalleDevuelto.setDetalleVenta(detalleVenta);
        detalleDevuelto.setCantidadDevuelta(new BigDecimal("1.0000"));
        detalleDevuelto.setAlmacenEntrada(almacen);
        detalleDevuelto.setLote(lote);

        DevolucionVenta devolucion = new DevolucionVenta();
        devolucion.setCliente(cliente);
        devolucion.setEmpleado(new Empleado());
        devolucion.setRazonDevolucion("Cambio de producto");
        devolucion.agregarDetalle(detalleDevuelto);

        when(detalleRepository.sumarCantidadesDevueltasPorDetalleVentaAndLote(10L, 4L))
                .thenReturn(BigDecimal.ZERO);
        when(notaRepository.save(any(NotaDeCredito.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(devolucionRepository.save(any(DevolucionVenta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.registrarDevolucion(devolucion, true);

        verify(inventarioService).sumarStock(almacen, lote, new BigDecimal("1.0000"));
        ArgumentCaptor<NotaDeCredito> notaCaptor = ArgumentCaptor.forClass(NotaDeCredito.class);
        verify(notaRepository).save(notaCaptor.capture());
        NotaDeCredito nota = notaCaptor.getValue();
        assertEquals(new BigDecimal("118.00"), nota.getMonto());
        assertEquals(new BigDecimal("118.00"), nota.getSaldoDisponible());
        assertSame(nota, devolucion.getNotaDeCredito());
        assertEquals(new BigDecimal("118.00"), devolucion.getMontoTotal());
    }
}
