package com.agroveterinaria.repository;

import com.agroveterinaria.entity.AbonoPrestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface AbonoPrestamoRepository extends JpaRepository<AbonoPrestamo, Long> {

    @Query("SELECT SUM(a.monto) FROM AbonoPrestamo a WHERE a.prestamo.idPrestamo = :idPrestamo")
    BigDecimal sumarAbonosPorPrestamo(@Param("idPrestamo") Long idPrestamo);
}