package com.agroveterinaria.repository;

import com.agroveterinaria.entity.PeriodoFiscal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodoFiscalRepository extends JpaRepository<PeriodoFiscal, Long> {
    
    List<PeriodoFiscal> findByCerradoFalseOrderByAnioDesc();
}
