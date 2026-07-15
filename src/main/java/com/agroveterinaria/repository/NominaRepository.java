package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.enums.PeriodoNomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface NominaRepository extends JpaRepository<Nomina, Long> {

    @Query("SELECT DISTINCT n FROM Nomina n " +
            "JOIN FETCH n.empleado e " +
            "JOIN FETCH e.persona " +
            "LEFT JOIN FETCH n.detalles " +
            "JOIN FETCH n.corrida c " +
            "ORDER BY c.fechaEmision DESC")
    List<Nomina> findAllConEmpleadoYDetalles();

    boolean existsByEmpleado(Empleado empleado);

    @Query("SELECT DISTINCT n.empleado.idEmpleado FROM Nomina n")
    Set<Long> findIdsEmpleadosConHistorial();
}
