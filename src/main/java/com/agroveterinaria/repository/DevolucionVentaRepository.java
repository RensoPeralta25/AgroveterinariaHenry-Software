package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DevolucionVenta;
import com.agroveterinaria.entity.NotaDeCredito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionVentaRepository extends JpaRepository<DevolucionVenta, Long> {
    boolean existsByNotaDeCredito(NotaDeCredito notaDeCredito);

    @EntityGraph(attributePaths = {
            "cliente",
            "cliente.persona",
            "empleado",
            "empleado.persona",
            "notaDeCredito"
    })
    @Query("SELECT d FROM DevolucionVenta d")
    List<DevolucionVenta> findAllConRelaciones();
}