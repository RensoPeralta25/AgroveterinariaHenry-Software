package com.agroveterinaria.service;

import com.agroveterinaria.dto.detalle_compra.DetalleCompraDTO;
import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.repository.CompraRepository;
import com.agroveterinaria.repository.DetalleCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;

    public CompraService(CompraRepository compraRepository, DetalleCompraRepository detalleCompraRepository) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
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
    public void registrarCompra(Proveedor proveedor, List<DetalleCompraDTO> detalles) {
        //Implementar en ticket AHS-58
    }

}