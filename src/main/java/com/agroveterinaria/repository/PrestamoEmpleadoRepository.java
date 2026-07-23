package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.PrestamoEmpleado;
import com.agroveterinaria.enums.EstadoPrestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestamoEmpleadoRepository extends JpaRepository<PrestamoEmpleado, Long> {
    List<PrestamoEmpleado> findByEmpleadoAndEstado(Empleado empleado, EstadoPrestamo estado);

    List<PrestamoEmpleado> findByEmpleado(Empleado empleado);

    boolean existsByEmpleado(Empleado empleado);

    boolean existsByEmpleadoAndEstado(Empleado empleado, EstadoPrestamo estado);
}
