package com.agroveterinaria.service;

import com.agroveterinaria.entity.PeriodoFiscal;
import com.agroveterinaria.enums.TipoCorrida;
import com.agroveterinaria.repository.CorridaNominaRepository;
import com.agroveterinaria.repository.PeriodoFiscalRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@RolesAllowed("ADMINISTRADOR")
@Transactional
public class PeriodoFiscalService {
    private final PeriodoFiscalRepository periodoFiscalRepository;
    private final CorridaNominaRepository corridaNominaRepository;

    public List<PeriodoFiscal> findAll() {
        return periodoFiscalRepository.findAll();
    }

    public PeriodoFiscal save(PeriodoFiscal periodoFiscal) {
        return periodoFiscalRepository.save(periodoFiscal);
    }

    public Optional<PeriodoFiscal> buscarPorFecha(LocalDate fecha) {
        return periodoFiscalRepository.findPeriodoActivoPorFecha(fecha);
    }

    public List<PeriodoFiscal> obtenerPeriodosDisponiblesParaBonificacion() {
        List<PeriodoFiscal> periodosCerrados = periodoFiscalRepository.findByCerradoTrue();

        return periodosCerrados.stream()
                .filter(periodo -> !corridaNominaRepository.existsByTipoAndPeriodoFiscal(TipoCorrida.BONIFICACION, periodo))
                .toList();
    }

    public boolean existePeriodoAbierto() {
        return periodoFiscalRepository.findByCerradoFalse().size() > 0;
    }

    public boolean existenPeriodosAnterioresAbiertos(int anio) {
        return periodoFiscalRepository.findByCerradoFalse().stream()
                .anyMatch(p -> p.getAnio() < anio);
    }
}
