package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.enums.EstadoRecepcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @EntityGraph(attributePaths = {"proveedor"})
    List<Compra> findByEstadoRecepcionIn(Collection<EstadoRecepcion> estados);
}
