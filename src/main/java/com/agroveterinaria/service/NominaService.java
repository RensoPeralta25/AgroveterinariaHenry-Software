package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.enums.PeriodoNomina;
import com.agroveterinaria.repository.EmpleadoRepository;
import com.agroveterinaria.repository.NominaRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class NominaService {
    private final NominaRepository nominaRepository;
    private final EmpleadoRepository empleadoRepository;

    public List<Nomina> findAll() {
        return nominaRepository.findAllConEmpleadoYDetalles();
    }

    public Nomina generarNomina(Nomina nomina) {
        nomina.calcularSueldoNeto();
        return nominaRepository.save(nomina);
    }

    public void delete(Nomina nomina) {
        nominaRepository.delete(nomina);
    }

    public boolean existeNominaEnPeriodo(Empleado empleado, PeriodoNomina periodo, LocalDate fecha) {
        LocalDate inicio = fecha.withDayOfMonth(1);
        LocalDate fin = fecha.withDayOfMonth(fecha.lengthOfMonth());
        return nominaRepository.existsByEmpleadoAndPeriodoAndFechaEmisionBetween(empleado, periodo, inicio, fin);
    }

    public Optional<Nomina> buscarPorId(Long id) {
        return nominaRepository.findById(id);
    }

}
