package com.agroveterinaria.repository;

import com.agroveterinaria.entity.CorridaNomina;
import com.agroveterinaria.enums.PeriodoNomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    boolean existsByPeriodoAndFechaEmisionBetween(
            PeriodoNomina periodo,
            LocalDate inicio,
            LocalDate fin
    );
}
