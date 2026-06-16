package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DetalleNomina;
import com.agroveterinaria.entity.Nomina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleNominaRepository extends JpaRepository<DetalleNomina, Long> {
    List<DetalleNomina> findByNomina(Nomina nomina);
}
