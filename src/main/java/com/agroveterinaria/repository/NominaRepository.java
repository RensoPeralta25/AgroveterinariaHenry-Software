package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.enums.PeriodoNomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface NominaRepository extends JpaRepository<Nomina, Long> {

    @Query("SELECT DISTINCT n FROM Nomina n " +
            "JOIN FETCH n.empleado e " +
            "JOIN FETCH e.persona " +
            "LEFT JOIN FETCH n.detalles " +
            "ORDER BY n.fechaEmision DESC")
    List<Nomina> findAllConEmpleadoYDetalles();

    boolean existsByEmpleadoAndPeriodoAndFechaEmisionBetween(
            Empleado empleado,
            PeriodoNomina periodo,
            LocalDate inicio,
            LocalDate fin
    );
}
