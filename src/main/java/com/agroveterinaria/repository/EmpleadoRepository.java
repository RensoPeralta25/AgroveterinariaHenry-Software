package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByPersonaCedula (String cedula);
}
