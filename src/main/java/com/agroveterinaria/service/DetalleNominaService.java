package com.agroveterinaria.service;

import com.agroveterinaria.entity.CorridaNomina;
import com.agroveterinaria.entity.DetalleNomina;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.repository.DetalleNominaRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed({"ADMINISTRADOR", "RECURSOS_HUMANOS"})
public class DetalleNominaService {

    private final DetalleNominaRepository detalleNominaRepository;

    public List<DetalleNomina> findByNomina(Nomina nomina) {
        return detalleNominaRepository.findByNomina(nomina);
    }

    public DetalleNomina save(DetalleNomina detalle) {
        validarEstadoCorrida(detalle.getNomina().getCorrida());
        return detalleNominaRepository.save(detalle);
    }

    public void delete(DetalleNomina detalle) {
        validarEstadoCorrida(detalle.getNomina().getCorrida());
        detalleNominaRepository.delete(detalle);
    }

    private void validarEstadoCorrida(CorridaNomina corrida) {
        if (corrida == null || corrida.getEstado() != EstadoCorrida.PENDIENTE) {
            throw new IllegalStateException("No se pueden modificar los detalles de una corrida que ya fue aprobada.");
        }
    }
}
