package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.*;
import com.agroveterinaria.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TipoClienteRepository tipoClienteRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CobroRepository cobroRepository;

    @Mock
    private AlmacenRepository almacenRepository;

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock LoteRepository loteRepository;

    @Mock
    private NotaDeCreditoRepository notaDeCreditoRepository;

    @Mock
    private PersonaService personaService;

    private VentaService ventaService;
    private Cliente cliente;
    private Empleado vendedor;

    @BeforeEach
    void setUp() {
        ventaService = new VentaService(
                ventaRepository,
                clienteRepository,
                tipoClienteRepository,
                empleadoRepository,
                productoRepository,
                cobroRepository,
                personaService,
                almacenRepository,
                inventarioRepository,
                loteRepository,
                notaDeCreditoRepository
        );

        cliente = cliente(1L);
        vendedor = vendedor(10L);
    }

    @Test
    void calcularResumenRechazaCantidadCeroONegativa() {
        VentaService.SolicitudVenta ventaCantidadCero = solicitud(List.of(linea(100L, "0.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL)));
        VentaService.SolicitudVenta ventaCantidadNegativa = solicitud(List.of(linea(100L, "-1.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL)));

        assertThrows(IllegalArgumentException.class, () -> ventaService.calcularResumen(ventaCantidadCero));
        assertThrows(IllegalArgumentException.class, () -> ventaService.calcularResumen(ventaCantidadNegativa));
    }

    @Test
    void detalleVentaCalculaSuSubtotal() {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setCantidad(new BigDecimal("3.00"));
        detalle.setPrecioUnitarioVenta(new BigDecimal("125.50"));
        detalle.setImpuesto(new BigDecimal("67.7700"));

        assertEquals(new BigDecimal("444.27"), detalle.calcularSubtotal());
    }

    @Test
    void ventaReemplazaDetallesYAsignaRelacionPadre() {
        Venta venta = new Venta();
        DetalleVenta primerDetalle = new DetalleVenta();
        DetalleVenta segundoDetalle = new DetalleVenta();

        venta.reemplazarDetalles(List.of(primerDetalle, segundoDetalle));

        assertEquals(2, venta.getDetallesVentas().size());
        assertSame(venta, primerDetalle.getVenta());
        assertSame(venta, segundoDetalle.getVenta());
    }

    @Test
    void calcularResumenCalculaSubtotalDescuentoTotalYBalance() {
        Producto alimento = producto(100L, CategoriaProducto.ALIMENTO, "150.00", "18.00");
        when(productoRepository.findById(100L)).thenReturn(Optional.of(alimento));

        VentaService.ResumenVenta resumen = ventaService.calcularResumen(solicitud(
                "25.00",
                "200.00",
                List.of(linea(100L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL))
        ));

        assertEquals(new BigDecimal("354.00"), resumen.subtotal());
        assertEquals(new BigDecimal("25.00"), resumen.descuento());
        assertEquals(new BigDecimal("329.00"), resumen.total());
        assertEquals(new BigDecimal("200.00"), resumen.montoPagado());
        assertEquals(new BigDecimal("129.00"), resumen.balancePendiente());
        assertEquals(EstadoVenta.PENDIENTE, resumen.estado());
    }

    @Test
    void calcularResumenPermiteSobrescribirElImpuestoSugeridoDelProducto() {
        Producto alimento = producto(100L, CategoriaProducto.ALIMENTO, "150.00", "18.00");
        when(productoRepository.findById(100L)).thenReturn(Optional.of(alimento));

        VentaService.ResumenVenta resumen = ventaService.calcularResumen(solicitud(
                "0.00",
                "0.00",
                List.of(new VentaService.LineaVentaRequest(
                        100L,
                        bd("2.00"),
                        bd("10.00"),
                        1L,
                        1L,
                        EstrategiaPrecioVenta.NORMAL
                ))
        ));

        assertEquals(new BigDecimal("310.00"), resumen.subtotal());
    }

    @Test
    void efectivoYTransferenciaEstanHabilitadosComoMetodosDePago() {
        assertTrue(List.of(MetodoPago.values()).containsAll(List.of(
                MetodoPago.EFECTIVO,
                MetodoPago.TARJETA,
                MetodoPago.NOTA_CREDITO,
                MetodoPago.TRANSFERENCIA
        )));

        Venta venta = venta(cliente, "1000.00");

        when(cobroRepository.save(any(Cobro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cobro cobroTransferencia = ventaService.registrarCobro(
                cliente,
                venta,
                MetodoPago.TRANSFERENCIA,
                new BigDecimal("100.00"),
                transferencia("BHD-001")
        );

        assertEquals(MetodoPago.TRANSFERENCIA, cobroTransferencia.getMetodoPago());
        assertEquals("BHD-001", cobroTransferencia.getReferenciaTransferencia());
        assertEquals("Cliente prueba", cobroTransferencia.getTitularTransferencia());
        assertThrows(IllegalArgumentException.class, () ->
                ventaService.registrarCobro(cliente, venta, MetodoPago.TARJETA, new BigDecimal("100.00"))
        );
        assertThrows(IllegalArgumentException.class, () ->
                ventaService.registrarCobro(cliente, venta, MetodoPago.NOTA_CREDITO, new BigDecimal("100.00"))
        );
    }

    @Test
    void transferenciaExigeEvidenciaYConfirmacionDelCajero() {
        Venta venta = venta(cliente, "1000.00");

        IllegalArgumentException sinDatos = assertThrows(IllegalArgumentException.class, () ->
                ventaService.registrarCobro(
                        cliente,
                        venta,
                        MetodoPago.TRANSFERENCIA,
                        new BigDecimal("100.00")
                )
        );

        assertEquals("Debes completar los datos de la transferencia.", sinDatos.getMessage());
    }

    @Test
    void transferenciaRechazaUnaReferenciaBancariaReutilizada() {
        Venta venta = venta(cliente, "1000.00");
        when(cobroRepository.existsByReferenciaTransferenciaIgnoreCase("BHD-001")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ventaService.registrarCobro(
                        cliente,
                        venta,
                        MetodoPago.TRANSFERENCIA,
                        new BigDecimal("100.00"),
                        transferencia("BHD-001")
                )
        );

        assertEquals("La referencia bancaria ya fue utilizada en otro cobro.", exception.getMessage());
    }

    @Test
    void estadosDisponiblesDeVentaSonNegociacionPendienteYCerrada() {
        assertEquals(List.of(
                EstadoVenta.NEGOCIACION,
                EstadoVenta.PENDIENTE,
                EstadoVenta.CERRADA
        ), List.of(EstadoVenta.values()));
    }

    @Test
    void calcularResumenAsignaEstadoPendienteOCerradaSegunPago() {
        Producto alimento = producto(100L, CategoriaProducto.ALIMENTO, "150.00", "18.00");
        when(productoRepository.findById(100L)).thenReturn(Optional.of(alimento));

        VentaService.ResumenVenta pagoParcial = ventaService.calcularResumen(solicitud(
                "0.00",
                "200.00",
                List.of(linea(100L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL))
        ));

        VentaService.ResumenVenta pagoTotal = ventaService.calcularResumen(solicitud(
                "0.00",
                "354.00",
                List.of(linea(100L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL))
        ));

        assertEquals(EstadoVenta.PENDIENTE, pagoParcial.estado());
        assertEquals(new BigDecimal("154.00"), pagoParcial.balancePendiente());
        assertEquals(EstadoVenta.CERRADA, pagoTotal.estado());
        assertEquals(BigDecimal.ZERO.setScale(2), pagoTotal.balancePendiente());
    }

    @Test
    void calcularResumenRechazaMontoPagadoMayorQueTotal() {
        Producto alimento = producto(100L, CategoriaProducto.ALIMENTO, "150.00", "18.00");
        when(productoRepository.findById(100L)).thenReturn(Optional.of(alimento));

        VentaService.SolicitudVenta solicitud = solicitud(
                "0.00",
                "355.00",
                List.of(linea(100L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL))
        );

        assertThrows(IllegalArgumentException.class, () -> ventaService.calcularResumen(solicitud));
    }

    @Test
    void registrarVentaAsignaCadaDetalleALaVentaGuardada() {
        Producto alimento = producto(100L, CategoriaProducto.ALIMENTO, "150.00", "18.00");
        Producto medicamento = producto(200L, CategoriaProducto.MEDICAMENTO, "80.00", "0.00");
        prepararVenta(alimento, medicamento);

        ventaService.registrarVenta(solicitud(
                "0.00",
                "514.00",
                List.of(
                        linea(100L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL),
                        linea(200L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL)
                )
        ));

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());

        Venta ventaGuardada = ventaCaptor.getValue();
        assertEquals(2, ventaGuardada.getDetallesVentas().size());
        assertEquals(new BigDecimal("514.00"), ventaGuardada.getMontoTotal());
        assertEquals(EstadoVenta.CERRADA, ventaGuardada.getEstado());
        assertEquals(new BigDecimal("54.0000"), ventaGuardada.getDetallesVentas().get(0).getImpuesto());
        assertEquals(new BigDecimal("0.0000"), ventaGuardada.getDetallesVentas().get(1).getImpuesto());

        for (DetalleVenta detalle : ventaGuardada.getDetallesVentas()) {
            assertSame(ventaGuardada, detalle.getVenta());
        }
    }

    @Test
    void registrarVentaRegistraCobroInicialAsociadoAlClienteYVentaCorrectos() {
        Producto alimento = producto(100L, CategoriaProducto.ALIMENTO, "150.00", "18.00");
        prepararVenta(alimento);

        ventaService.registrarVenta(solicitud(
                "0.00",
                "354.00",
                List.of(linea(100L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL))
        ));

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        ArgumentCaptor<Cobro> cobroCaptor = ArgumentCaptor.forClass(Cobro.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        verify(cobroRepository).save(cobroCaptor.capture());

        Venta ventaGuardada = ventaCaptor.getValue();
        Cobro cobroGuardado = cobroCaptor.getValue();

        assertSame(cliente, cobroGuardado.getCliente());
        assertSame(ventaGuardada, cobroGuardado.getVenta());
        assertEquals(new BigDecimal("354.00"), cobroGuardado.getMontoTotal());
        assertEquals(MetodoPago.EFECTIVO, cobroGuardado.getMetodoPago());
    }

    @Test
    void calcularDeudaRestanteQuedaEnCeroCuandoLaVentaEstaTotalmenteCobrada() {
        Venta venta = venta(cliente, "1000.00");
        when(cobroRepository.sumMontoByVenta(venta)).thenReturn(new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("1000.00"), ventaService.calcularTotalCobrado(venta));
        assertEquals(BigDecimal.ZERO.setScale(2), ventaService.calcularDeudaRestante(venta));
    }

    @Test
    void calcularDeudaRestanteDetectaDiferenciaEntreVentaCobroParcialYDeuda() {
        Venta venta = venta(cliente, "1000.00");
        when(cobroRepository.sumMontoByVenta(venta)).thenReturn(new BigDecimal("400.00"));

        assertEquals(new BigDecimal("400.00"), ventaService.calcularTotalCobrado(venta));
        assertEquals(new BigDecimal("600.00"), ventaService.calcularDeudaRestante(venta));
    }

    @Test
    void registrarCobroRechazaMontoMayorQueDeudaRestanteDeLaVenta() {
        Venta venta = venta(cliente, "1000.00");
        when(cobroRepository.sumMontoByVenta(venta)).thenReturn(new BigDecimal("900.00"));

        assertThrows(IllegalArgumentException.class, () ->
                ventaService.registrarCobro(cliente, venta, MetodoPago.EFECTIVO, new BigDecimal("200.00"))
        );
    }

    @Test
    void registrarCobroActualizaEstadoACerradaCuandoLiquidaLaDeuda() {
        Venta venta = venta(cliente, "1000.00");
        when(cobroRepository.sumMontoByVenta(venta)).thenReturn(new BigDecimal("900.00"));
        when(cobroRepository.save(any(Cobro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ventaService.registrarCobro(cliente, venta, MetodoPago.EFECTIVO, new BigDecimal("100.00"));

        assertEquals(EstadoVenta.CERRADA, venta.getEstado());
        verify(ventaRepository).save(venta);
    }

    @Test
    void registrarCobroMantienePendienteCuandoQuedaDeuda() {
        Venta venta = venta(cliente, "1000.00");
        when(cobroRepository.sumMontoByVenta(venta)).thenReturn(new BigDecimal("300.00"));
        when(cobroRepository.save(any(Cobro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ventaService.registrarCobro(cliente, venta, MetodoPago.EFECTIVO, new BigDecimal("200.00"));

        assertEquals(EstadoVenta.PENDIENTE, venta.getEstado());
    }

    @Test
    void registrarCobroPermiteCobroSinVentaAsociada() {
        when(cobroRepository.save(any(Cobro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cobro cobro = ventaService.registrarCobro(cliente, null, MetodoPago.EFECTIVO, new BigDecimal("125.00"));

        assertSame(cliente, cobro.getCliente());
        assertNull(cobro.getVenta());
        assertEquals(new BigDecimal("125.00"), cobro.getMontoTotal());
        assertEquals(MetodoPago.EFECTIVO, cobro.getMetodoPago());
    }

    @Test
    void registrarCobroConNotaCreditoDescuentaSaldoYConservaTrazabilidad() {
        Venta venta = venta(cliente, "1000.00");
        NotaDeCredito nota = notaCredito(7L, cliente, "500.00");
        when(notaDeCreditoRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(nota));
        when(notaDeCreditoRepository.save(any(NotaDeCredito.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cobroRepository.save(any(Cobro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cobro cobro = ventaService.registrarCobro(
                cliente,
                venta,
                MetodoPago.NOTA_CREDITO,
                new BigDecimal("200.00"),
                null,
                7L
        );

        assertEquals(new BigDecimal("300.00"), nota.getSaldoDisponible());
        assertSame(nota, cobro.getNotaDeCredito());
        assertEquals(MetodoPago.NOTA_CREDITO, cobro.getMetodoPago());
        assertEquals(new BigDecimal("200.00"), cobro.getMontoTotal());
    }

    @Test
    void registrarCobroConNotaCreditoRechazaMontoMayorAlSaldo() {
        Venta venta = venta(cliente, "1000.00");
        NotaDeCredito nota = notaCredito(7L, cliente, "150.00");
        when(notaDeCreditoRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(nota));

        assertThrows(IllegalArgumentException.class, () ->
                ventaService.registrarCobro(
                        cliente,
                        venta,
                        MetodoPago.NOTA_CREDITO,
                        new BigDecimal("200.00"),
                        null,
                        7L
                )
        );

        assertEquals(new BigDecimal("150.00"), nota.getSaldoDisponible());
    }

    @Test
    void calcularResumenSoportaMultiplesLineasMezclandoProductosYServicios() {
        Producto alimento = producto(100L, CategoriaProducto.ALIMENTO, "150.00", "18.00");
        Producto servicio = producto(300L, CategoriaProducto.SERVICIO, "500.00", "0.00");
        when(productoRepository.findById(100L)).thenReturn(Optional.of(alimento));
        when(productoRepository.findById(300L)).thenReturn(Optional.of(servicio));

        VentaService.ResumenVenta resumen = ventaService.calcularResumen(solicitud(
                "50.00",
                "804.00",
                List.of(
                        linea(100L, "2.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL),
                        linea(300L, "1.00", 1L, 1L, EstrategiaPrecioVenta.NORMAL)
                )
        ));

        assertEquals(new BigDecimal("854.00"), resumen.subtotal());
        assertEquals(new BigDecimal("50.00"), resumen.descuento());
        assertEquals(new BigDecimal("804.00"), resumen.total());
        assertEquals(BigDecimal.ZERO.setScale(2), resumen.balancePendiente());
        assertEquals(EstadoVenta.CERRADA, resumen.estado());
    }

    private void prepararVenta(Producto... productos) {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(empleadoRepository.findById(10L)).thenReturn(Optional.of(vendedor));

        Almacen almacen = almacen(1L);
        Lote lote = lote(1L);
        Inventario inventario = inventario(almacen, lote, "1000.00");

        when(almacenRepository.findById(1L)).thenReturn(Optional.of(almacen));
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(inventarioRepository.findByAlmacenAndLote(any(Almacen.class), any(Lote.class)))
                .thenReturn(Optional.of(inventario));

        for (Producto producto : productos) {
            when(productoRepository.findById(producto.getIdProducto())).thenReturn(Optional.of(producto));
        }
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setIdVenta(55L);
            return venta;
        });
    }

    private Almacen almacen(Long idAlmacen) {
        Almacen almacen = new Almacen();
        almacen.setIdAlmacen(idAlmacen);
        almacen.setNombre("Almacen prueba");
        return almacen;
    }

    private Lote lote(Long idLote) {
        Lote lote = new Lote();
        lote.setIdLote(idLote);
        lote.setNumeroLote("L-001");
        return lote;
    }

    private Inventario inventario(Almacen almacen, Lote lote, String cantidadActual) {
        Inventario inventario = new Inventario();
        inventario.setAlmacen(almacen);
        inventario.setLote(lote);
        inventario.setCantidadActual(bd(cantidadActual));
        return inventario;
    }

    private VentaService.SolicitudVenta solicitud(List<VentaService.LineaVentaRequest> lineas) {
        return solicitud("0.00", "0.00", lineas);
    }

    private VentaService.SolicitudVenta solicitud(
            String descuento,
            String montoPagado,
            List<VentaService.LineaVentaRequest> lineas
    ) {
        return new VentaService.SolicitudVenta(
                new VentaService.ClienteVentaRequest(1L, null, null, null, null, null),
                10L,
                false,
                LocalDate.now().plusDays(15),
                "B0100000001",
                new BigDecimal(0),
                bd(descuento),
                bd(montoPagado),
                MetodoPago.EFECTIVO,
                null,
                null,
                lineas
        );
    }

    private VentaService.DatosTransferencia transferencia(String referencia) {
        return new VentaService.DatosTransferencia(
                "BHD",
                "Cliente prueba",
                referencia,
                new byte[]{1, 2, 3},
                "comprobante.png",
                "image/png",
                true
        );
    }

    private VentaService.LineaVentaRequest linea(Long idProducto, String cantidad, Long idAlmacen, Long idLote, EstrategiaPrecioVenta estrategia) {
        return new VentaService.LineaVentaRequest(idProducto, bd(cantidad), null, idAlmacen, idLote, estrategia);
    }

    private Producto producto(Long idProducto, CategoriaProducto categoria, String precioEmpaque, String porcentajeImpuesto) {
        Producto producto = new Producto();
        producto.setIdProducto(idProducto);
        producto.setNombre(categoria == CategoriaProducto.SERVICIO ? "Consulta veterinaria" : "Producto " + idProducto);
        producto.setCategoria(categoria);
        producto.setUnidadEmpaque(UnidadEmpaque.UNIDAD_COMPLETA);
        producto.setPrecioEmpaque(bd(precioEmpaque));
        producto.setPorcentajeImpuesto(bd(porcentajeImpuesto));
        producto.setPermiteFraccionamiento(false);
        producto.setStatus(StatusEntidad.ACTIVO);
        return producto;
    }

    private Cliente cliente(Long idCliente) {
        Persona persona = new Persona();
        persona.setNombre("Cliente prueba");
        persona.setCedula("001-0000000-1");

        TipoCliente tipoCliente = new TipoCliente();
        tipoCliente.setIdTipoCliente(1L);
        tipoCliente.setNombreTipoCliente("Regular");
        tipoCliente.setDescuento(BigDecimal.ZERO);

        Cliente cliente = new Cliente();
        cliente.setIdCliente(idCliente);
        cliente.setPersona(persona);
        cliente.setTipoCliente(tipoCliente);
        return cliente;
    }

    private NotaDeCredito notaCredito(Long id, Cliente cliente, String saldo) {
        NotaDeCredito nota = new NotaDeCredito();
        nota.setIdNotaCredito(id);
        nota.setCliente(cliente);
        nota.setMonto(bd(saldo));
        nota.setSaldoDisponible(bd(saldo));
        return nota;
    }

    private Empleado vendedor(Long idEmpleado) {
        Persona persona = new Persona();
        persona.setNombre("Vendedor prueba");

        Empleado empleado = new Empleado();
        empleado.setIdEmpleado(idEmpleado);
        empleado.setPersona(persona);
        return empleado;
    }

    private Venta venta(Cliente cliente, String montoTotal) {
        Venta venta = new Venta();
        venta.setIdVenta(99L);
        venta.setCliente(cliente);
        venta.setMontoTotal(bd(montoTotal));
        venta.setEstado(EstadoVenta.PENDIENTE);
        return venta;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
