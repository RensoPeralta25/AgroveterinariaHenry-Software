package com.agroveterinaria.service;

import com.agroveterinaria.entity.DiaFeriado;
import com.agroveterinaria.repository.DiaFeriadoRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Service
@RolesAllowed({"ADMINISTRADOR", "RECURSOS_HUMANOS"})
public class DiaFeriadoService {
    private final DiaFeriadoRepository diaFeriadoRepository;

    public DiaFeriado save(DiaFeriado diaFeriado){ return diaFeriadoRepository.save(diaFeriado); }

    public List<DiaFeriado> findAll(){ return diaFeriadoRepository.findAll(); }

    public void delete(DiaFeriado diaFeriado){ diaFeriadoRepository.delete(diaFeriado);}

    public List<LocalDate> obtenerFechasFeriadasEnRango(LocalDate inicio, LocalDate fin) {
        return diaFeriadoRepository.findFechasBetween(inicio, fin);
    }
}
