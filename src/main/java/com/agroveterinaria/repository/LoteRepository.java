package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    @Query("SELECT l FROM Lote l JOIN FETCH l.producto")
    List<Lote> findAll();

    List<Lote> findByProducto(Producto producto);

}