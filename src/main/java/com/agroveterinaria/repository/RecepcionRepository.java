package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Recepcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecepcionRepository extends JpaRepository<Recepcion, Long> {
}