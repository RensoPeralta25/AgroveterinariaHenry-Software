package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.enums.EstrategiaPrecioVenta;
import com.agroveterinaria.enums.MetodoPago;
import com.agroveterinaria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final TipoClienteRepository tipoClienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ProductoRepository productoRepository;
    private final CobroRepository cobroRepository;
    private final AlmacenRepository almacenRepository;
    private final InventarioRepository inventarioRepository;
    private final LoteRepository loteRepository;
    private final PersonaService personaService;

    public VentaService(
            VentaRepository ventaRepository,
            ClienteRepository clienteRepository,
            TipoClienteRepository tipoClienteRepository,
            EmpleadoRepository empleadoRepository,
            ProductoRepository productoRepository,
            CobroRepository cobroRepository,
            PersonaService personaService,
            AlmacenRepository almacenRepository,
            InventarioRepository inventarioRepository,
            LoteRepository loteRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.tipoClienteRepository = tipoClienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.productoRepository = productoRepository;
        this.cobroRepository = cobroRepository;
        this.personaService = personaService;
        this.almacenRepository = almacenRepository;
        this.inventarioRepository = inventarioRepository;
        this.loteRepository = loteRepository;
    }

    @Transactional(readOnly = true)
    public List<Venta> listarTodos() {
        return ventaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cobro> listarCobros() {
        return cobroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Venta> buscarPorId(Long idVenta) {
        return ventaRepository.findById(idVenta);
    }

    @Transactional
    public Venta guardar(Venta venta) {
        return ventaRepository.save(venta);
    }

    @Transactional
    public Venta registrarVenta(SolicitudVenta solicitud) {
        validarSolicitud(solicitud);

        Cliente cliente = buscarOCrearCliente(solicitud.cliente());
        Empleado vendedor = empleadoRepository.findById(solicitud.idVendedor())
                .orElseThrow(() -> new IllegalArgumentException("El vendedor seleccionado no existe."));

        ResumenVenta resumen = calcularResumen(solicitud);
        BigDecimal montoPagado = normalizarMonto(solicitud.montoPagado());
        EstadoVenta estado = calcularEstado(resumen.total(), montoPagado);

        if (estado == EstadoVenta.PENDIENTE && solicitud.fechaVencimientoPago() == null) {
            throw new IllegalArgumentException("Debes indicar la fecha de vencimiento cuando la venta queda pendiente.");
        }

        Venta venta = new Venta();
        venta.setFechaHoraVenta(LocalDateTime.now());
        venta.setCliente(cliente);
        venta.setVendedor(vendedor);
        venta.setEstado(estado);
        venta.setComprobanteFiscal(valorNormalizado(solicitud.comprobanteFiscal()));
        venta.setAplicaDescuentoVenta(resumen.descuento().compareTo(BigDecimal.ZERO) > 0);

        boolean llevaDespacho = Boolean.TRUE.equals(solicitud.llevaDespacho());
        venta.setLlevaDespacho(llevaDespacho);
        venta.setFechaVencimientoPago(estado == EstadoVenta.PENDIENTE ? toDateTime(solicitud.fechaVencimientoPago()) : null);

        List<DetalleVenta> detalles = new ArrayList<>();

        for (LineaVentaRequest linea : solicitud.lineas()) {
            Producto producto = productoRepository.findById(linea.idProducto())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos seleccionados no existe."));

            BigDecimal cantidadPedida = normalizarCantidad(linea.cantidad());
            BigDecimal precioUnitario = seleccionarPrecio(producto, cantidadPedida, linea.estrategia());
            BigDecimal impuestoBase = normalizarMonto(linea.impuesto());

            if (producto.getCategoria() == CategoriaProducto.SERVICIO) {
                DetalleVenta detalle = new DetalleVenta();
                detalle.setProducto(producto);
                detalle.setCantidad(cantidadPedida);
                detalle.setPrecioUnitarioVenta(precioUnitario);
                detalle.setImpuesto(impuestoBase.setScale(4, RoundingMode.HALF_UP));
                detalle.setAlmacen(null);
                detalle.setLote(null);

                detalles.add(detalle);
            }
            else {
                if (linea.idAlmacen() == null) {
                    throw new IllegalArgumentException("El almacén origen es obligatorio para el producto físico: " + producto.getNombre());
                }

                Almacen almacen = almacenRepository.findById(linea.idAlmacen())
                        .orElseThrow(() -> new IllegalArgumentException("El almacén origen no es válido."));

                if (linea.idLote() != null) {
                    Lote lote = loteRepository.findById(linea.idLote())
                            .orElseThrow(() -> new IllegalArgumentException("Lote no válido."));

                    if (!llevaDespacho) {
                        Inventario inv = inventarioRepository.findByAlmacenAndLote(almacen, lote)
                                .orElseThrow(() -> new IllegalArgumentException("No hay registro de inventario para el lote " + lote.getNumeroLote()));

                        if (inv.getCantidadActual().compareTo(cantidadPedida) < 0) {
                            throw new IllegalArgumentException("Stock físico insuficiente para entrega inmediata del lote " + lote.getNumeroLote());
                        }
                        inv.setCantidadActual(inv.getCantidadActual().subtract(cantidadPedida));
                        inventarioRepository.save(inv);
                    }

                    DetalleVenta detalle = new DetalleVenta();
                    detalle.setProducto(producto);
                    detalle.setCantidad(cantidadPedida);
                    detalle.setPrecioUnitarioVenta(precioUnitario);
                    detalle.setImpuesto(impuestoBase.setScale(4, RoundingMode.HALF_UP));
                    detalle.setAlmacen(almacen);
                    detalle.setLote(lote);

                    detalles.add(detalle);
                }
                else {
                    BigDecimal cantidadPendientePorAsignar = cantidadPedida;

                    List<Inventario> inventarioDisponible = inventarioRepository
                            .findByAlmacenAndProductoOrderByLote_FechaVencimientoAsc(almacen, producto).stream()
                            .filter(inv -> inv.getCantidadActual().compareTo(BigDecimal.ZERO) > 0)
                            .toList();

                    for (Inventario inv : inventarioDisponible) {
                        if (cantidadPendientePorAsignar.compareTo(BigDecimal.ZERO) <= 0) break;

                        BigDecimal cantidadATomar = cantidadPendientePorAsignar.min(inv.getCantidadActual());

                        if (!llevaDespacho) {
                            inv.setCantidadActual(inv.getCantidadActual().subtract(cantidadATomar));
                            inventarioRepository.save(inv);
                        }

                        BigDecimal proporcion = cantidadATomar.divide(cantidadPedida, 6, RoundingMode.HALF_UP);
                        BigDecimal impuestoProporcional = impuestoBase.multiply(proporcion);

                        DetalleVenta detalleFraccionado = new DetalleVenta();
                        detalleFraccionado.setProducto(producto);
                        detalleFraccionado.setCantidad(cantidadATomar);
                        detalleFraccionado.setPrecioUnitarioVenta(precioUnitario);
                        detalleFraccionado.setImpuesto(impuestoProporcional.setScale(4, RoundingMode.HALF_UP));
                        detalleFraccionado.setAlmacen(almacen);
                        detalleFraccionado.setLote(inv.getLote());

                        detalles.add(detalleFraccionado);
                        cantidadPendientePorAsignar = cantidadPendientePorAsignar.subtract(cantidadATomar);
                    }

                    if (cantidadPendientePorAsignar.compareTo(BigDecimal.ZERO) > 0) {

                        if (!llevaDespacho) {
                            throw new IllegalArgumentException(
                                    "No hay stock físico suficiente para entrega inmediata de: "
                                            + producto.getNombre() + ". Faltan " + cantidadPendientePorAsignar
                                            + " unidades. Si el cliente esperará a que llegue, marque 'Lleva Despacho'."
                            );
                        }

                        BigDecimal proporcionRestante = cantidadPendientePorAsignar.divide(cantidadPedida, 6, RoundingMode.HALF_UP);
                        BigDecimal impuestoRestante = impuestoBase.multiply(proporcionRestante);

                        DetalleVenta detallePendiente = new DetalleVenta();
                        detallePendiente.setProducto(producto);
                        detallePendiente.setCantidad(cantidadPendientePorAsignar);
                        detallePendiente.setPrecioUnitarioVenta(precioUnitario);
                        detallePendiente.setImpuesto(impuestoRestante.setScale(4, RoundingMode.HALF_UP));
                        detallePendiente.setAlmacen(almacen);
                        detallePendiente.setLote(null);

                        detalles.add(detallePendiente);
                    }
                }
            }
        }

        venta.reemplazarDetalles(detalles);
        venta.setMontoTotal(resumen.total());

        Venta ventaGuardada = ventaRepository.save(venta);
        registrarCobroInicial(cliente, ventaGuardada, solicitud.metodoPago(), montoPagado);
        return ventaGuardada;
    }

    @Transactional(readOnly = true)
    public ResumenVenta calcularResumen(SolicitudVenta solicitud) {
        validarLineas(solicitud.lineas());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaVentaRequest linea : solicitud.lineas()) {
            Producto producto = productoRepository.findById(linea.idProducto())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos seleccionados no existe."));
            BigDecimal cantidad = normalizarCantidad(linea.cantidad());
            BigDecimal impuesto = normalizarMonto(linea.impuesto());
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitarioVenta(seleccionarPrecio(producto, cantidad, linea.estrategia()));
            detalle.setImpuesto(impuesto.setScale(4, RoundingMode.HALF_UP));
            subtotal = subtotal.add(detalle.calcularSubtotal());
        }

        BigDecimal descuento = normalizarMonto(solicitud.descuento());
        if (descuento.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("El descuento no puede ser mayor que el subtotal.");
        }

        BigDecimal total = subtotal.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoPagado = normalizarMonto(solicitud.montoPagado());
        if (montoPagado.compareTo(total) > 0) {
            throw new IllegalArgumentException("El monto pagado no puede ser mayor que el total.");
        }

        return new ResumenVenta(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                descuento,
                total,
                montoPagado,
                total.subtract(montoPagado).setScale(2, RoundingMode.HALF_UP),
                calcularEstado(total, montoPagado)
        );
    }

    @Transactional
    public void eliminarPorId(Long idVenta) {
        ventaRepository.deleteById(idVenta);
    }

    @Transactional
    public void eliminar(Venta venta) {
        ventaRepository.delete(venta);
    }

    private Cliente buscarOCrearCliente(ClienteVentaRequest clienteRequest) {
        if (clienteRequest.idCliente() != null) {
            return clienteRepository.findById(clienteRequest.idCliente())
                    .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));
        }

        return clienteRepository.findByPersonaCedula(valorNormalizado(clienteRequest.cedula()))
                .orElseGet(() -> crearCliente(clienteRequest));
    }

    private Cliente crearCliente(ClienteVentaRequest clienteRequest) {
        TipoCliente tipoCliente = tipoClienteRepository.findById(clienteRequest.idTipoCliente())
                .orElseThrow(() -> new IllegalArgumentException("El tipo de cliente seleccionado no existe."));

        String cedula = valorNormalizado(clienteRequest.cedula());
        Persona persona = personaService.findByCedula(cedula)
                .orElseGet(() -> {
                    Persona nuevaPersona = new Persona();
                    nuevaPersona.setCedula(cedula);
                    nuevaPersona.setNombre(valorNormalizado(clienteRequest.nombre()));
                    nuevaPersona.setTelefono(valorNormalizado(clienteRequest.telefono()));
                    nuevaPersona.setDireccion(valorNormalizado(clienteRequest.direccion()));
                    return personaService.save(nuevaPersona);
                });

        Cliente cliente = new Cliente();
        cliente.setPersona(persona);
        cliente.setTipoCliente(tipoCliente);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cobro registrarCobro(Cliente cliente, Venta venta, MetodoPago metodoPago, BigDecimal monto) {
        if (cliente == null) {
            throw new IllegalArgumentException("Debes indicar el cliente del cobro.");
        }

        BigDecimal montoNormalizado = normalizarMonto(monto);
        if (montoNormalizado.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("El monto cobrado debe ser mayor que cero.");
        }

        if (metodoPago == null) {
            throw new IllegalArgumentException("Debes seleccionar el metodo de pago.");
        }

        validarMetodoPagoDisponible(metodoPago);

        BigDecimal deudaDespuesDelCobro = null;
        if (venta != null) {
            if (!esMismoCliente(cliente, venta.getCliente())) {
                throw new IllegalArgumentException("El cobro debe pertenecer al mismo cliente de la venta.");
            }

            BigDecimal deudaRestante = calcularDeudaRestante(venta);
            if (montoNormalizado.compareTo(deudaRestante) > 0) {
                throw new IllegalArgumentException("El monto cobrado no puede ser mayor que la deuda restante de la venta.");
            }
            deudaDespuesDelCobro = deudaRestante.subtract(montoNormalizado).setScale(2, RoundingMode.HALF_UP);
        }

        Cobro cobro = new Cobro();
        cobro.setCliente(cliente);
        cobro.setVenta(venta);
        cobro.setMontoTotal(montoNormalizado);
        cobro.setMetodoPago(metodoPago);
        Cobro cobroGuardado = cobroRepository.save(cobro);

        actualizarEstadoPorDeuda(venta, deudaDespuesDelCobro);

        return cobroGuardado;
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalCobrado(Venta venta) {
        if (venta == null || venta.getIdVenta() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalCobrado = cobroRepository.sumMontoByVenta(venta);
        return (totalCobrado != null ? totalCobrado : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularDeudaRestante(Venta venta) {
        if (venta == null || venta.getMontoTotal() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return venta.getMontoTotal()
                .subtract(calcularTotalCobrado(venta))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void registrarCobroInicial(Cliente cliente, Venta venta, MetodoPago metodoPago, BigDecimal montoPagado) {
        if (montoPagado.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        registrarCobro(cliente, venta, metodoPago, montoPagado);
    }

    private boolean esMismoCliente(Cliente cliente, Cliente clienteVenta) {
        if (cliente == null || clienteVenta == null) {
            return false;
        }
        if (cliente.getIdCliente() != null && clienteVenta.getIdCliente() != null) {
            return cliente.getIdCliente().equals(clienteVenta.getIdCliente());
        }
        return cliente == clienteVenta;
    }

    private void validarMetodoPagoDisponible(MetodoPago metodoPago) {
        if (metodoPago != MetodoPago.EFECTIVO) {
            throw new IllegalArgumentException("Por ahora solo se aceptan pagos en efectivo.");
        }
    }

    private void actualizarEstadoPorDeuda(Venta venta, BigDecimal deudaDespuesDelCobro) {
        if (venta == null || deudaDespuesDelCobro == null) {
            return;
        }

        EstadoVenta nuevoEstado = deudaDespuesDelCobro.compareTo(BigDecimal.ZERO) == 0
                ? EstadoVenta.CERRADA
                : EstadoVenta.PENDIENTE;

        if (venta.getEstado() != nuevoEstado) {
            venta.setEstado(nuevoEstado);
            ventaRepository.save(venta);
        }
    }

    private void validarSolicitud(SolicitudVenta solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("No se recibieron datos de la venta.");
        }
        validarCliente(solicitud.cliente());
        if (solicitud.idVendedor() == null) {
            throw new IllegalArgumentException("Debes seleccionar un vendedor.");
        }
        validarLineas(solicitud.lineas());
    }

    private void validarCliente(ClienteVentaRequest cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Debes indicar los datos del cliente.");
        }
        if (cliente.idCliente() != null) {
            return;
        }
        if (valorNormalizado(cliente.cedula()).isBlank()) {
            throw new IllegalArgumentException("La cedula del cliente es obligatoria.");
        }
        if (valorNormalizado(cliente.nombre()).isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
        if (valorNormalizado(cliente.telefono()).isBlank()) {
            throw new IllegalArgumentException("El telefono del cliente es obligatorio.");
        }
        if (valorNormalizado(cliente.direccion()).isBlank()) {
            throw new IllegalArgumentException("La direccion del cliente es obligatoria.");
        }
        if (cliente.idTipoCliente() == null) {
            throw new IllegalArgumentException("Debes seleccionar el tipo de cliente.");
        }
    }

    private void validarLineas(List<LineaVentaRequest> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("Debes agregar al menos un producto a la venta.");
        }
        for (LineaVentaRequest linea : lineas) {
            if (linea.idProducto() == null) {
                throw new IllegalArgumentException("Todas las lineas deben tener un producto.");
            }
            normalizarCantidad(linea.cantidad());
        }
    }

    private BigDecimal seleccionarPrecio(Producto producto, BigDecimal cantidad, EstrategiaPrecioVenta estrategia) {
        if (!Boolean.TRUE.equals(producto.getPermiteFraccionamiento())) {
            return producto.getPrecioEmpaque().setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal factor = producto.getContenidoPorEmpaque();
        if (factor == null || factor.compareTo(BigDecimal.ONE) <= 0) {
            return producto.getPrecioEmpaque().setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal precioEmp = producto.getPrecioEmpaque();
        BigDecimal precioFracc = producto.getPrecioFraccion() != null ? producto.getPrecioFraccion() : precioEmp.divide(factor, 4, RoundingMode.HALF_UP);

        BigDecimal subtotalSinImpuesto = BigDecimal.ZERO;

        if (estrategia == null) {
            estrategia = EstrategiaPrecioVenta.NORMAL;
        }

        switch (estrategia) {
            case TODO_PRECIO_EMPAQUE:
                BigDecimal precioUnidadProporcional = precioEmp.divide(factor, 6, RoundingMode.HALF_UP);
                subtotalSinImpuesto = cantidad.multiply(precioUnidadProporcional);
                break;
            case TODO_PRECIO_FRACCION:
                subtotalSinImpuesto = cantidad.multiply(precioFracc);
                break;
            case NORMAL:
            default:
                BigDecimal[] division = cantidad.divideAndRemainder(factor);
                BigDecimal cajas = division[0];
                BigDecimal unidadesSueltas = division[1];
                subtotalSinImpuesto = cajas.multiply(precioEmp).add(unidadesSueltas.multiply(precioFracc));
                break;
        }

        return subtotalSinImpuesto.divide(cantidad, 6, RoundingMode.HALF_UP);
    }

    private EstadoVenta calcularEstado(BigDecimal total, BigDecimal montoPagado) {
        return montoPagado.compareTo(total) >= 0 ? EstadoVenta.CERRADA : EstadoVenta.PENDIENTE;
    }

    private BigDecimal normalizarCantidad(BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        return cantidad.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarMonto(BigDecimal monto) {
        if (monto == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los montos no pueden ser negativos.");
        }
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime toDateTime(LocalDate fecha) {
        return fecha != null ? fecha.atStartOfDay() : null;
    }

    private String valorNormalizado(String valor) {
        return valor == null ? "" : valor.trim();
    }

    @Transactional(readOnly = true)
    public List<Venta> findByEstado(EstadoVenta estado) {
        if (estado == null) {
            return List.of();
        }
        return ventaRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public Venta obtenerVentaConDetalles(Long idVenta) {
        if (idVenta == null) return null;
        return ventaRepository.findVentaConDetallesByIdVenta(idVenta).orElse(null);
    }

    public record SolicitudVenta(
            ClienteVentaRequest cliente,
            Long idVendedor,
            Boolean llevaDespacho,
            LocalDate fechaVencimientoPago,
            String comprobanteFiscal,
            BigDecimal descuento,
            BigDecimal montoPagado,
            MetodoPago metodoPago,
            List<LineaVentaRequest> lineas
    ) {
    }

    public record ClienteVentaRequest(
            Long idCliente,
            String cedula,
            String nombre,
            String telefono,
            String direccion,
            Long idTipoCliente
    ) {
    }

    public record LineaVentaRequest(
            Long idProducto,
            BigDecimal cantidad,
            BigDecimal impuesto,
            Long idAlmacen,
            Long idLote,
            EstrategiaPrecioVenta estrategia
    ) {
    }

    public record ResumenVenta(
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal total,
            BigDecimal montoPagado,
            BigDecimal balancePendiente,
            EstadoVenta estado
    ) {
    }
}
