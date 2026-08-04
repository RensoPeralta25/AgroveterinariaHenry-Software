package com.agroveterinaria.repository;

import com.agroveterinaria.entity.AbonoAnticipo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AbonoAnticipoRepository extends JpaRepository<AbonoAnticipo, Long> {

    boolean existsByReferenciaTransferenciaIgnoreCase(String referenciaTransferencia);

    @EntityGraph(attributePaths = {"empleadoRegistrador", "empleadoRegistrador.persona"})
    List<AbonoAnticipo> findByAnticipoSalario_IdOrderByFechaAbonoDesc(Long idAnticipo);

    @Query("SELECT SUM(a.monto) FROM AbonoAnticipo a WHERE a.fechaAbono >= :inicio AND a.fechaAbono < :fin")
    BigDecimal sumarAbonosEntre(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}