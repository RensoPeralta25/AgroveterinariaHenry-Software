package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Cobro;
import com.agroveterinaria.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CobroRepository extends JpaRepository<Cobro, Long> {

    boolean existsByReferenciaTransferenciaIgnoreCase(String referenciaTransferencia);

    List<Cobro> findByVenta(Venta venta);

    @Query("SELECT COALESCE(SUM(c.montoTotal), 0) FROM Cobro c WHERE c.venta = :venta")
    BigDecimal sumMontoByVenta(@Param("venta") Venta venta);
}
