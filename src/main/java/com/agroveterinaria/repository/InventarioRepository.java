package com.agroveterinaria.repository;

import com.agroveterinaria.dto.inventario.InventarioGlobalDTO;
import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Inventario;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    @Query("SELECT new com.agroveterinaria.dto.inventario.InventarioGlobalDTO(i.lote.producto, SUM(i.cantidadActual)) " +
            "FROM Inventario i " +
            "WHERE i.lote.producto.status = 'ACTIVO' " +
            "GROUP BY i.lote.producto " +
            "HAVING SUM(i.cantidadActual) > 0")
    List<InventarioGlobalDTO> obtenerInventarioGlobalConsolidado();

    @Query("SELECT i FROM Inventario i " +
            "JOIN FETCH i.almacen " +
            "JOIN FETCH i.lote l " +
            "WHERE l.producto = :producto AND i.cantidadActual > 0 " +
            "ORDER BY l.fechaVencimiento ASC NULLS LAST")
    List<Inventario> buscarDesglosePorProducto(@Param("producto") Producto producto);

    @Query("SELECT i FROM Inventario i JOIN FETCH i.lote l JOIN FETCH l.producto WHERE i.almacen = :almacen")
    List<Inventario> findByAlmacen(@Param("almacen") Almacen almacen);

    @Query("SELECT COALESCE(SUM(i.cantidadActual), 0) FROM Inventario i WHERE i.lote.producto = :producto")
    BigDecimal sumarStockPorProducto(@Param("producto") Producto producto);

    Optional<Inventario> findByAlmacenAndLote(Almacen almacen, Lote lote);

}
