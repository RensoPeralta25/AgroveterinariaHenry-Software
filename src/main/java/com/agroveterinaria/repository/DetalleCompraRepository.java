package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DetalleCompra;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {

    Optional<DetalleCompra> findFirstByProductoAndCompraProveedorOrderByCompraFechaHoraCompraDesc(
            Producto producto, Proveedor proveedor
    );

}