package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Despacho;
import com.agroveterinaria.entity.Transferencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    @EntityGraph(attributePaths = {
            "transferencia.almacenDestino",
            "ventas.cliente.persona"
    })
    @Query("SELECT d FROM Despacho d ORDER BY d.fechaHoraSalidaProgramada DESC")
    List<Despacho> findAllConRelaciones();

    Despacho findFirstByTransferenciaOrderByFechaHoraSalidaProgramadaDesc(Transferencia transferencia);

    long countByFechaHoraEntregaIsNull();

    @Query("SELECT d FROM Despacho d " +
            "JOIN FETCH d.transporte t " +
            "JOIN FETCH t.vehiculo " +
            "JOIN FETCH t.conductor " +
            "LEFT JOIN FETCH t.ruta " +
            "WHERE d.idDespacho = :idDespacho")
    Optional<Despacho> findByIdWithFullTransporte(@Param("idDespacho") Long idDespacho);
}