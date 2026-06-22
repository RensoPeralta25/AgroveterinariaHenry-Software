package com.agroveterinaria.repository;

import com.agroveterinaria.entity.EmbargoSalarial;
import com.agroveterinaria.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmbargoSalarialRepository extends JpaRepository<EmbargoSalarial, Long> {
    List<EmbargoSalarial> findByEmpleadoAndActivoTrue(Empleado empleado);
}
