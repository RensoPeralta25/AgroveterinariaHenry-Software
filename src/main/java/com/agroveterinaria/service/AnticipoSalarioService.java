package com.agroveterinaria.service;

import com.agroveterinaria.entity.AnticipoSalario;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.EstadoAnticipo;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.repository.AnticipoSalarioRepository;
import com.agroveterinaria.repository.CorridaNominaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class AnticipoSalarioService {

    private final AnticipoSalarioRepository anticipoSalarioRepository;
    private final CorridaNominaRepository corridaNominaRepository;
    private final ConfiguracionNominaService configuracionNominaService;

    @Transactional(readOnly = true)
    public List<AnticipoSalario> findAll() {
        return anticipoSalarioRepository.findAll();
    }

    @Transactional
    public AnticipoSalario save(AnticipoSalario anticipo) {
        validarNominaPendiente();

        BigDecimal remanenteDisponible = calcularRemanenteTrasEmbargo(anticipo.getEmpleado());
        validarLimitesFinancieros(anticipo, remanenteDisponible);

        if (anticipo.getId() == null) {
            if (anticipoSalarioRepository.existsByEmpleadoIdEmpleadoAndEstadoIn(anticipo.getEmpleado().getIdEmpleado(),
                    Arrays.asList(EstadoAnticipo.PENDIENTE, EstadoAnticipo.APROBADO))) {
                throw new IllegalStateException("El empleado ya posee un anticipo activo o en proceso de aprobación.");
            }

            anticipo.setSaldoPendiente(anticipo.getMontoOriginal());
            anticipo.setMontoDescontado(BigDecimal.ZERO);
            anticipo.setEstado(EstadoAnticipo.PENDIENTE);
        } else {
            if (anticipo.getEstado() == EstadoAnticipo.PENDIENTE) {
                anticipo.setSaldoPendiente(anticipo.getMontoOriginal());
            }
        }
        return anticipoSalarioRepository.save(anticipo);
    }

    @Transactional
    public AnticipoSalario aprobar(AnticipoSalario anticipo) {
        validarNominaPendiente();

        if (anticipo.getEstado() != EstadoAnticipo.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar anticipos en estado PENDIENTE.");
        }
        anticipo.setEstado(EstadoAnticipo.APROBADO);
        return anticipoSalarioRepository.save(anticipo);
    }

    @Transactional
    public void delete(AnticipoSalario anticipo) {
        if (anticipo.getEstado() != EstadoAnticipo.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden eliminar anticipos en estado PENDIENTE.");
        }
        anticipoSalarioRepository.delete(anticipo);
    }

    private void validarNominaPendiente() {
        if (corridaNominaRepository.existsByEstado(EstadoCorrida.PENDIENTE)) {
            throw new IllegalStateException(
                    "Hay una corrida de nómina PENDIENTE. Para incluir este anticipo en el período actual, " +
                            "elimine esa corrida, registre el anticipo, y vuelva a generar la nómina."
            );
        }
    }

    private void validarLimitesFinancieros(AnticipoSalario anticipo, BigDecimal remanenteDisponible) {
        BigDecimal montoPropuesto = anticipo.getMontoOriginal();
        BigDecimal cuotaPropuesta = anticipo.getCuotaDescuento();

        BigDecimal montoMinimoPermitido = configuracionNominaService.getSalarioMinimoLegal()
                .multiply(configuracionNominaService.getAnticipoPorcentajeMinimoMonto());

        BigDecimal montoMaximoPermitido = remanenteDisponible
                .multiply(configuracionNominaService.getAnticipoPorcentajeMaximoMonto());

        if (montoPropuesto.compareTo(montoMinimoPermitido) < 0) {
            throw new IllegalArgumentException("El monto solicitado es inferior al mínimo permitido (RD$ " + format(montoMinimoPermitido) + ").");
        }
        if (montoPropuesto.compareTo(montoMaximoPermitido) > 0) {
            throw new IllegalArgumentException("El monto excede el 50% permitido del salario neto disponible (Máximo autorizado: RD$ " + format(montoMaximoPermitido) + ").");
        }

        BigDecimal divisorMaximo = configuracionNominaService.getAnticipoDivisorMaximoCuota();
        BigDecimal cuotaMaximaPermitida = remanenteDisponible.divide(divisorMaximo, 2, RoundingMode.HALF_UP);

        int plazoMaximo = configuracionNominaService.getAnticipoPlazoMaximoMeses();
        BigDecimal cuotaMinimaPermitida = montoPropuesto.divide(new BigDecimal(plazoMaximo), 2, RoundingMode.HALF_UP);

        if (cuotaPropuesta.compareTo(cuotaMaximaPermitida) > 0) {
            throw new IllegalArgumentException("La cuota excede el límite permitido del remanente legal del empleado (Máximo permitido: RD$ " + format(cuotaMaximaPermitida) + ").");
        }
        if (cuotaPropuesta.compareTo(cuotaMinimaPermitida) < 0) {
            throw new IllegalArgumentException("La cuota es muy baja para saldar el anticipo en " + plazoMaximo + " meses. La cuota mínima exigida es de RD$ " + format(cuotaMinimaPermitida) + ".");
        }

    }

    private BigDecimal calcularRemanenteTrasEmbargo(Empleado empleado) {
        BigDecimal salarioBruto = empleado.getSalario();

        BigDecimal tss = configuracionNominaService.calcularAFP(salarioBruto).add(configuracionNominaService.calcularSFS(salarioBruto));
        BigDecimal baseGravable = salarioBruto.subtract(tss);

        BigDecimal isr = configuracionNominaService.calcularISR(baseGravable, com.agroveterinaria.enums.PeriodoNomina.MES);

        BigDecimal netoMensual = baseGravable.subtract(isr);

        BigDecimal montoEmbargo = BigDecimal.ZERO; // TODO: Falta implementar los embargos

        return netoMensual.subtract(montoEmbargo);
    }

    public java.util.Map<String, BigDecimal> calcularLimitesParaUI(Empleado empleado) {
        BigDecimal remanente = calcularRemanenteTrasEmbargo(empleado);

        BigDecimal minMonto = configuracionNominaService.getSalarioMinimoLegal()
                .multiply(configuracionNominaService.getAnticipoPorcentajeMinimoMonto());

        BigDecimal maxMonto = remanente
                .multiply(configuracionNominaService.getAnticipoPorcentajeMaximoMonto());

        BigDecimal maxCuota = remanente
                .divide(configuracionNominaService.getAnticipoDivisorMaximoCuota(), 2, RoundingMode.HALF_UP);

        BigDecimal plazoMaximo = new BigDecimal(configuracionNominaService.getAnticipoPlazoMaximoMeses());

        return java.util.Map.of(
                "minMonto", minMonto,
                "maxMonto", maxMonto,
                "maxCuota", maxCuota,
                "plazoMaximo", plazoMaximo
        );
    }

    private String format(BigDecimal amount) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "DO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(amount);
    }
}
