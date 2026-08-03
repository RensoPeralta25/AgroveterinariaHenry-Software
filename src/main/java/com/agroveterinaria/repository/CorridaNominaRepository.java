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
import java.util.Optional;

public interface CorridaNominaRepository extends JpaRepository<CorridaNomina, Long> {

    @Query("SELECT DISTINCT c FROM CorridaNomina c " +
            "LEFT JOIN FETCH c.nominas n " +
            "LEFT JOIN FETCH n.empleado e " +
            "LEFT JOIN FETCH e.persona " +
            "LEFT JOIN FETCH n.detalles " +
            "ORDER BY c.fechaEmision DESC")
    List<CorridaNomina> findAllConNominas();

    @Query("SELECT COUNT(c) > 0 FROM CorridaNomina c WHERE c.tipo = :tipo AND EXTRACT(YEAR FROM c.fechaEmision) = :anio")
    boolean existeCorridaAnualPorTipo(@Param("tipo") TipoCorrida tipo, @Param("anio") int anio);

    boolean existsByTipoAndPeriodoFiscal(TipoCorrida tipo, PeriodoFiscal periodoFiscal);

    @Query("SELECT COUNT(c) FROM CorridaNomina c WHERE c.tipo = :tipo AND EXTRACT(YEAR FROM c.fechaEmision) = :anio")
    long countByTipoAndAnio(@Param("tipo") TipoCorrida tipo, @Param("anio") int anio);

    boolean existsByEstado(EstadoCorrida estado);

    @Query("SELECT COUNT(c) > 0 FROM CorridaNomina c WHERE c.tipo = :tipo AND ((c.fechaInicio <= :fin) AND (c.fechaFin >= :inicio))")
    boolean existeSolapamiento(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin, @Param("tipo") TipoCorrida tipo);

    boolean existsByTipoAndPeriodoAndFechaInicioAndFechaFin(TipoCorrida tipo, PeriodoNomina periodo, LocalDate fechaInicio, LocalDate fechaFin);

    boolean existsByTipoAndFechaFinBetween(TipoCorrida tipo, LocalDate inicio, LocalDate fin);

    Optional<CorridaNomina> findTopByPeriodoAndEstadoAndTipoOrderByFechaFinDesc(PeriodoNomina periodo, EstadoCorrida estado, TipoCorrida tipo);

    @Query("SELECT COUNT(c) > 0 FROM CorridaNomina c WHERE c.tipo = :tipo AND MONTH(c.fechaInicio) = :mes AND YEAR(c.fechaInicio) = :anio AND c.periodo != :periodo")
    boolean existeOtraModalidadEnMesDeInicio(@Param("tipo") TipoCorrida tipo, @Param("mes") int mes, @Param("anio") int anio, @Param("periodo") PeriodoNomina periodo);

    Optional<CorridaNomina> findTopByTipoAndEstadoOrderByFechaFinDesc(TipoCorrida tipo, EstadoCorrida estado);

    @Query("SELECT COUNT(c) > 0 FROM CorridaNomina c WHERE c.tipo = :tipo AND c.periodo = :periodo AND c.estado = 'APROBADA' AND MONTH(c.fechaInicio) = :mes AND YEAR(c.fechaInicio) = :anio AND DAY(c.fechaInicio) <= 15")
    boolean existePrimeraQuincenaEnMes(
            @org.springframework.data.repository.query.Param("tipo") TipoCorrida tipo,
            @org.springframework.data.repository.query.Param("periodo") PeriodoNomina periodo,
            @org.springframework.data.repository.query.Param("mes") int mes,
            @org.springframework.data.repository.query.Param("anio") int anio
    );
}
