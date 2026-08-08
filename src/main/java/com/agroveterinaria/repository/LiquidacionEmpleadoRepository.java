package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.LiquidacionEmpleado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LiquidacionEmpleadoRepository extends JpaRepository<LiquidacionEmpleado, Long> {

    @EntityGraph(attributePaths = {"empleado", "empleado.persona"})
    Optional<LiquidacionEmpleado> findFirstByEmpleadoOrderByFechaLiquidacionDesc(Empleado empleado);
}