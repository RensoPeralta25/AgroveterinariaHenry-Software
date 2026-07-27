package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Recepcion;
import com.agroveterinaria.entity.Ruta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecepcionRepository extends JpaRepository<Recepcion, Long> {

    @Query("SELECT COUNT(r) > 0 FROM Recepcion r WHERE r.transporte.ruta = :ruta")
    boolean existsByRutaEnTransporte(@Param("ruta") Ruta ruta);

    @EntityGraph(attributePaths = {
            "detalles",
            "detalles.almacen",
            "detalles.lote",
            "detalles.detalleCompra.producto",
            "detalles.detalleTransferencia.lote.producto"
    })
    Optional<Recepcion> findRecepcionConDetallesByIdRecepcion(Long idRecepcion);

}