package com.agroveterinaria.service;

import com.agroveterinaria.entity.CorridaNomina;
import com.agroveterinaria.entity.CuotaExtraEmbargo;
import com.agroveterinaria.entity.EmbargoSalarial;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.EstadoEmbargo;
import com.agroveterinaria.repository.CorridaNominaRepository;
import com.agroveterinaria.repository.EmbargoSalarialRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
@RolesAllowed("ADMINISTRADOR")
@Transactional
public class EmbargoSalarialService {
    private final EmbargoSalarialRepository embargoSalarialRepository;
    private final CorridaNominaRepository corridaNominaRepository;

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

        if (corridaNominaRepository.existsByEstado(EstadoCorrida.PENDIENTE)) {
            throw new IllegalStateException("Operación denegada: Existe una corrida de nómina en estado PENDIENTE. "
                    + "Debe aprobar o eliminar la corrida actual antes de registrar o modificar embargos.");
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
        return embargoSalarialRepository.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(empleado, EstadoEmbargo.ACTIVO);
    }

    public EmbargoSalarial cambiarEstado(EmbargoSalarial embargo, EstadoEmbargo nuevoEstado) {
        if (embargo.getEstado() == EstadoEmbargo.INACTIVO) {
            throw new IllegalStateException("Un embargo cerrado Inactivo es de solo lectura y no puede ser reactivado. Para un nuevo caso, registre un nuevo embargo.");
        }

        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo.");
        }

        embargo.setEstado(nuevoEstado);
        return embargoSalarialRepository.save(embargo);
    }
}
