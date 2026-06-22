package com.agroveterinaria.service;

import com.agroveterinaria.entity.EmbargoSalarial;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.repository.EmbargoSalarialRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
@RolesAllowed("ADMINISTRADOR")
@Transactional
public class EmbargoSalarialService {
    private final EmbargoSalarialRepository embargoSalarialRepository;

    public EmbargoSalarial save(EmbargoSalarial embargo) {
        return embargoSalarialRepository.save(embargo);
    }

    public List<EmbargoSalarial> findByEmpleadoAndActivoTrue(Empleado empleado) {
        return embargoSalarialRepository.findByEmpleadoAndActivoTrue(empleado);
    }
}
