package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DetalleNomina;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.enums.TipoConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DetalleNominaRepository extends JpaRepository<DetalleNomina, Long> {

    @Query("SELECT COALESCE(SUM(d.monto), 0) FROM DetalleNomina d " +
            "WHERE d.nomina.empleado.IdEmpleado = :idEmpleado " +
            "AND d.tipo = 'PRESTAMO_EMPRESA' " +
            "AND d.nomina.corrida.estado = 'APROBADA'")
    BigDecimal sumarPagosDePrestamosPorEmpleado(@Param("idEmpleado") Long idEmpleado);

    List<DetalleNomina> findByNomina(Nomina nomina);

    @Query("SELECT COALESCE(SUM(d.monto), 0) FROM DetalleNomina d " +
            "WHERE d.nomina.empleado = :empleado " +
            "AND d.nomina.corrida.estado = 'APROBADA' " +
            "AND d.nomina.corrida.fechaEmision BETWEEN :inicioAnio AND :finAnio " +
            "AND d.tipo IN :conceptosOrdinarios")
    BigDecimal sumarSalarioOrdinarioDelAnio(
            @Param("empleado") Empleado empleado,
            @Param("inicioAnio") LocalDate inicioAnio,
            @Param("finAnio") LocalDate finAnio,
            @Param("conceptosOrdinarios") List<TipoConcepto> conceptosOrdinarios
    );
}
