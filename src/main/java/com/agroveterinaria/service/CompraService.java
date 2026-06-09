package com.agroveterinaria.service;

import com.agroveterinaria.dto.detalle_compra.DetalleCompraDTO;
import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.entity.DetalleCompra;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.enums.EstadoRecepcion;
import com.agroveterinaria.repository.CompraRepository;
import com.agroveterinaria.repository.DetalleCompraRepository;
import com.agroveterinaria.repository.DetalleRecepcionRepository;
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

    public CompraService(CompraRepository compraRepository, DetalleCompraRepository detalleCompraRepository, DetalleRecepcionRepository detalleRecepcionRepository) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.detalleRecepcionRepository = detalleRecepcionRepository;
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
        compraRepository.deleteById(idCompra);
    }

    @Transactional
    public void eliminar(Compra compra) {
        compraRepository.delete(compra);
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
        // El estadoRecepcion ya es pendiente por defecto en la entidad

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

        compraRepository.save(nuevaCompra);
    }

    public List<Compra> listarComprasPendientes() {
        return compraRepository.findByEstadoRecepcionIn(List.of(EstadoRecepcion.PENDIENTE, EstadoRecepcion.PARCIAL));
    }

    public List<DetalleCompra> obtenerDetallesPorCompra(Long idCompra) {
        return detalleCompraRepository.findByCompra_IdCompra(idCompra);
    }

    public BigDecimal calcularCantidadPendiente(DetalleCompra detalleCompra) {
        BigDecimal yaRecibido = detalleRecepcionRepository.sumCantidadRecibidaByDetalleCompra(detalleCompra);
        return detalleCompra.getCantidad().subtract(yaRecibido);
    }

}