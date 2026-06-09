package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.repository.EmpleadoRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class EmpleadoService {
    private static final Pattern CEDULA_PATTERN = Pattern.compile("^\\d{3}-\\d{7}-\\d{1}$");
    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^\\d{3}-\\d{3}-\\d{4}$");

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

    public void delete(Empleado emp){
        empleadoRepository.delete(emp);
    }

    public void validar(Empleado empleado){
        if (empleado == null) {
            throw new IllegalArgumentException("Error: No se recibieron datos del empleado.");
        }

        validarPersona(empleado.getPersona());

        if (empleado.getSalario() == null) {
            throw new IllegalArgumentException("Error: El salario es obligatorio.");
        }

        if (empleado.getSalario().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Error: El salario no puede ser negativo.");
        }

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

        if (empleado.getCargos() == null || empleado.getCargos().isEmpty()) {
            throw new IllegalArgumentException("Error: El empleado debe tener al menos un rol asignado.");
        }
    }

    private void validarPersona(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("Error: Los datos personales del empleado son obligatorios.");
        }

        persona.setCedula(valorNormalizado(persona.getCedula()));
        persona.setNombre(valorNormalizado(persona.getNombre()));
        persona.setTelefono(valorNormalizado(persona.getTelefono()));
        persona.setDireccion(valorNormalizado(persona.getDireccion()));

        if (persona.getCedula().isBlank()) {
            throw new IllegalArgumentException("Error: La cédula es obligatoria.");
        }

        if (!CEDULA_PATTERN.matcher(persona.getCedula()).matches()) {
            throw new IllegalArgumentException("Error: La cédula debe tener el formato 000-0000000-0.");
        }

        if (persona.getNombre().isBlank()) {
            throw new IllegalArgumentException("Error: El nombre es obligatorio.");
        }

        if (persona.getTelefono().isBlank()) {
            throw new IllegalArgumentException("Error: El teléfono es obligatorio.");
        }

        if (!TELEFONO_PATTERN.matcher(persona.getTelefono()).matches()) {
            throw new IllegalArgumentException("Error: El teléfono debe tener el formato 000-000-0000.");
        }

        if (persona.getDireccion().isBlank()) {
            throw new IllegalArgumentException("Error: La dirección es obligatoria.");
        }
    }

    private String valorNormalizado(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public Empleado findByUsuario (Usuario usuario){
        return empleadoRepository.findByUsuario(usuario).orElse(null);
    }

    public List<Empleado> findByCargo(RolEmpleado cargo) {
        return empleadoRepository.findByCargo(cargo);
    }

    @RolesAllowed({"ADMINISTRADOR", "CAJERO"})
    public List<Empleado> findVendedores() {
        return empleadoRepository.findAll().stream()
                .filter(empleado -> empleado.getCargos().contains(RolEmpleado.CAJERO)
                        || empleado.getCargos().contains(RolEmpleado.ADMINISTRADOR))
                .toList();
    }

}
