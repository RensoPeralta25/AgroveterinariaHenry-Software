package com.agroveterinaria.repository;

import com.agroveterinaria.entity.HistorialDevengadoAnual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface HistorialDevengadoAnualRepository extends JpaRepository<HistorialDevengadoAnual, Long> {

    Optional<HistorialDevengadoAnual> findByEmpleadoIdEmpleadoAndAnioAndMes(Long idEmpleado, int anio, int mes);

    @Query("SELECT SUM(h.montoDevengadoReal) FROM HistorialDevengadoAnual h WHERE h.empleado.idEmpleado = :idEmpleado AND h.anio = :anio")
    BigDecimal sumarDevengadoAnualPorEmpleado(@Param("idEmpleado") Long idEmpleado, @Param("anio") int anio);
}