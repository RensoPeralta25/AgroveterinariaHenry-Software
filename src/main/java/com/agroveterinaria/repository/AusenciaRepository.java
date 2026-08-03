package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Ausencia;
import com.agroveterinaria.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

public interface AusenciaRepository extends JpaRepository<Ausencia, Long> {

    @Query("SELECT a FROM Ausencia a JOIN FETCH a.empleado e JOIN FETCH e.persona")
    List<Ausencia> findAllConRelaciones();

    @Query("SELECT a FROM Ausencia a WHERE a.empleado.idEmpleado = :idEmpleado " +
            "AND (a.aplicadaEnNomina = false OR a.estadoRegistro = :estadoAbierta)")
    List<Ausencia> findAusenciasPendientesPorEmpleado(@Param("idEmpleado") Long idEmpleado,
                                                      @Param("estadoAbierta") EstadoRegistro estadoAbierta);

    @Query("SELECT a FROM Ausencia a WHERE a.empleado.idEmpleado = :idEmpleado " +
            "AND a.fechaInicio <= :finPeriodo " +
            "AND (a.fechaFin IS NULL OR a.fechaFin >= :inicioPeriodo)")
    List<Ausencia> findAusenciasEnRango(@Param("idEmpleado") Long idEmpleado,
                                        @Param("inicioPeriodo") LocalDate inicioPeriodo,
                                        @Param("finPeriodo") LocalDate finPeriodo);

    @Query("SELECT COUNT(a) FROM Ausencia a WHERE a.empleado.idEmpleado = :idEmpleado " +
            "AND a.fechaInicio <= :finValidacion " +
            "AND (a.fechaFin IS NULL OR a.fechaFin >= :inicioValidacion) " +
            "AND (:idAusencia IS NULL OR a.id != :idAusencia)")
    long countAusenciasSolapadas(@Param("idEmpleado") Long idEmpleado,
                                 @Param("inicioValidacion") LocalDate inicioValidacion,
                                 @Param("finValidacion") LocalDate finValidacion,
                                 @Param("idAusencia") Long idAusencia);
}