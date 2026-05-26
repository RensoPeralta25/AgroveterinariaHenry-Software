package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.repository.EmpleadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmpleadoService {
    private final EmpleadoRepository empleadoRepository;
    private final PersonaService personaService;

    public EmpleadoService (EmpleadoRepository empleadoRepository, PersonaService personaService){
        this.empleadoRepository = empleadoRepository;
        this.personaService = personaService;
    }

    public List<Empleado> findAll() { return empleadoRepository.findAll(); }

    public Empleado save(Empleado emp){
        validar(emp);

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

    public Empleado update(Empleado emp){
        validar(emp);

        Empleado empleadoExistente = empleadoRepository.findById(emp.getIdEmpleado())
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));

        Persona personaExistente = empleadoExistente.getPersona();
        Persona personaForm = emp.getPersona();

        if (personaExistente != null && personaForm != null) {
            personaExistente.setNombre(personaForm.getNombre());
            personaExistente.setTelefono(personaForm.getTelefono());
            personaExistente.setDireccion(personaForm.getDireccion());
            personaService.save(personaExistente);
            emp.setPersona(personaExistente);
        }

        return empleadoRepository.save(emp);
    }

    public void validar(Empleado empleado){
        if (empleado.getPersona() != null && empleado.getPersona().getCedula() != null) {
            String cedula = empleado.getPersona().getCedula();
            Optional<Empleado> empleadoExistente = empleadoRepository.findByPersonaCedula(cedula);

            if (empleadoExistente.isPresent()) {
                if (empleado.getIdEmpleado() == null) {
                    throw new IllegalArgumentException("Error: Ya existe un empleado registrado con la cédula " + cedula);
                }

                else if (!empleadoExistente.get().getIdEmpleado().equals(empleado.getIdEmpleado())) {
                    throw new IllegalArgumentException("Error: La cédula " + cedula + " ya le pertenece a otro empleado.");
                }
            }
        }

        if (empleado.getCargos() == null || empleado.getCargos().isEmpty()) {
            throw new IllegalArgumentException("Error: El empleado debe tener al menos un rol asignado.");
        }
    }

}
