package com.agroveterinaria.repository;

import com.agroveterinaria.entity.AbonoAnticipo;
import com.agroveterinaria.entity.AnticipoSalario;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.EstadoAnticipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnticipoSalarioRepository extends JpaRepository<AnticipoSalario, Long> {
    List<AnticipoSalario> findByEmpleadoIdEmpleadoAndEstado(Long empleadoId, EstadoAnticipo estado);

    boolean existsByEmpleadoIdEmpleadoAndEstadoIn(Long empleadoId, List<EstadoAnticipo> estados);

    boolean existsByEmpleadoAndEstadoIn(Empleado empleado, List<EstadoAnticipo> estados);
}
