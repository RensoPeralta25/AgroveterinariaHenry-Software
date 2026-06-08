package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.enums.RolEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByPersonaCedula (String cedula);

    Optional<Empleado> findByUsuario (Usuario usuario);

    @Query("SELECT e FROM Empleado e JOIN e.cargos c WHERE c = :rol")
    List<Empleado> findByCargo(@Param("rol") RolEmpleado rol);
}
