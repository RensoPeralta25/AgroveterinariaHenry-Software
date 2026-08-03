package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DetalleDevVenta;
import com.agroveterinaria.entity.DevolucionVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DetalleDevVentaRepository extends JpaRepository<DetalleDevVenta, Long> {

    @Query("SELECT COALESCE(SUM(d.cantidadDevuelta), 0) " +
            "FROM DetalleDevVenta d " +
            "WHERE d.detalleVenta.idDetalleVenta = :idDetalleVenta " +
            "AND d.devolucionVenta.estado = 'COMPLETADA'")
    BigDecimal sumarCantidadesDevueltasPorDetalleVenta(@Param("idDetalleVenta") Long idDetalleVenta);

    @EntityGraph(attributePaths = {
            "detalleVenta",
            "detalleVenta.producto",
            "lote",
            "almacenEntrada"
    })
    List<DetalleDevVenta> findByDevolucionVentaIdDevolucionVenta(Long idDevolucionVenta);

    @EntityGraph(attributePaths = {"cliente", "cliente.persona", "empleado", "empleado.persona"})
    @Query("SELECT d FROM DevolucionVenta d")
    List<DevolucionVenta> findAllConRelaciones();

    @Query("SELECT COALESCE(SUM(d.cantidadDevuelta), 0) FROM DetalleDevVenta d WHERE d.detalleVenta.idDetalleVenta = :idDetalle AND d.lote.idLote = :idLote")
    BigDecimal sumarCantidadesDevueltasPorDetalleVentaAndLote(@Param("idDetalle") Long idDetalle, @Param("idLote") Long idLote);

    @Query("SELECT COALESCE(SUM(d.cantidadDevuelta), 0) FROM DetalleDevVenta d WHERE d.detalleVenta.idDetalleVenta = :idDetalle AND d.lote IS NULL")
    BigDecimal sumarCantidadesDevueltasPorDetalleVentaSinLote(@Param("idDetalle") Long idDetalle);
}