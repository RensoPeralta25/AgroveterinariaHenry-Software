package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DetalleDevVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface DetalleDevVentaRepository extends JpaRepository<DetalleDevVenta, Long> {

    @Query("SELECT COALESCE(SUM(d.cantidadDevuelta), 0) " +
            "FROM DetalleDevVenta d " +
            "WHERE d.detalleVenta.idDetalleVenta = :idDetalleVenta " +
            "AND d.devolucionVenta.estado = 'COMPLETADA'")
    BigDecimal sumarCantidadesDevueltasPorDetalleVenta(@Param("idDetalleVenta") Long idDetalleVenta);
}