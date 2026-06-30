package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DiaFeriado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DiaFeriadoRepository extends JpaRepository<DiaFeriado, Long> {

    @Query("SELECT d.fecha FROM DiaFeriado d WHERE d.fecha >= :inicio AND d.fecha <= :fin")
    List<LocalDate> findFechasBetween(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}
