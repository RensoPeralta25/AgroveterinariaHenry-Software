package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.PrestamoEmpleado;
import com.agroveterinaria.enums.EstadoPrestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrestamoEmpleadoRepository extends JpaRepository<PrestamoEmpleado, Long> {
    List<PrestamoEmpleado> findByEmpleadoAndEstado(Empleado empleado, EstadoPrestamo estado);

    List<PrestamoEmpleado> findByEmpleado(Empleado empleado);

    boolean existsByEmpleado(Empleado empleado);

    boolean existsByEmpleadoAndEstado(Empleado empleado, EstadoPrestamo estado);

    boolean existsByEmpleadoAndEstadoIn(Empleado empleado, List<EstadoPrestamo> list);

    @Query("SELECT p FROM PrestamoEmpleado p JOIN FETCH p.empleado e JOIN FETCH e.persona")
    List<PrestamoEmpleado> findAllFetchEmpleado();

    List<PrestamoEmpleado> findByEmpleado_IdEmpleadoAndEstadoIn(Long idEmpleado, List<EstadoPrestamo> estados);
}
