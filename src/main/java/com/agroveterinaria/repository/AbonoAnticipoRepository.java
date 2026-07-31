package com.agroveterinaria.repository;

import com.agroveterinaria.entity.AbonoAnticipo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonoAnticipoRepository extends JpaRepository<AbonoAnticipo, Long> {

    boolean existsByReferenciaTransferenciaIgnoreCase(String referenciaTransferencia);

    @EntityGraph(attributePaths = {"empleadoRegistrador", "empleadoRegistrador.persona"})
    List<AbonoAnticipo> findByAnticipoSalario_IdOrderByFechaAbonoDesc(Long idAnticipo);
}