package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.VacacionEmpleado;
import com.agroveterinaria.repository.VacacionEmpleadoRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
@RolesAllowed("ADMINISTRADOR")
public class VacacionEmpleadoService {
    private final VacacionEmpleadoRepository vacacionEmpleadoRepository;

    public VacacionEmpleado save(VacacionEmpleado vacacionEmpleado){
        validarFechasUnicas(vacacionEmpleado);

        LocalDate hoy = LocalDate.now();

        if (vacacionEmpleado.getFechaInicio().isBefore(hoy)) {
            throw new IllegalStateException("La fecha de inicio no puede ser anterior a la fecha actual.");
        }
        if (vacacionEmpleado.getFechaInicio().isAfter(hoy.plusYears(1))) {
            throw new IllegalStateException("No se pueden programar vacaciones con más de un año de anticipación.");
        }
        if (vacacionEmpleado.getFechaFin().isBefore(vacacionEmpleado.getFechaInicio())) {
            throw new IllegalStateException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        return vacacionEmpleadoRepository.save(vacacionEmpleado);
    }

    public VacacionEmpleado update(VacacionEmpleado vacacionModificada){
        validarFechasUnicas(vacacionModificada);

        LocalDate hoy = LocalDate.now();
        VacacionEmpleado original = vacacionEmpleadoRepository.findById(vacacionModificada.getId())
                .orElseThrow(() -> new IllegalArgumentException("La vacación no existe."));

        if (original.isPagado()) {
            throw new IllegalStateException("Acción denegada: No se pueden modificar los datos de una vacación que ya fue procesada y pagada en nómina.");
        }

        if (!original.getEmpleado().getIdEmpleado().equals(vacacionModificada.getEmpleado().getIdEmpleado())) {
            throw new IllegalStateException("Acción denegada: No se puede transferir una vacación registrada a otro empleado.");
        }

        if (vacacionModificada.getFechaInicio().isBefore(hoy)) {
            throw new IllegalStateException("La nueva fecha de inicio no puede ser anterior a la fecha actual.");
        }
        if (vacacionModificada.getFechaInicio().isAfter(hoy.plusYears(1))) {
            throw new IllegalStateException("No se pueden reprogramar vacaciones para más de un año en el futuro.");
        }
        if (vacacionModificada.getFechaFin().isBefore(vacacionModificada.getFechaInicio())) {
            throw new IllegalStateException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        return vacacionEmpleadoRepository.save(vacacionModificada);
    }

    public void marcarComoPagada(VacacionEmpleado vacacion) {
        vacacion.setPagado(true);
        vacacionEmpleadoRepository.save(vacacion);
    }

    private void validarFechasUnicas(VacacionEmpleado vacacion) {
        boolean existe = vacacionEmpleadoRepository.existeInterseccionFechas(
                vacacion.getEmpleado(),
                vacacion.getFechaInicio(),
                vacacion.getFechaFin(),
                vacacion.getId()
        );
        if (existe) {
            throw new IllegalStateException("Error: El empleado ya tiene vacaciones registradas en este rango de fechas.");
        }
    }

    public List<VacacionEmpleado> findAll(){
        return vacacionEmpleadoRepository.findAll();
    }

    public void delete(VacacionEmpleado vacacionEmpleado){
        if (vacacionEmpleado.isPagado()) {
            throw new IllegalStateException("Acción denegada: No se puede eliminar un registro de vacaciones que ya ha sido pagado en nómina.");
        }
        vacacionEmpleadoRepository.delete(vacacionEmpleado);
    }

    public List<VacacionEmpleado> findByEmpleadoAndPagadoFalse(Empleado empleado){
        return vacacionEmpleadoRepository.findByEmpleadoAndPagadoFalse(empleado);
    }

    public boolean existsByEmpleado(Empleado empleado){
        return vacacionEmpleadoRepository.existsByEmpleado(empleado);
    }

    List<VacacionEmpleado> encontrarVacacionesEnPeriodo(Empleado empleado, LocalDate inicioPeriodo, LocalDate finPeriodo){
        return vacacionEmpleadoRepository.encontrarVacacionesEnPeriodo(empleado, inicioPeriodo, finPeriodo);
    }

    public int obtenerDiasYaTomados(Long empleadoId, LocalDate inicioAniversario, LocalDate finAniversario) {
        return vacacionEmpleadoRepository.sumDiasDisfrutadosEnPeriodo(empleadoId, inicioAniversario, finAniversario);
    }

    public Optional<VacacionEmpleado> findById(Long id){
        return vacacionEmpleadoRepository.findById(id);
    }
}
