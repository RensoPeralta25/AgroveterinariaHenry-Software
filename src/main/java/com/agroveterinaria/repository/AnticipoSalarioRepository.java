package com.agroveterinaria.repository;

import com.agroveterinaria.entity.AnticipoSalario;
import com.agroveterinaria.enums.EstadoAnticipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnticipoSalarioRepository extends JpaRepository<AnticipoSalario, Long> {
    List<AnticipoSalario> findByEmpleadoIdEmpleadoAndEstado(Long empleadoId, EstadoAnticipo estado);

    boolean existsByEmpleadoIdEmpleadoAndEstadoIn(Long empleadoId, List<EstadoAnticipo> estados);
}
