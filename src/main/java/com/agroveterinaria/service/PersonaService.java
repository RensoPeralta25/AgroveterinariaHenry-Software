package com.agroveterinaria.service;

import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.repository.PersonaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PersonaService {
    private final PersonaRepository personaRepository;

    public PersonaService (PersonaRepository personaRepository){ this.personaRepository = personaRepository; }

    public Optional<Persona> findByCedula(String cedula) { return personaRepository.findByCedula(cedula); }

    public Persona save(Persona p) { return personaRepository.save(p); }

    public List<Persona> findAll(){ return personaRepository.findAll(); }

    public void delete(Persona p){ personaRepository.delete(p); }
}
