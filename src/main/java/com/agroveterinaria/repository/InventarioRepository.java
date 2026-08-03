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

    @Query("SELECT COUNT(p) FROM Producto p " +
            "WHERE p.status = com.agroveterinaria.enums.StatusEntidad.ACTIVO " +
            "AND p.categoria <> com.agroveterinaria.enums.CategoriaProducto.SERVICIO " +
            "AND COALESCE((SELECT SUM(i.cantidadActual) FROM Inventario i WHERE i.lote.producto = p), 0) <= :stockMinimo")
    long contarProductosActivosConStockBajo(@Param("stockMinimo") BigDecimal stockMinimo);

    @Query("SELECT i.lote.producto.categoria, COUNT(DISTINCT i.lote.producto.idProducto) FROM Inventario i " +
            "WHERE i.lote.producto.status = com.agroveterinaria.enums.StatusEntidad.ACTIVO " +
            "AND i.cantidadActual > 0 " +
            "GROUP BY i.lote.producto.categoria " +
            "ORDER BY COUNT(DISTINCT i.lote.producto.idProducto) DESC")
    List<Object[]> contarProductosConStockPorCategoria();

    Optional<Inventario> findByAlmacenAndLote(Almacen almacen, Lote lote);

    @Query("SELECT i.lote FROM Inventario i WHERE i.almacen = :almacen AND i.lote.producto = :producto AND i.cantidadActual > 0")
    List<Lote> findLotesConStock(@Param("almacen") Almacen almacen, @Param("producto") Producto producto);

    @Query("SELECT i FROM Inventario i WHERE i.almacen = :almacen AND i.lote.producto = :producto ORDER BY i.lote.fechaVencimiento ASC")
    List<Inventario> findByAlmacenAndProductoOrderByLote_FechaVencimientoAsc(
            @Param("almacen") Almacen almacen,
            @Param("producto") Producto producto
    );
}
