package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DevolucionVenta;
import com.agroveterinaria.entity.NotaDeCredito;
import com.agroveterinaria.enums.EstadoDevolucion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DevolucionVentaRepository extends JpaRepository<DevolucionVenta, Long> {
    boolean existsByNotaDeCredito(NotaDeCredito notaDeCredito);

    @EntityGraph(attributePaths = {
            "cliente",
            "cliente.persona",
            "empleado",
            "empleado.persona",
            "notaDeCredito"
    })
    @Query("SELECT d FROM DevolucionVenta d")
    List<DevolucionVenta> findAllConRelaciones();

    List<DevolucionVenta> findByFechaHoraGreaterThanEqualAndEstadoOrderByFechaHoraAsc(
            LocalDateTime fechaInicio,
            EstadoDevolucion estado
    );

    @Query("SELECT COALESCE(SUM(d.montoTotal), 0) FROM DevolucionVenta d " +
            "WHERE d.fechaHora >= :inicio AND d.fechaHora < :fin " +
            "AND d.estado = :estado")
    BigDecimal sumarMontoEntre(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoDevolucion estado
    );
}
