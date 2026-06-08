package com.agroveterinaria.service;

import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Cobro;
import com.agroveterinaria.entity.DetalleVenta;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.TipoCliente;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.enums.MetodoPago;
import com.agroveterinaria.repository.ClienteRepository;
import com.agroveterinaria.repository.CobroRepository;
import com.agroveterinaria.repository.EmpleadoRepository;
import com.agroveterinaria.repository.ProductoRepository;
import com.agroveterinaria.repository.TipoClienteRepository;
import com.agroveterinaria.repository.VentaRepository;
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
    private final PersonaService personaService;

    public VentaService(
            VentaRepository ventaRepository,
            ClienteRepository clienteRepository,
            TipoClienteRepository tipoClienteRepository,
            EmpleadoRepository empleadoRepository,
            ProductoRepository productoRepository,
            CobroRepository cobroRepository,
            PersonaService personaService
    ) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.tipoClienteRepository = tipoClienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.productoRepository = productoRepository;
        this.cobroRepository = cobroRepository;
        this.personaService = personaService;
    }

    @Transactional(readOnly = true)
    public List<Venta> listarTodos() {
        return ventaRepository.findAll();
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
        venta.setLlevaDespacho(Boolean.TRUE.equals(solicitud.llevaDespacho()));
        venta.setFechaVencimientoPago(estado == EstadoVenta.PENDIENTE ? toDateTime(solicitud.fechaVencimientoPago()) : null);

        List<DetalleVenta> detalles = new ArrayList<>();
        for (LineaVentaRequest linea : solicitud.lineas()) {
            Producto producto = productoRepository.findById(linea.idProducto())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos seleccionados no existe."));

            BigDecimal cantidad = normalizarCantidad(linea.cantidad());
            BigDecimal precioUnitario = seleccionarPrecio(producto, cantidad);
            BigDecimal impuesto = normalizarMonto(linea.impuesto());

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitarioVenta(precioUnitario);
            detalle.setImpuesto(impuesto.setScale(4, RoundingMode.HALF_UP));
            detalles.add(detalle);
        }

        venta.reemplazarDetalles(detalles);
        venta.setMontoTotal(resumen.total());

        Venta ventaGuardada = ventaRepository.save(venta);
        registrarCobroInicial(cliente, solicitud.metodoPago(), montoPagado);
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
            detalle.setPrecioUnitarioVenta(seleccionarPrecio(producto, cantidad));
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

    private void registrarCobroInicial(Cliente cliente, MetodoPago metodoPago, BigDecimal montoPagado) {
        if (montoPagado.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (metodoPago == null) {
            throw new IllegalArgumentException("Debes seleccionar el metodo de pago cuando registras un monto pagado.");
        }

        Cobro cobro = new Cobro();
        cobro.setCliente(cliente);
        cobro.setMontoTotal(montoPagado);
        cobro.setMetodoPago(metodoPago);
        cobroRepository.save(cobro);
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

    private BigDecimal seleccionarPrecio(Producto producto, BigDecimal cantidad) {
        boolean ventaFraccionada = Boolean.TRUE.equals(producto.getPermiteFraccionamiento())
                && cantidad.stripTrailingZeros().scale() > 0;

        BigDecimal precio = ventaFraccionada ? producto.getPrecioFraccion() : producto.getPrecioEmpaque();
        if (precio == null) {
            throw new IllegalArgumentException("El producto " + producto.getNombre() + " no tiene precio configurado para esta venta.");
        }
        return precio.setScale(2, RoundingMode.HALF_UP);
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
            BigDecimal impuesto
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
