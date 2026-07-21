package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RutaRepository extends JpaRepository<Ruta, Long> {
    @Query("SELECT DISTINCT r FROM Ruta r LEFT JOIN FETCH r.paradas")
    List<Ruta> findAllConParadas();
}
