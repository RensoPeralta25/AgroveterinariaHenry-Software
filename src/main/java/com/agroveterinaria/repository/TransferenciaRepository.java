package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Transferencia;
import com.agroveterinaria.enums.EstadoTransferencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {
    @EntityGraph(attributePaths = {"almacenOrigen", "almacenDestino"})
    List<Transferencia> findByEstadoIn(Collection<EstadoTransferencia> estados);

    List<Transferencia> Estado(EstadoTransferencia estado);

    List<Transferencia> findByEstado(EstadoTransferencia estado);

    @Query("SELECT t FROM Transferencia t JOIN FETCH t.almacenOrigen JOIN FETCH t.almacenDestino")
    List<Transferencia> findAllConAlmacenes();

    @Query("SELECT t FROM Transferencia t LEFT JOIN FETCH t.detalles d LEFT JOIN FETCH d.lote l LEFT JOIN FETCH l.producto WHERE t.idTransferencia = :id")
    Optional<Transferencia> findByIdConDetalles(@Param("id") Long id);
}
