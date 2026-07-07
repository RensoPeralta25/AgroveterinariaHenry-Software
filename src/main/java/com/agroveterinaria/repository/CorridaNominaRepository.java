package com.agroveterinaria.repository;

import com.agroveterinaria.entity.CorridaNomina;
import com.agroveterinaria.entity.PeriodoFiscal;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.PeriodoNomina;
import com.agroveterinaria.enums.TipoCorrida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CorridaNominaRepository extends JpaRepository<CorridaNomina, Long> {

    @Query("SELECT DISTINCT c FROM CorridaNomina c " +
            "LEFT JOIN FETCH c.nominas n " +
            "LEFT JOIN FETCH n.empleado e " +
            "LEFT JOIN FETCH e.persona " +
            "LEFT JOIN FETCH n.detalles " +
            "ORDER BY c.fechaEmision DESC")
    List<CorridaNomina> findAllConNominas();

    boolean existsByTipoAndPeriodoAndFechaEmisionBetween(
            TipoCorrida tipo,
            PeriodoNomina periodo,
            LocalDate inicio,
            LocalDate fin
    );

    @Query("SELECT COUNT(c) > 0 FROM CorridaNomina c WHERE c.tipo = :tipo AND EXTRACT(YEAR FROM c.fechaEmision) = :anio")
    boolean existeCorridaAnualPorTipo(@Param("tipo") TipoCorrida tipo, @Param("anio") int anio);

    boolean existsByTipoAndPeriodoFiscal(TipoCorrida tipo, PeriodoFiscal periodoFiscal);

    @Query("SELECT COUNT(c) FROM CorridaNomina c WHERE c.tipo = :tipo AND EXTRACT(YEAR FROM c.fechaEmision) = :anio")
    long countByTipoAndAnio(@Param("tipo") TipoCorrida tipo, @Param("anio") int anio);

    boolean existsByEstado(EstadoCorrida estado);

    boolean existsByPeriodoAndFechaEmisionAndTipo(PeriodoNomina periodo, LocalDate fechaEmision, TipoCorrida tipo);
}
