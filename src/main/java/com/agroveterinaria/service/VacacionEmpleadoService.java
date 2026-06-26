package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.VacacionEmpleado;
import com.agroveterinaria.repository.VacacionEmpleadoRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Service
public class VacacionEmpleadoService {
    private final VacacionEmpleadoRepository vacacionEmpleadoRepository;

    public VacacionEmpleado save(VacacionEmpleado vacacionEmpleado){
        return vacacionEmpleadoRepository.save(vacacionEmpleado);
    }

    public List<VacacionEmpleado>  findByEmpleadoAndPagadoPorAdelantadoFalse(Empleado empleado){
        return vacacionEmpleadoRepository.findByEmpleadoAndPagadoPorAdelantadoFalse(empleado);
    }

    public boolean existsByEmpleado(Empleado empleado){
        return vacacionEmpleadoRepository.existsByEmpleado(empleado);
    }

    List<VacacionEmpleado> encontrarVacacionesEnPeriodo(Empleado empleado, LocalDate inicioPeriodo, LocalDate finPeriodo){
        return vacacionEmpleadoRepository.encontrarVacacionesEnPeriodo(empleado, inicioPeriodo, finPeriodo);
    }
}
