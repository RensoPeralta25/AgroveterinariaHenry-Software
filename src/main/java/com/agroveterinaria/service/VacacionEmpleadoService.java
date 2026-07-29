package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.VacacionEmpleado;
import com.agroveterinaria.enums.EstadoVacacion;
import com.agroveterinaria.repository.VacacionEmpleadoRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
@RolesAllowed("ADMINISTRADOR")
public class VacacionEmpleadoService {
    private final VacacionEmpleadoRepository vacacionEmpleadoRepository;

    public VacacionEmpleado save(VacacionEmpleado vacacionEmpleado){
        validarFechasUnicas(vacacionEmpleado);
        validarAntiguedadMinima(vacacionEmpleado);

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

    public void aprobarVacacion(VacacionEmpleado vacacion, Empleado aprobador) {
        if (vacacion.getEstado() != EstadoVacacion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar vacaciones en estado PENDIENTE.");
        }
        vacacion.setEstado(EstadoVacacion.APROBADA);
        vacacion.setAprobadoPor(aprobador);
        vacacionEmpleadoRepository.save(vacacion);
    }

    public VacacionEmpleado update(VacacionEmpleado vacacionModificada){
        validarFechasUnicas(vacacionModificada);
        validarAntiguedadMinima(vacacionModificada);

        LocalDate hoy = LocalDate.now();
        VacacionEmpleado original = vacacionEmpleadoRepository.findById(vacacionModificada.getId())
                .orElseThrow(() -> new IllegalArgumentException("La vacación no existe."));

        if (original.getEstado() != EstadoVacacion.PENDIENTE) {
            throw new IllegalStateException("Acción denegada: No se pueden modificar los datos de una vacación que ya fue aprobada o pagada.");
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
        vacacion.setEstado(EstadoVacacion.PAGADA);
        vacacionEmpleadoRepository.save(vacacion);
    }

    private void validarAntiguedadMinima(VacacionEmpleado vacacionEmpleado) {
        long aniosAntiguedad = ChronoUnit.YEARS.between(
                vacacionEmpleado.getEmpleado().getFechaIngreso(),
                vacacionEmpleado.getFechaInicio()
        );

        if (aniosAntiguedad < 1) {
            throw new IllegalStateException("Acción denegada: El empleado debe tener al menos 1 año en la empresa para registrar vacaciones.");
        }
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
        if (vacacionEmpleado.getEstado() != EstadoVacacion.PENDIENTE) {
            throw new IllegalStateException("Acción denegada: No se puede eliminar un registro de vacaciones que ya ha sido aprobado o pagado.");
        }
        vacacionEmpleadoRepository.delete(vacacionEmpleado);
    }

    public List<VacacionEmpleado> findByEmpleadoYNoPagadas(Empleado empleado){
        return vacacionEmpleadoRepository.findByEmpleadoAndEstadoNot(empleado, EstadoVacacion.PAGADA);
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
