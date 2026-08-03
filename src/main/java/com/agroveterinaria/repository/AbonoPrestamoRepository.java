package com.agroveterinaria.repository;

import com.agroveterinaria.entity.AbonoPrestamo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AbonoPrestamoRepository extends JpaRepository<AbonoPrestamo, Long> {

    @Query("SELECT SUM(a.monto) FROM AbonoPrestamo a WHERE a.prestamo.idPrestamo = :idPrestamo")
    BigDecimal sumarAbonosPorPrestamo(@Param("idPrestamo") Long idPrestamo);

    boolean existsByReferenciaTransferenciaIgnoreCase(String referenciaTransferencia);

    @EntityGraph(attributePaths = {"empleadoRegistrador", "empleadoRegistrador.persona"})
    List<AbonoPrestamo> findByPrestamo_IdPrestamoOrderByFechaAbonoDesc(Long idPrestamo);
}