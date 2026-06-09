package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.enums.EstadoRecepcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Override
    @EntityGraph(attributePaths = {"proveedor", "detalles", "detalles.producto"})
    Optional<Compra> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"proveedor"})
    List<Compra> findAll();

    @EntityGraph(attributePaths = {"proveedor"})
    List<Compra> findByEstadoRecepcionIn(Collection<EstadoRecepcion> estados);

    @EntityGraph(attributePaths = {"proveedor"})
    Optional<Compra> findFirstByEstadoRecepcionOrderByIdCompraDesc(EstadoRecepcion estado);
}
