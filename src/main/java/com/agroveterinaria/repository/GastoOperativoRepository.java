package com.agroveterinaria.repository;

import com.agroveterinaria.entity.GastoOperativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GastoOperativoRepository extends JpaRepository<GastoOperativo, Long> {
    List<GastoOperativo> findByFechaBetweenOrderByFechaDesc(LocalDate inicio, LocalDate fin);
    List<GastoOperativo> findAllByOrderByFechaDesc();
}