package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.enums.EstadoRecepcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    long countByEstadoRecepcionIn(Collection<EstadoRecepcion> estados);

    List<Compra> findByFechaHoraCompraGreaterThanEqualAndEstadoRecepcionNotOrderByFechaHoraCompraAsc(
            LocalDateTime fechaInicio,
            EstadoRecepcion estadoExcluido
    );

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c " +
            "WHERE c.fechaHoraCompra >= :inicio AND c.fechaHoraCompra < :fin " +
            "AND c.estadoRecepcion <> :estadoExcluido")
    BigDecimal sumarTotalEntre(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoExcluido") EstadoRecepcion estadoExcluido
    );
}
