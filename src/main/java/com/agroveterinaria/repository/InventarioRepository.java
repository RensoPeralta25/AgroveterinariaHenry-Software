package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    @Query("SELECT i FROM Inventario i JOIN FETCH i.lote l JOIN FETCH l.producto WHERE i.almacen = :almacen")
    List<Inventario> findByAlmacen(@Param("almacen") Almacen almacen);

}
