package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Transferencia;
import com.agroveterinaria.enums.EstadoTransferencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {
    @EntityGraph(attributePaths = {"almacenOrigen", "almacenDestino"})
    List<Transferencia> findByEstadoIn(Collection<EstadoTransferencia> estados);

    List<Transferencia> Estado(EstadoTransferencia estado);

    List<Transferencia> findByEstado(EstadoTransferencia estado);
}
