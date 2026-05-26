package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
