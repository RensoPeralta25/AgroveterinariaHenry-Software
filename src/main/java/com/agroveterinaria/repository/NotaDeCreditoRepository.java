package com.agroveterinaria.repository;

import com.agroveterinaria.entity.NotaDeCredito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotaDeCreditoRepository extends JpaRepository<NotaDeCredito, Long> {

    @EntityGraph(attributePaths = {"cliente", "cliente.persona"})
    List<NotaDeCredito> findAllByOrderByFechaEmisionDesc();

    @EntityGraph(attributePaths = {"cliente", "cliente.persona"})
    List<NotaDeCredito> findByClienteIdClienteAndSaldoDisponibleGreaterThanOrderByFechaEmisionAsc(
            Long idCliente,
            BigDecimal saldoMinimo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n
            FROM NotaDeCredito n
            JOIN FETCH n.cliente c
            WHERE n.idNotaCredito = :idNotaCredito
            """)
    Optional<NotaDeCredito> buscarPorIdParaActualizar(@Param("idNotaCredito") Long idNotaCredito);
}