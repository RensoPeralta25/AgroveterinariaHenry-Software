package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.EstadoVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    @EntityGraph(attributePaths = {"cliente.persona"})
    List<Venta> findByEstadoAndLlevaDespachoTrue(EstadoVenta estado);

    @EntityGraph(attributePaths = {"cobros"})
    List<Venta> findByEstado(EstadoVenta estado);

    long countByEstado(EstadoVenta estado);

    List<Venta> findByFechaHoraVentaGreaterThanEqualOrderByFechaHoraVentaAsc(LocalDateTime fechaInicio);

    @Query("SELECT COALESCE(SUM(v.montoTotal), 0) FROM Venta v " +
            "WHERE v.fechaHoraVenta >= :inicio AND v.fechaHoraVenta < :fin")
    BigDecimal sumarMontoEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
