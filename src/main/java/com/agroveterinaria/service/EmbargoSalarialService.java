package com.agroveterinaria.service;

import com.agroveterinaria.entity.CuotaExtraEmbargo;
import com.agroveterinaria.entity.EmbargoSalarial;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.repository.EmbargoSalarialRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@Service
@RolesAllowed("ADMINISTRADOR")
@Transactional
public class EmbargoSalarialService {
    private final EmbargoSalarialRepository embargoSalarialRepository;

    public EmbargoSalarial save(EmbargoSalarial embargo) {
        if (embargo.getMontoCuotaOrdinaria().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La cuota del embargo debe ser mayor a cero.");
        }

        if (embargo.getCuotasExtras() != null) {
            for (CuotaExtraEmbargo cuota : embargo.getCuotasExtras()) {
                if (cuota.getMontoExtra() == null || cuota.getMontoExtra().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("El monto de la cuota extraordinaria no puede ser negativo.");
                }
            }
        }

        return embargoSalarialRepository.save(embargo);
    }

    public List<EmbargoSalarial> findAll() {
        return embargoSalarialRepository.findAll();
    }

    public List<EmbargoSalarial> findAllParaVista() {
        return embargoSalarialRepository.findAllParaVista();
    }

    public boolean existsByEmpleado(Empleado empleado){
        return embargoSalarialRepository.existsByEmpleado(empleado);
    }

    public List<EmbargoSalarial> findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(Empleado empleado) {
        return embargoSalarialRepository.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(empleado, StatusEntidad.ACTIVO);
    }
}
