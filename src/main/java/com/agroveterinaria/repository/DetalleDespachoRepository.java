package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Despacho;
import com.agroveterinaria.entity.DetalleDespacho;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface DetalleDespachoRepository extends JpaRepository<DetalleDespacho, Long> {
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) " +
            "FROM DetalleDespacho d " +
            "WHERE d.detalleTransferencia.idDetalleTransferencia = :id")
    BigDecimal sumCantidadByIdDetalleTransferencia(@Param("id") Long idDetalleTransferencia);

    @Query("SELECT SUM(d.cantidad) FROM DetalleDespacho d WHERE d.detalleVenta.idDetalleVenta = :id")
    BigDecimal sumCantidadByIdDetalleVenta(@Param("id") Long id);
}