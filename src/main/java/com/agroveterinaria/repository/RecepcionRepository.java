package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Recepcion;
import com.agroveterinaria.entity.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecepcionRepository extends JpaRepository<Recepcion, Long> {

    @Query("SELECT COUNT(r) > 0 FROM Recepcion r WHERE r.transporte.ruta = :ruta")
    boolean existsByRutaEnTransporte(@Param("ruta") Ruta ruta);

}