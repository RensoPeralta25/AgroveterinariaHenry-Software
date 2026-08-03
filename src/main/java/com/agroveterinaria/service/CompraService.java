package com.agroveterinaria.service;

import com.agroveterinaria.dto.detalle_compra.DetalleCompraDTO;
import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.entity.DetalleCompra;
import com.agroveterinaria.entity.GastoOperativo;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.enums.EstadoRecepcion;
import com.agroveterinaria.enums.TipoGasto;
import com.agroveterinaria.repository.CompraRepository;
import com.agroveterinaria.repository.DetalleCompraRepository;
import com.agroveterinaria.repository.DetalleRecepcionRepository;
import com.agroveterinaria.repository.GastoOperativoRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RolesAllowed("ADMINISTRADOR")
public class CompraService {

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final DetalleRecepcionRepository detalleRecepcionRepository;
    private final GastoOperativoRepository gastoOperativoRepository;

    public CompraService(CompraRepository compraRepository, DetalleCompraRepository detalleCompraRepository, DetalleRecepcionRepository detalleRecepcionRepository, GastoOperativoRepository gastoOperativoRepository) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.detalleRecepcionRepository = detalleRecepcionRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
    }

    @Transactional(readOnly = true)
    public List<Compra> listarTodos() {
        return compraRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Compra> buscarPorId(Long idCompra) {
        return compraRepository.findById(idCompra);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerUltimoCosto(Producto producto, Proveedor proveedor) {
        if (producto == null || proveedor == null) {
            return null;
        }

        return detalleCompraRepository
                .findFirstByProductoAndCompraProveedorOrderByCompraFechaHoraCompraDesc(producto, proveedor)
                .map(detalle -> detalle.getPrecioUnitarioCompra())
                .orElse(null);
    }

    @Transactional
    public Compra guardar(Compra compra) {
        return compraRepository.save(compra);
    }

    @Transactional
    public void eliminarPorId(Long idCompra) {
        compraRepository.findById(idCompra).ifPresent(compra -> {
            GastoOperativo gastoAsociado = compra.getGastoAsociado();
            compraRepository.delete(compra);

            if (gastoAsociado != null) {
                gastoOperativoRepository.delete(gastoAsociado);
            }
        });
    }

    @Transactional
    public void eliminar(Compra compra) {
        eliminarPorId(compra.getIdCompra());
    }

    @Transactional
    public void registrarCompra(Proveedor proveedor, List<DetalleCompraDTO> detallesDTO) {
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor no puede ser nulo");
        }
        if (detallesDTO == null || detallesDTO.isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un producto");
        }

        Compra nuevaCompra = new Compra();
        nuevaCompra.setProveedor(proveedor);
        nuevaCompra.setFechaHoraCompra(java.time.LocalDateTime.now());

        BigDecimal totalCompra = BigDecimal.ZERO;
        for (DetalleCompraDTO dto : detallesDTO) {
            totalCompra = totalCompra.add(dto.getSubtotal());
        }
        nuevaCompra.setTotal(totalCompra);

        for (DetalleCompraDTO dto : detallesDTO) {
            DetalleCompra detalleReal = new DetalleCompra();
            detalleReal.setCompra(nuevaCompra);
            detalleReal.setProducto(dto.getProducto());
            detalleReal.setCantidad(dto.getCantidad());
            detalleReal.setPrecioUnitarioCompra(dto.getCostoActual());
            detalleReal.setImpuesto(BigDecimal.ZERO);

            nuevaCompra.addDetalle(detalleReal);
        }

        GastoOperativo nuevoGasto = new GastoOperativo();
        nuevoGasto.setNotas("Compra de mercancía - Proveedor: " + proveedor.getNombre());
        nuevoGasto.setMonto(totalCompra);
        nuevoGasto.setFecha(java.time.LocalDate.now());
        nuevoGasto.setTipoGasto(TipoGasto.VARIABLE);

        nuevoGasto = gastoOperativoRepository.save(nuevoGasto);
        nuevaCompra.setGastoAsociado(nuevoGasto);

        compraRepository.save(nuevaCompra);
    }

    public List<Compra> listarComprasPendientes() {
        return compraRepository.findByEstadoRecepcionIn(List.of(EstadoRecepcion.PENDIENTE, EstadoRecepcion.PARCIAL));
    }

    public List<DetalleCompra> obtenerDetallesPorCompra(Long idCompra) {
        return detalleCompraRepository.findByCompra_IdCompra(idCompra);
    }

    public BigDecimal calcularCantidadPendiente(DetalleCompra detalleCompra) {
        BigDecimal yaRecibido = detalleRecepcionRepository.sumCantidadProcesadaByDetalleCompra(detalleCompra);
        if(yaRecibido == null) yaRecibido = BigDecimal.ZERO;
        return detalleCompra.getCantidad().subtract(yaRecibido);
    }

    public Optional<Compra> obtenerUltimoBorrador() {
        return compraRepository.findFirstByEstadoRecepcionOrderByIdCompraDesc(EstadoRecepcion.BORRADOR);
    }

    public boolean tieneRecepcionesAsociadas(Long idCompra) {
        Compra compra = compraRepository.findById(idCompra).orElse(null);
        if (compra == null) return false;
        return compra.getEstadoRecepcion() == EstadoRecepcion.PARCIAL
                || compra.getEstadoRecepcion() == EstadoRecepcion.RECIBIDA;
    }

    @Transactional
    public Compra guardarBorradorSilencioso(Long idBorrador, Proveedor proveedor, List<DetalleCompraDTO> detallesDTO) {
        Compra borrador;
        if (idBorrador != null) {
            borrador = compraRepository.findById(idBorrador).orElse(new Compra());
            borrador.getDetalles().clear();
        } else {
            borrador = new Compra();
        }

        borrador.setProveedor(proveedor);
        borrador.setEstadoRecepcion(EstadoRecepcion.BORRADOR);
        borrador.setFechaHoraCompra(java.time.LocalDateTime.now());

        BigDecimal totalCompra = BigDecimal.ZERO;
        for (DetalleCompraDTO dto : detallesDTO) {
            totalCompra = totalCompra.add(dto.getSubtotal());

            DetalleCompra detalleReal = new DetalleCompra();
            detalleReal.setCompra(borrador);
            detalleReal.setProducto(dto.getProducto());
            detalleReal.setCantidad(dto.getCantidad());
            detalleReal.setPrecioUnitarioCompra(dto.getCostoActual());
            detalleReal.setImpuesto(BigDecimal.ZERO);

            borrador.addDetalle(detalleReal);
        }
        borrador.setTotal(totalCompra);

        if (borrador.getGastoAsociado() != null) {
            GastoOperativo gasto = borrador.getGastoAsociado();
            gasto.setMonto(totalCompra);
            gastoOperativoRepository.save(gasto);
        }

        return compraRepository.save(borrador);
    }

    @Transactional
    public void confirmarBorradorComoPendiente(Long idBorrador) {
        Compra compra = compraRepository.findById(idBorrador)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el borrador a procesar"));

        compra.setEstadoRecepcion(EstadoRecepcion.PENDIENTE);
        compra.setFechaHoraCompra(java.time.LocalDateTime.now());

        GastoOperativo gasto = compra.getGastoAsociado();
        if (gasto == null) {
            gasto = new GastoOperativo();
            gasto.setNotas("Compra de mercancía - Proveedor: " + compra.getProveedor().getNombre());
            gasto.setFecha(java.time.LocalDate.now());
            gasto.setTipoGasto(TipoGasto.VARIABLE);
        }

        gasto.setMonto(compra.getTotal());
        gasto = gastoOperativoRepository.save(gasto);
        compra.setGastoAsociado(gasto);

        compraRepository.save(compra);
    }

    @Transactional
    public void actualizarCompraExistente(Long idCompra, Proveedor proveedor, List<DetalleCompraDTO> detallesDTO) {
        Compra compra = compraRepository.findById(idCompra)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));

        compra.setProveedor(proveedor);
        compra.getDetalles().clear();

        BigDecimal totalCompra = BigDecimal.ZERO;
        for (DetalleCompraDTO dto : detallesDTO) {
            totalCompra = totalCompra.add(dto.getSubtotal());

            DetalleCompra detalleReal = new DetalleCompra();
            detalleReal.setCompra(compra);
            detalleReal.setProducto(dto.getProducto());
            detalleReal.setCantidad(dto.getCantidad());
            detalleReal.setPrecioUnitarioCompra(dto.getCostoActual());
            detalleReal.setImpuesto(BigDecimal.ZERO);

            compra.addDetalle(detalleReal);
        }
        compra.setTotal(totalCompra);

        GastoOperativo gasto = compra.getGastoAsociado();
        if (gasto != null) {
            gasto.setMonto(totalCompra);
            gasto.setNotas("Compra de mercancía - Proveedor: " + proveedor.getNombre());
            gastoOperativoRepository.save(gasto);
        }

        compraRepository.save(compra);
    }

}