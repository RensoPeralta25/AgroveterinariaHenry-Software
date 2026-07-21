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
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    @EntityGraph(attributePaths = {"cliente.persona"})
    List<Venta> findByEstadoAndLlevaDespachoTrue(EstadoVenta estado);

    @EntityGraph(attributePaths = {"cobros"})
    List<Venta> findConCobrosByEstado(EstadoVenta estado);

    @EntityGraph(attributePaths = {
            "detallesVentas",
            "detallesVentas.producto",
            "detallesVentas.lote"
    })
    Optional<Venta> findVentaConDetallesByIdVenta(Long idVenta);

    List<Venta> findByEstado(EstadoVenta estado);

    long countByEstado(EstadoVenta estado);

    List<Venta> findByFechaHoraVentaGreaterThanEqualOrderByFechaHoraVentaAsc(LocalDateTime fechaInicio);

    @Query("SELECT COALESCE(SUM(v.montoTotal), 0) FROM Venta v " +
            "WHERE v.fechaHoraVenta >= :inicio AND v.fechaHoraVenta < :fin")
    BigDecimal sumarMontoEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @EntityGraph(attributePaths = {"cliente.persona"})
    @Query("SELECT DISTINCT v FROM Venta v JOIN v.detallesVentas dv " +
            "WHERE v.llevaDespacho = true " +
            "AND dv.cantidad > (SELECT COALESCE(SUM(dd.cantidad), 0) FROM DetalleDespacho dd WHERE dd.detalleVenta = dv)")
    List<Venta> findVentasConMercanciaPendienteDeDespacho();
}
