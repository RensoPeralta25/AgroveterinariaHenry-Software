package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DetalleCompra;
import com.agroveterinaria.entity.DetalleRecepcion;
import com.agroveterinaria.entity.DetalleTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface DetalleRecepcionRepository extends JpaRepository<DetalleRecepcion, Long> {

    @Query("SELECT COALESCE(SUM(dr.cantidad + dr.cantidadMerma), 0) FROM DetalleRecepcion dr WHERE dr.detalleCompra = :dc")
    BigDecimal sumCantidadProcesadaByDetalleCompra(@Param("dc") DetalleCompra dc);

    @Query("SELECT COALESCE(SUM(dr.cantidad + dr.cantidadMerma), 0) FROM DetalleRecepcion dr WHERE dr.detalleTransferencia = :dt")
    BigDecimal sumCantidadProcesadaByDetalleTransferencia(@Param("dt") DetalleTransferencia dt);
}