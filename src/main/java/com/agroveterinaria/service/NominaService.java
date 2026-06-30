package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.repository.EmpleadoRepository;
import com.agroveterinaria.repository.NominaRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class NominaService {
    private final NominaRepository nominaRepository;
    private final EmpleadoRepository empleadoRepository;

    public List<Nomina> findAllConEmpleadoYDetalles() {
        return nominaRepository.findAllConEmpleadoYDetalles();
    }

    public Nomina generarNomina(Nomina nomina) {
        nomina.calcularSueldoNeto();
        return nominaRepository.save(nomina);
    }

    public void delete(Nomina nomina) {
        nominaRepository.delete(nomina);
    }

    public Optional<Nomina> findById(Long id) {
        return nominaRepository.findById(id);
    }

    public Set<Long> getIdsEmpleadosConHistorial() {
        return nominaRepository.findIdsEmpleadosConHistorial();
    }

    public boolean existsByEmpleado(Empleado empleado){
        return nominaRepository.existsByEmpleado(empleado);
    }

}
