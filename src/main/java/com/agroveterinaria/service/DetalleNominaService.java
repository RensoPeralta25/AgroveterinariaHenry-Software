package com.agroveterinaria.service;

import com.agroveterinaria.entity.DetalleNomina;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.repository.DetalleNominaRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class DetalleNominaService {

    private final DetalleNominaRepository detalleNominaRepository;

    public List<DetalleNomina> findByNomina(Nomina nomina) {
        return detalleNominaRepository.findByNomina(nomina);
    }

    public DetalleNomina save(DetalleNomina detalle) {
        return detalleNominaRepository.save(detalle);
    }

    public void delete(DetalleNomina detalle) {
        detalleNominaRepository.delete(detalle);
    }
}
