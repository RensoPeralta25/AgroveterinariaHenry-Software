package com.agroveterinaria.repository;

import com.agroveterinaria.entity.GastoOperativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GastoOperativoRepository extends JpaRepository<GastoOperativo, Long> {
    List<GastoOperativo> findByFechaBetweenOrderByFechaDesc(LocalDate inicio, LocalDate fin);
    List<GastoOperativo> findAllByOrderByFechaDesc();

    List<GastoOperativo> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate fechaInicio);

    @Query("""
            SELECT COALESCE(SUM(g.monto), 0)
            FROM GastoOperativo g
            WHERE g.fecha >= :inicio AND g.fecha < :fin
            """)
    BigDecimal sumarMontoEntre(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin
    );
}
