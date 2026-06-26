package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.VacacionEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VacacionEmpleadoRepository extends JpaRepository<VacacionEmpleado, Long> {

    @Query("SELECT v FROM VacacionEmpleado v WHERE v.empleado = :empleado AND " +
            "(v.fechaInicio <= :finPeriodo AND v.fechaFin >= :inicioPeriodo)")
    List<VacacionEmpleado> encontrarVacacionesEnPeriodo(
            @Param("empleado") Empleado empleado,
            @Param("inicioPeriodo") LocalDate inicioPeriodo,
            @Param("finPeriodo") LocalDate finPeriodo
    );

    List<VacacionEmpleado> findByEmpleadoAndPagadoPorAdelantadoFalse(Empleado empleado);

    boolean existsByEmpleado(Empleado empleado);
}
