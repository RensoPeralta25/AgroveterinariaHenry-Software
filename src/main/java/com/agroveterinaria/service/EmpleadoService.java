package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.repository.EmpleadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpleadoService {
    private final EmpleadoRepository empleadoRepository;
    private final PersonaService personaService;

    public EmpleadoService (EmpleadoRepository empleadoRepository, PersonaService personaService){
        this.empleadoRepository = empleadoRepository;
        this.personaService = personaService;
    }

    public List<Empleado> findAll() { return empleadoRepository.findAll(); }

    @Transactional
    public Empleado save(Empleado emp){
        Persona personaForm = emp.getPersona();

        if (personaForm != null && personaForm.getCedula() != null) {
            personaService
                    .findByCedula(personaForm.getCedula())
                    .ifPresentOrElse(
                            emp::setPersona,
                            () -> {
                                Persona personaGuardada = personaService.save(personaForm);
                                emp.setPersona(personaGuardada);
                            }
                    );
        }

        return empleadoRepository.save(emp);
    }

}
