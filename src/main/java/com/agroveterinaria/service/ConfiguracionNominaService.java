package com.agroveterinaria.service;

import com.agroveterinaria.entity.ConfiguracionNomina;
import com.agroveterinaria.enums.PeriodoNomina;
import com.agroveterinaria.repository.ConfiguracionNominaRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class ConfiguracionNominaService {
    private final ConfiguracionNominaRepository configuracionRepository;

    public ConfiguracionNominaService(ConfiguracionNominaRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    public List<ConfiguracionNomina> findAll() {
        return configuracionRepository.findAll();
    }

    public ConfiguracionNomina actualizar(ConfiguracionNomina configuracion) {
        return configuracionRepository.save(configuracion);
    }

    public BigDecimal obtenerValor(String clave) {
        return configuracionRepository.findByClave(clave).map(ConfiguracionNomina::getValor)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + clave));
    }

    public BigDecimal calcularAFP(BigDecimal devengado) {
        BigDecimal porcentaje = obtenerValor("AFP_PORCENTAJE");
        BigDecimal tope = obtenerValor("AFP_TOPE");
        BigDecimal base = devengado.min(tope);
        return base.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularSFS(BigDecimal devengado) {
        BigDecimal porcentaje = obtenerValor("SFS_PORCENTAJE");
        BigDecimal tope = obtenerValor("SFS_TOPE");
        BigDecimal base = devengado.min(tope);
        return base.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularISR(BigDecimal devengadoMensual, PeriodoNomina periodo) {
        int divisor = periodo == PeriodoNomina.MES ? 12 : 24;
        BigDecimal devengadoAnual = devengadoMensual.multiply(BigDecimal.valueOf(divisor));

        BigDecimal tramo1Limite = obtenerValor("ISR_TRAMO_1_LIMITE");
        BigDecimal tramo2Limite = obtenerValor("ISR_TRAMO_2_LIMITE");
        BigDecimal tramo3Limite = obtenerValor("ISR_TRAMO_3_LIMITE");
        BigDecimal tramo1Pct    = obtenerValor("ISR_TRAMO_1_PORCENTAJE");
        BigDecimal tramo2Base   = obtenerValor("ISR_TRAMO_2_BASE");
        BigDecimal tramo2Pct    = obtenerValor("ISR_TRAMO_2_PORCENTAJE");
        BigDecimal tramo3Base   = obtenerValor("ISR_TRAMO_3_BASE");
        BigDecimal tramo3Pct    = obtenerValor("ISR_TRAMO_3_PORCENTAJE");

        BigDecimal isrAnual;

        if (devengadoAnual.compareTo(tramo1Limite) <= 0) {
            isrAnual = BigDecimal.ZERO;

        } else if (devengadoAnual.compareTo(tramo2Limite) <= 0) {
            isrAnual = devengadoAnual.subtract(tramo1Limite).multiply(tramo1Pct).setScale(2, RoundingMode.HALF_UP);

        } else if (devengadoAnual.compareTo(tramo3Limite) <= 0) {
            isrAnual = tramo2Base.add(devengadoAnual.subtract(tramo2Limite).multiply(tramo2Pct)).setScale(2, RoundingMode.HALF_UP);

        } else {
            isrAnual = tramo3Base.add(devengadoAnual.subtract(tramo3Limite).multiply(tramo3Pct)).setScale(2, RoundingMode.HALF_UP);
        }

        return isrAnual.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularHorasExtras(Double cantidadHoras) {
        if (cantidadHoras == null || cantidadHoras <= 0) return BigDecimal.ZERO;
        BigDecimal valorFijo = obtenerValor("HORA_EXTRA_VALOR_FIJO");
        return valorFijo.multiply(BigDecimal.valueOf(cantidadHoras))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
