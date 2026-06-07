package com.agroveterinaria.service;

import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.repository.PersonaRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class PersonaService {
    private final PersonaRepository personaRepository;

    public PersonaService (PersonaRepository personaRepository){ this.personaRepository = personaRepository; }

    @RolesAllowed({"ADMINISTRADOR", "VETERINARIO", "CAJERO"})
    public Optional<Persona> findByCedula(String cedula) { return personaRepository.findByCedula(cedula); }

    @RolesAllowed({"ADMINISTRADOR", "VETERINARIO", "CAJERO"})
    public Persona save(Persona p) { return personaRepository.save(p); }

    @RolesAllowed({"ADMINISTRADOR", "VETERINARIO", "CAJERO"})
    public List<Persona> findAll(){ return personaRepository.findAll(); }

    @RolesAllowed("ADMINISTRADOR")
    public void delete(Persona p){ personaRepository.delete(p); }
}
