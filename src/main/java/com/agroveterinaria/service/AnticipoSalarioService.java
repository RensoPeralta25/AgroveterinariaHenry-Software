package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoAnticipo;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.EstadoPrestamo;
import com.agroveterinaria.repository.*;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@AllArgsConstructor
@RolesAllowed({"ADMINISTRADOR", "RECURSOS_HUMANOS"})
public class AnticipoSalarioService {

    private final AnticipoSalarioRepository anticipoSalarioRepository;
    private final PrestamoEmpleadoRepository prestamoEmpleadoRepository;
    private final CorridaNominaRepository corridaNominaRepository;
    private final ConfiguracionNominaService configuracionNominaService;
    private final EmbargoSalarialService embargoSalarialService;
    private final AbonoAnticipoRepository abonoAnticipoRepository;
    private final GastoOperativoRepository gastoOperativoRepository;

    @Transactional(readOnly = true)
    public List<AnticipoSalario> findAll() {
        return anticipoSalarioRepository.findAll();
    }

    @Transactional
    public AnticipoSalario save(AnticipoSalario anticipo) {
        validarNominaPendiente();

        BigDecimal sueldoNetoBase = calcularSueldoNetoBase(anticipo.getEmpleado());

        validarMargenLegalEmbargos(anticipo, sueldoNetoBase);
        validarLimitesFinancieros(anticipo, sueldoNetoBase);

        if (anticipo.getId() == null) {
            if (anticipoSalarioRepository.existsByEmpleadoIdEmpleadoAndEstadoIn(anticipo.getEmpleado().getIdEmpleado(),
                    Arrays.asList(EstadoAnticipo.PENDIENTE, EstadoAnticipo.APROBADO))) {
                throw new IllegalStateException("El empleado ya posee un anticipo activo o en proceso de aprobación.");
            }

            boolean tienePrestamoActivo = prestamoEmpleadoRepository.existsByEmpleadoAndEstadoIn(
                    anticipo.getEmpleado(),
                    Arrays.asList(EstadoPrestamo.PENDIENTE, EstadoPrestamo.APROBADO)
            );

            if (tienePrestamoActivo) {
                throw new IllegalStateException("El empleado tiene un Préstamo Activo o Pendiente. No puede solicitar un Anticipo.");
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

        BigDecimal sueldoNetoBase = calcularSueldoNetoBase(anticipo.getEmpleado());
        validarMargenLegalEmbargos(anticipo, sueldoNetoBase);

        anticipo.setEstado(EstadoAnticipo.APROBADO);

        GastoOperativo gastoAnticipo = new GastoOperativo();
        gastoAnticipo.setTipoGasto(com.agroveterinaria.enums.TipoGasto.ANTICIPO_SALARIO);
        gastoAnticipo.setFecha(LocalDate.now());
        gastoAnticipo.setMonto(anticipo.getMontoOriginal());
        gastoAnticipo.setNotas("Anticipo salarial a empleado: " +  anticipo.getEmpleado().getPersona().getNombre() + " " +
                anticipo.getEmpleado().getPersona().getApellido());

        gastoAnticipo = gastoOperativoRepository.save(gastoAnticipo);
        anticipo.setGastoAsociado(gastoAnticipo);

        return anticipoSalarioRepository.save(anticipo);
    }

    @Transactional
    public void delete(AnticipoSalario anticipo) {
        if (anticipo.getEstado() != EstadoAnticipo.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden eliminar anticipos en estado PENDIENTE.");
        }
        anticipoSalarioRepository.delete(anticipo);
    }

    public List<AbonoAnticipo> obtenerHistorialAbonos(Long idAnticipo) {
        return abonoAnticipoRepository.findByAnticipoSalario_IdOrderByFechaAbonoDesc(idAnticipo);
    }

    @Transactional
    public AbonoAnticipo registrarAbonoExtraordinario(AbonoAnticipo abono) {
        validarNominaPendienteParaAbono();

        AnticipoSalario anticipo = abono.getAnticipoSalario();

        if (anticipo.getEstado() != EstadoAnticipo.APROBADO) {
            throw new IllegalStateException("Solo se pueden registrar abonos a anticipos activos (APROBADOS).");
        }

        if (abono.getMonto().compareTo(anticipo.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException("El monto abonado no puede ser mayor al saldo pendiente.");
        }

        if (abono.getMetodoPago() == com.agroveterinaria.enums.MetodoPago.TRANSFERENCIA) {
            String ref = abono.getReferenciaTransferencia();
            if (ref == null || ref.isBlank()) {
                throw new IllegalArgumentException("La referencia bancaria es obligatoria para transferencias.");
            }

            if (abono.getComprobanteTransferencia() == null || abono.getComprobanteTransferencia().length == 0) {
                throw new IllegalArgumentException("El comprobante de transferencia es obligatorio.");
            }

            if (abonoAnticipoRepository.existsByReferenciaTransferenciaIgnoreCase(ref)) {
                throw new IllegalArgumentException("Error: La referencia bancaria '" + ref + "' ya fue utilizada en otro abono de anticipo.");
            }
        }

        BigDecimal nuevoSaldo = anticipo.getSaldoPendiente().subtract(abono.getMonto());

        anticipo.setMontoDescontado(anticipo.getMontoDescontado().add(abono.getMonto()));
        anticipo.setSaldoPendiente(nuevoSaldo);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0) {
            anticipo.setEstado(EstadoAnticipo.SALDADO);
        }

        anticipoSalarioRepository.save(anticipo);
        return abonoAnticipoRepository.save(abono);
    }

    private void validarNominaPendiente() {
        if (corridaNominaRepository.existsByEstado(EstadoCorrida.PENDIENTE)) {
            throw new IllegalStateException(
                    "Hay una corrida de nómina PENDIENTE. Para incluir este anticipo en el período actual, " +
                            "elimine esa corrida, registre el anticipo, y vuelva a generar la nómina."
            );
        }
    }

    private void validarNominaPendienteParaAbono() {
        if (corridaNominaRepository.existsByEstado(EstadoCorrida.PENDIENTE)) {
            throw new IllegalStateException(
                    "Hay una corrida de nómina PENDIENTE. " +
                            "No se pueden registrar abonos mientras la nómina esté en proceso, " +
                            "ya que alteraría los balances y descuadraría los descuentos ya calculados. " +
                            "Por favor, apruebe o elimine la nómina actual antes de aplicar el abono."
            );
        }
    }

    private void validarLimitesFinancieros(AnticipoSalario anticipo, BigDecimal sueldoNetoBase) {
        BigDecimal montoPropuesto = anticipo.getMontoOriginal();
        BigDecimal cuotaPropuesta = anticipo.getCuotaDescuento();

        BigDecimal montoMinimoPermitido = configuracionNominaService.getSalarioMinimoLegal()
                .multiply(configuracionNominaService.getAnticipoPorcentajeMinimoMonto());

        BigDecimal montoMaximoPermitido = sueldoNetoBase
                .multiply(configuracionNominaService.getAnticipoPorcentajeMaximoMonto());

        if (montoPropuesto.compareTo(montoMinimoPermitido) < 0) {
            throw new IllegalArgumentException("El monto solicitado es inferior al mínimo permitido (RD$ " + format(montoMinimoPermitido) + ").");
        }
        if (montoPropuesto.compareTo(montoMaximoPermitido) > 0) {
            throw new IllegalArgumentException("El monto excede el 50% permitido del salario neto disponible (Máximo autorizado: RD$ " + format(montoMaximoPermitido) + ").");
        }

        BigDecimal divisorMaximo = configuracionNominaService.getAnticipoDivisorMaximoCuota();
        BigDecimal cuotaMaximaPermitida = sueldoNetoBase.divide(divisorMaximo, 2, RoundingMode.HALF_UP);

        int plazoMaximo = configuracionNominaService.getAnticipoPlazoMaximoMeses();
        BigDecimal cuotaMinimaPermitida = montoPropuesto.divide(new BigDecimal(plazoMaximo), 2, RoundingMode.HALF_UP);

        if (cuotaPropuesta.compareTo(cuotaMaximaPermitida) > 0) {
            throw new IllegalArgumentException("La cuota excede el límite permitido del remanente legal del empleado (Máximo permitido: RD$ " + format(cuotaMaximaPermitida) + ").");
        }
        if (cuotaPropuesta.compareTo(cuotaMinimaPermitida) < 0) {
            throw new IllegalArgumentException("La cuota es muy baja para saldar el anticipo en " + plazoMaximo + " meses. La cuota mínima exigida es de RD$ " + format(cuotaMinimaPermitida) + ".");
        }

    }

    private BigDecimal calcularSueldoNetoBase(Empleado empleado) {
        BigDecimal salarioBruto = empleado.getSalario();

        BigDecimal tss = configuracionNominaService.calcularAFP(salarioBruto).add(configuracionNominaService.calcularSFS(salarioBruto));
        BigDecimal baseGravable = salarioBruto.subtract(tss);

        BigDecimal isr = configuracionNominaService.calcularISR(baseGravable, com.agroveterinaria.enums.PeriodoNomina.MES);

        return baseGravable.subtract(isr);
    }

    public java.util.Map<String, BigDecimal> calcularLimitesParaUI(Empleado empleado) {
        BigDecimal remanente = calcularSueldoNetoBase(empleado);

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

    private void validarMargenLegalEmbargos(AnticipoSalario anticipo, BigDecimal salarioNetoMensual) {
        Empleado empleado = anticipo.getEmpleado();
        if (empleado == null) return;

        List<EmbargoSalarial> embargos = embargoSalarialService.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(empleado);

        if (embargos.isEmpty()) {
            return;
        }

        BigDecimal totalMora = embargos.stream()
                .map(EmbargoSalarial::getSaldoPendienteMora)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalMora.compareTo(BigDecimal.ZERO) > 0) {
            lanzarErrorMargenLegal("Solicitud denegada: El empleado posee mora judicial acumulada activa (RD$ " + format(totalMora) + ").");
        }

        BigDecimal limiteMensual = salarioNetoMensual.multiply(configuracionNominaService.getPorcentajeLimiteEmbargo());
        BigDecimal cuotaAnticipo = anticipo.getCuotaDescuento();

        int mesesAnticipo = anticipo.getMontoOriginal().divide(cuotaAnticipo, 0, RoundingMode.CEILING).intValue();

        BigDecimal totalCuotasOrdEmbargos = embargos.stream()
                .map(EmbargoSalarial::getMontoCuotaOrdinaria)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate fechaActual = java.time.LocalDate.now();

        for (int i = 0; i < mesesAnticipo; i++) {
            LocalDate mesProyectado = fechaActual.plusMonths(i);
            int mesIteracion = mesProyectado.getMonthValue();
            int anioIteracion = mesProyectado.getYear();

            BigDecimal totalExtraordinarioDelMes = embargos.stream()
                    .flatMap(e -> e.getCuotasExtras().stream())
                    .filter(cuota -> cuota.getMesAplicacion() == mesIteracion)
                    .map(CuotaExtraEmbargo::getMontoExtra)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cargaTotalProyectada = totalCuotasOrdEmbargos
                    .add(totalExtraordinarioDelMes)
                    .add(cuotaAnticipo);

            if (cargaTotalProyectada.compareTo(limiteMensual) > 0) {
                lanzarErrorMargenLegal(
                        "Solicitud denegada por proyección legal. El anticipo superará el 50% legal en el mes "
                                + mesIteracion + "/" + anioIteracion + ". "
                                + "(Carga proyectada: RD$ " + format(cargaTotalProyectada)
                                + " | Límite: RD$ " + format(limiteMensual) + ")"
                );
            }
        }
    }

    private void lanzarErrorMargenLegal(String mensaje) {
        throw new IllegalStateException(mensaje);
    }

    private String format(BigDecimal amount) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "DO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(amount);
    }
}
