package com.agroveterinaria.service;

import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Inventario;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.repository.AlmacenRepository;
import com.agroveterinaria.repository.InventarioRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RolesAllowed("ADMINISTRADOR")
public class InventarioService {

    private final InventarioRepository inventarioRepository;

    public InventarioService(InventarioRepository inventarioRepository) {
        this.inventarioRepository = inventarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Inventario> listarTodos() {
        return inventarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Inventario> buscarPorId(Long idInventario) {
        return inventarioRepository.findById(idInventario);
    }

    @Transactional
    public Inventario guardar(Inventario inventario) {
        return inventarioRepository.save(inventario);
    }

    @Transactional
    public void eliminarPorId(Long idInventario) {
        inventarioRepository.deleteById(idInventario);
    }

    @Transactional
    public void eliminar(Inventario inventario) {
        inventarioRepository.delete(inventario);
    }

    @Transactional(readOnly = true)
    public List<Inventario> listarPorAlmacen(Almacen almacen) {
        return inventarioRepository.findByAlmacen(almacen);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerStockTotal(Producto producto) {
        if (producto == null || producto.getIdProducto() == null) {
            return BigDecimal.ZERO;
        }
        return inventarioRepository.sumarStockPorProducto(producto);
    }

}