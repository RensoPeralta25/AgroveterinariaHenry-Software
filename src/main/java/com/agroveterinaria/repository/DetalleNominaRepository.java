package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DetalleNomina;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.TipoConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DetalleNominaRepository extends JpaRepository<DetalleNomina, Long> {

    List<DetalleNomina> findByNomina(Nomina nomina);

    @Query("SELECT COALESCE(SUM(d.monto), 0) FROM DetalleNomina d " +
            "WHERE d.nomina.empleado = :empleado " +
            "AND d.nomina.corrida.estado = :estadoCorrida " +
            "AND d.nomina.corrida.fechaEmision BETWEEN :inicioAnio AND :finAnio " +
            "AND d.tipo IN :conceptosOrdinarios")
    BigDecimal sumarSalarioOrdinarioDelAnio(
            @Param("empleado") Empleado empleado,
            @Param("estadoCorrida") EstadoCorrida estadoCorrida,
            @Param("inicioAnio") LocalDate inicioAnio,
            @Param("finAnio") LocalDate finAnio,
            @Param("conceptosOrdinarios") List<TipoConcepto> conceptosOrdinarios
    );

    @Query("SELECT SUM(d.monto) FROM DetalleNomina d " +
            "JOIN d.nomina n " +
            "JOIN n.corrida c " +
            "WHERE n.empleado = :empleado " +
            "AND d.tipo = :concepto " +
            "AND c.estado = EstadoCorrida.APROBADA " +
            "AND c.fechaEmision BETWEEN :inicio AND :fin")
    BigDecimal sumarTotalPorConceptoYRangoDeFechas(
            @Param("empleado") Empleado empleado,
            @Param("concepto") TipoConcepto concepto,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
}
