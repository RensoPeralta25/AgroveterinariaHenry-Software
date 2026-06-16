package com.agroveterinaria.repository;

import com.agroveterinaria.entity.ConfiguracionNomina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionNominaRepository extends JpaRepository<ConfiguracionNomina, Long> {

    Optional<ConfiguracionNomina> findByClave(String clave);
}
