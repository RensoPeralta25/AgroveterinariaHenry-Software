package com.agroveterinaria.repository;

import com.agroveterinaria.entity.AjusteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long> {
}