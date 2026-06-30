package com.agroveterinaria.service;

import com.agroveterinaria.entity.PeriodoFiscal;
import com.agroveterinaria.repository.PeriodoFiscalRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@RolesAllowed("ADMINISTRADOR")
@Transactional
public class PeriodoFiscalService {
    private final PeriodoFiscalRepository periodoFiscalRepository;

    public List<PeriodoFiscal> findAll() {
        return periodoFiscalRepository.findAll();
    }
}
