package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.VacacionEmpleado;
import com.agroveterinaria.enums.EstadoVacacion;
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

    List<VacacionEmpleado> findByEmpleadoAndEstadoNot(Empleado empleado, EstadoVacacion estado);

    boolean existsByEmpleado(Empleado empleado);

    @Query("SELECT COALESCE(SUM(v.cantidadDiasDescanso), 0) FROM VacacionEmpleado v " +
            "WHERE v.empleado.idEmpleado = :empleadoId " +
            "AND v.fechaInicio >= :inicioAniversario " +
            "AND v.fechaInicio < :finAniversario")
    int sumDiasDisfrutadosEnPeriodo(
            @Param("empleadoId") Long empleadoId,
            @Param("inicioAniversario") LocalDate inicioAniversario,
            @Param("finAniversario") LocalDate finAniversario
    );

    @Query("SELECT COUNT(v) > 0 FROM VacacionEmpleado v " +
            "WHERE v.empleado = :empleado " +
            "AND v.fechaInicio <= :fin " +
            "AND v.fechaFin >= :inicio " +
            "AND (:id IS NULL OR v.id != :id)")
    boolean existeInterseccionFechas(
            @Param("empleado") Empleado empleado,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("id") Long id
    );
}
