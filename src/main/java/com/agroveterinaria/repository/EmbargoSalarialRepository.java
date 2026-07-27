package com.agroveterinaria.repository;

import com.agroveterinaria.entity.EmbargoSalarial;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.StatusEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmbargoSalarialRepository extends JpaRepository<EmbargoSalarial, Long> {
    List<EmbargoSalarial> findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(Empleado empleado, StatusEntidad estado);

    boolean existsByEmpleado(Empleado empleado);

    @Query("SELECT DISTINCT e FROM EmbargoSalarial e JOIN FETCH e.empleado emp JOIN FETCH emp.persona LEFT JOIN FETCH e.cuotasExtras")
    List<EmbargoSalarial> findAllParaVista();
}
