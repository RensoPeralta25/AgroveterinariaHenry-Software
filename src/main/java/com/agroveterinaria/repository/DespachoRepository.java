package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Despacho;
import com.agroveterinaria.entity.Transferencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    @EntityGraph(attributePaths = {
            "transferencia.almacenDestino",
            "ventas.cliente.persona"
    })
    @Query("SELECT d FROM Despacho d ORDER BY d.fechaHoraSalidaProgramada DESC")
    List<Despacho> findAllConRelaciones();

    Despacho findFirstByTransferenciaOrderByFechaHoraSalidaProgramadaDesc(Transferencia transferencia);
}