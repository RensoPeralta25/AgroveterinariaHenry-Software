package com.agroveterinaria.service;

import com.agroveterinaria.entity.Mascota;
import com.agroveterinaria.repository.MascotaRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RolesAllowed({"ADMINISTRADOR","VETERINARIO"})
public class MascotaService {

    private final MascotaRepository mascotaRepository;

    public MascotaService (MascotaRepository mascotaRepository) {
        this.mascotaRepository = mascotaRepository;
    }

    @Transactional(readOnly = true)
    public List<Mascota> findAll() {
        return mascotaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Mascota> findMascotaById(Long idMascota) {
        return mascotaRepository.findByIdMascota(idMascota);
    }

    @Transactional(readOnly = true)
    public List<Mascota> findMascotaByIdCliente (Long idCliente) {
        return mascotaRepository.findByClienteIdCliente(idCliente);
    }

    public Mascota save (Mascota mascota) {
        validar(mascota);
        return mascotaRepository.save(mascota);
    }

    public void delete (Mascota mascota) {
        mascotaRepository.delete(mascota);
    }


    public void validar (Mascota mascota) {
        if (mascota == null) {
            throw new IllegalArgumentException("La mascota es obligatoria");
        }

        if (mascota.getCliente() == null) {
            throw new IllegalArgumentException("La mascota debe tener un cliente");
        }

        if (mascota.getNombre() == null || mascota.getNombre().isBlank()) {
            throw new IllegalArgumentException("La mascota debe tener un nombre");
        }

        if (mascota.getSexo() == null || mascota.getSexo().isBlank()) {
            throw new IllegalArgumentException("La mascota debe tener un sexo");
        }

        if (mascota.getRaza() == null || mascota.getRaza().isBlank()) {
            throw new IllegalArgumentException("La mascota debe tener una raza");
        }

        if (mascota.getFechaNacimiento() == null) {
            throw new IllegalArgumentException("La mascota debe tener fecha de nacimiento");
        }

        if (mascota.getTipoAnimal() == null) {
            throw new IllegalArgumentException("La mascota debe tener un tipo de animal");
        }

    }
}
