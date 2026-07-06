package com.agroveterinaria.repository;

import com.agroveterinaria.entity.PeriodoFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PeriodoFiscalRepository extends JpaRepository<PeriodoFiscal, Long> {

    List<PeriodoFiscal> findByCerradoTrue();

    @Query("SELECT p FROM PeriodoFiscal p WHERE :fecha BETWEEN p.fechaInicio AND p.fechaCierre")
    Optional<PeriodoFiscal> findPeriodoActivoPorFecha(@Param("fecha") LocalDate fecha);


    List<PeriodoFiscal> findByCerradoFalse();
}
