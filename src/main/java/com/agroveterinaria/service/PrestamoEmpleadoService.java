package com.agroveterinaria.service;

import com.agroveterinaria.dto.nomina.CuotaAmortizacionDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoAnticipo;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.EstadoPrestamo;
import com.agroveterinaria.enums.TipoRecalculoPrestamo;
import com.agroveterinaria.repository.*;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

@AllArgsConstructor
@Service
@RolesAllowed({"ADMINISTRADOR", "RECURSOS_HUMANOS"})
@Transactional
public class PrestamoEmpleadoService {
    private final PrestamoEmpleadoRepository prestamoEmpleadoRepository;
    private final AnticipoSalarioRepository anticipoSalarioRepository;
    private final AbonoPrestamoRepository abonoPrestamoRepository;
    private final ConfiguracionNominaService configuracionNominaService;
    private CorridaNominaRepository corridaNominaRepository;
    private EmbargoSalarialService embargoSalarialService;
    private GastoOperativoRepository gastoOperativoRepository;

    public PrestamoEmpleado save(PrestamoEmpleado prestamo) {
        validarNominaPendiente();

        if (prestamo.getIdPrestamo() == null) {
            validarExclusividad(prestamo.getEmpleado());
        }
        validarLimitesFinancieros(prestamo);

        BigDecimal cuotaCalculada = calcularCuotaFrancesa(prestamo.getMontoCapital(), prestamo.getTasaInteres(), prestamo.getPlazoMeses());
        prestamo.setCuotaPeriodica(cuotaCalculada);

        BigDecimal divisorLegal = configuracionNominaService.getDivisiorLimiteCuotaPrestamo();
        BigDecimal limiteLegal = prestamo.getEmpleado().getSalario().divide(divisorLegal, 2, RoundingMode.HALF_UP);

        if (prestamo.getCuotaPeriodica().compareTo(limiteLegal) > 0) {
            throw new IllegalArgumentException("La cuota supera 1/" + divisorLegal.intValue()
                    + " del salario (Max: RD$ " + limiteLegal.setScale(2, RoundingMode.HALF_UP) + ").");
        }

        if (prestamo.getIdPrestamo() == null) {
            prestamo.setBalanceCapitalPendiente(prestamo.getMontoCapital());
            prestamo.setEstado(EstadoPrestamo.PENDIENTE);
            prestamo.setCuotasPagadas(0);
        }

        BigDecimal sueldoNetoBase = calcularSueldoNetoBase(prestamo.getEmpleado());
        validarMargenLegalEmbargos(prestamo, sueldoNetoBase);

        return prestamoEmpleadoRepository.save(prestamo);
    }

    public void delete(PrestamoEmpleado prestamo) {
        if (prestamo.getEstado() != EstadoPrestamo.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden eliminar préstamos en estado PENDIENTE.");
        }
        prestamoEmpleadoRepository.delete(prestamo);
    }

    @Transactional
    public PrestamoEmpleado aprobar(PrestamoEmpleado prestamo) {
        validarNominaPendiente();

        if (prestamo.getEstado() != EstadoPrestamo.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar préstamos en estado PENDIENTE.");
        }

        BigDecimal sueldoNetoBase = calcularSueldoNetoBase(prestamo.getEmpleado());
        validarMargenLegalEmbargos(prestamo, sueldoNetoBase);

        prestamo.setFechaAprobacion(LocalDate.now());
        prestamo.setEstado(EstadoPrestamo.APROBADO);

        GastoOperativo gastoPrestamo = new GastoOperativo();
        gastoPrestamo.setTipoGasto(com.agroveterinaria.enums.TipoGasto.PRESTAMO_EMPLEADO);
        gastoPrestamo.setFecha(prestamo.getFechaAprobacion());
        gastoPrestamo.setMonto(prestamo.getMontoCapital());
        gastoPrestamo.setNotas("Préstamo a empleado: " +   prestamo.getEmpleado().getPersona().getNombre() + " " +
                prestamo.getEmpleado().getPersona().getApellido());

        gastoPrestamo = gastoOperativoRepository.save(gastoPrestamo);
        prestamo.setGastoAsociado(gastoPrestamo);

        return prestamoEmpleadoRepository.save(prestamo);
    }

    public void procesarCuotaMensual(PrestamoEmpleado prestamo, BigDecimal montoCobradoEnNomina) {
        BigDecimal balanceActual = prestamo.getBalanceCapitalPendiente();
        BigDecimal tasaMensual = prestamo.getTasaInteres().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 8, RoundingMode.HALF_UP);
        BigDecimal pagoInteres = balanceActual.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);

        BigDecimal pagoCapital;

        if (montoCobradoEnNomina.compareTo(pagoInteres) > 0) {
            pagoCapital = montoCobradoEnNomina.subtract(pagoInteres);
        } else {
            pagoCapital = BigDecimal.ZERO;
        }

        BigDecimal nuevoBalance = balanceActual.subtract(pagoCapital);

        if (nuevoBalance.compareTo(BigDecimal.ZERO) <= 0) {
            prestamo.setBalanceCapitalPendiente(BigDecimal.ZERO);
            prestamo.setEstado(EstadoPrestamo.SALDADO);
        } else {
            prestamo.setBalanceCapitalPendiente(nuevoBalance);
        }

        prestamo.setCuotasPagadas(prestamo.getCuotasPagadas() + 1);
        prestamoEmpleadoRepository.save(prestamo);
    }

    public AbonoPrestamo registrarAbonoExtraordinario(AbonoPrestamo abono) {
        validarNominaPendienteParaAbono();

        PrestamoEmpleado prestamo = abono.getPrestamo();

        if (prestamo.getEstado() != EstadoPrestamo.APROBADO) {
            throw new IllegalStateException("Solo se pueden registrar abonos a préstamos activos (APROBADOS).");
        }

        if (abono.getMonto().compareTo(prestamo.getBalanceCapitalPendiente()) > 0) {
            throw new IllegalArgumentException("El monto del abono (RD$ " + abono.getMonto() +
                    ") no puede ser mayor al balance pendiente (RD$ " + prestamo.getBalanceCapitalPendiente() + ").");
        }

        if (abono.getMetodoPago() == com.agroveterinaria.enums.MetodoPago.TRANSFERENCIA) {
            String ref = abono.getReferenciaTransferencia();
            if (ref == null || ref.isBlank()) {
                throw new IllegalArgumentException("La referencia bancaria es obligatoria para transferencias.");
            }

            if (abono.getComprobanteTransferencia() == null || abono.getComprobanteTransferencia().length == 0) {
                throw new IllegalArgumentException("El comprobante de transferencia es obligatorio.");
            }

            if (abonoPrestamoRepository.existsByReferenciaTransferenciaIgnoreCase(ref)) {
                throw new IllegalArgumentException("Error: La referencia bancaria '" + ref + "' ya fue utilizada en otro abono de préstamo.");
            }
        }

        BigDecimal nuevoBalance = prestamo.getBalanceCapitalPendiente().subtract(abono.getMonto());

        if (nuevoBalance.compareTo(BigDecimal.ZERO) <= 0) {
            prestamo.setBalanceCapitalPendiente(BigDecimal.ZERO);
            prestamo.setEstado(EstadoPrestamo.SALDADO);
            abono.setTipoRecalculo(TipoRecalculoPrestamo.REDUCIR_PLAZO);
        } else {
            prestamo.setBalanceCapitalPendiente(nuevoBalance);
            switch (abono.getTipoRecalculo()) {
                case REDUCIR_CUOTA:
                    int mesesRestantes = prestamo.getPlazoMeses() - prestamo.getCuotasPagadas();
                    if (mesesRestantes > 0) {
                        prestamo.setCuotaPeriodica(calcularCuotaFrancesa(prestamo.getBalanceCapitalPendiente(), prestamo.getTasaInteres(), mesesRestantes));
                    }
                    break;
                case REDUCIR_PLAZO:
                    break;
            }
        }
        prestamoEmpleadoRepository.save(prestamo);
        return abonoPrestamoRepository.save(abono);
    }

    public Map<String, BigDecimal> calcularLimitesParaUI(Empleado empleado) {
        BigDecimal sueldoNeto = calcularSueldoNetoBase(empleado);
        BigDecimal divisorLegal = configuracionNominaService.getDivisiorLimiteCuotaPrestamo();

        BigDecimal factorMinimo = configuracionNominaService.getPrestamoFactorMinimoSalario();
        BigDecimal factorMaximo = configuracionNominaService.getPrestamoFactorMaximoSalario();

        BigDecimal cuotaMaxima = empleado.getSalario().divide(divisorLegal, 2, RoundingMode.HALF_UP);

        BigDecimal maxTasaDecimal = configuracionNominaService.getTasaInteresMaximaPrestamo();
        BigDecimal maxTasaPorcentual = maxTasaDecimal.multiply(new BigDecimal("100"));

        return Map.of(
                "minMonto", sueldoNeto.multiply(factorMinimo),
                "maxMonto", sueldoNeto.multiply(factorMaximo),
                "maxMeses", BigDecimal.valueOf(configuracionNominaService.getPrestamoPlazoMaximoMeses()),
                "maxCuota", cuotaMaxima,
                "maxTasa", maxTasaPorcentual
        );
    }

    private void validarLimitesFinancieros(PrestamoEmpleado prestamo) {
        BigDecimal sueldoNeto = calcularSueldoNetoBase(prestamo.getEmpleado());

        BigDecimal factorMinimo = configuracionNominaService.getPrestamoFactorMinimoSalario();
        BigDecimal factorMaximo = configuracionNominaService.getPrestamoFactorMaximoSalario();
        int maxMeses = configuracionNominaService.getPrestamoPlazoMaximoMeses();

        BigDecimal minMonto = sueldoNeto.multiply(factorMinimo);
        BigDecimal maxMonto = sueldoNeto.multiply(factorMaximo);

        BigDecimal maxTasaDecimal = configuracionNominaService.getTasaInteresMaximaPrestamo();
        BigDecimal maxTasaPorcentual = maxTasaDecimal.multiply(new BigDecimal("100"));

        if (prestamo.getTasaInteres().compareTo(maxTasaPorcentual) > 0) {
            throw new IllegalArgumentException("La tasa de interés excede el máximo permitido del "
                    + maxTasaPorcentual.setScale(2, RoundingMode.HALF_UP) + "% anual.");
        }

        if (prestamo.getMontoCapital().compareTo(minMonto) < 0) {
            throw new IllegalArgumentException("El monto es inferior al 50% de su salario neto (Mínimo requerido: RD$ " + minMonto.setScale(2, RoundingMode.HALF_UP) + ").");
        }
        if (prestamo.getMontoCapital().compareTo(maxMonto) > 0) {
            throw new IllegalArgumentException("El monto excede 2 veces su salario neto (Máximo permitido: RD$ " + maxMonto.setScale(2, RoundingMode.HALF_UP) + ").");
        }
        if (prestamo.getPlazoMeses() > maxMeses) {
            throw new IllegalArgumentException("El plazo excede el máximo permitido de " + maxMeses + " meses.");
        }
    }

    private BigDecimal calcularSueldoNetoBase(Empleado empleado) {
        BigDecimal salarioBruto = empleado.getSalario();
        BigDecimal tss = configuracionNominaService.calcularAFP(salarioBruto).add(configuracionNominaService.calcularSFS(salarioBruto));
        BigDecimal baseGravable = salarioBruto.subtract(tss);

        BigDecimal isr = configuracionNominaService.calcularISR(baseGravable, com.agroveterinaria.enums.PeriodoNomina.MES);
        return baseGravable.subtract(isr);
    }

    private void validarNominaPendiente() {
        if (corridaNominaRepository.existsByEstado(EstadoCorrida.PENDIENTE)) {
            throw new IllegalStateException("Hay una corrida de nómina PENDIENTE. Debe aprobarla o eliminarla antes de procesar préstamos.");
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

    private void validarExclusividad(Empleado empleado) {
        if (anticipoSalarioRepository.existsByEmpleadoAndEstadoIn(empleado, Arrays.asList(EstadoAnticipo.PENDIENTE, EstadoAnticipo.APROBADO))) {
            throw new IllegalStateException("El empleado posee un Anticipo activo o en proceso de aprobación.");
        }
        if (prestamoEmpleadoRepository.existsByEmpleadoAndEstadoIn(empleado, Arrays.asList(EstadoPrestamo.PENDIENTE, EstadoPrestamo.APROBADO))) {
            throw new IllegalStateException("El empleado ya posee un Préstamo en proceso o aprobado.");
        }
    }

    private void validarMargenLegalEmbargos(PrestamoEmpleado prestamo, BigDecimal salarioNetoMensual) {
        Empleado empleado = prestamo.getEmpleado();
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
        BigDecimal cuotaPrestamo = prestamo.getCuotaPeriodica();

        int mesesPrestamo = prestamo.getPlazoMeses();
        LocalDate fechaActual = LocalDate.now();

        for (int i = 0; i < mesesPrestamo; i++) {
            LocalDate mesProyectado = fechaActual.plusMonths(i);
            int mesIteracion = mesProyectado.getMonthValue();
            int anioIteracion = mesProyectado.getYear();

            BigDecimal totalExtraordinarioDelMes = embargos.stream()
                    .flatMap(e -> e.getCuotasExtras().stream())
                    .filter(cuota -> cuota.getMesAplicacion() == mesIteracion)
                    .map(CuotaExtraEmbargo::getMontoExtra)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCuotasOrdEmbargos = embargos.stream()
                    .map(EmbargoSalarial::getMontoCuotaOrdinaria)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            BigDecimal cuotaSegura = (cuotaPrestamo != null) ? cuotaPrestamo : BigDecimal.ZERO;
            BigDecimal cargaTotalProyectada = totalCuotasOrdEmbargos
                    .add(totalExtraordinarioDelMes)
                    .add(cuotaSegura);

            if (cargaTotalProyectada.compareTo(limiteMensual) > 0) {
                lanzarErrorMargenLegal(
                        "Solicitud denegada: La suma de esta cuota y los embargos activos del empleado supera el límite legal de deducciones (50% del sueldo neto) para el mes "
                                + mesIteracion + "/" + anioIteracion + ". "
                                + "(Total proyectado: RD$ " + format(cargaTotalProyectada)
                                + " | Límite permitido: RD$ " + format(limiteMensual) + ")"
                );
            }
        }
    }

    public BigDecimal calcularCuotaFrancesa(BigDecimal capital, BigDecimal tasaInteresAnual, Integer plazoMeses) {
        if (tasaInteresAnual == null || tasaInteresAnual.compareTo(BigDecimal.ZERO) == 0) {
            return capital.divide(new BigDecimal(plazoMeses), 2, RoundingMode.HALF_UP);
        }

        BigDecimal tasaMensual = tasaInteresAnual
                .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 8, RoundingMode.HALF_UP);

        BigDecimal factor = BigDecimal.ONE.add(tasaMensual).pow(plazoMeses);
        BigDecimal numerador = capital.multiply(tasaMensual).multiply(factor);
        BigDecimal denominador = factor.subtract(BigDecimal.ONE);

        return numerador.divide(denominador, 2, RoundingMode.HALF_UP);
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

    public List<CuotaAmortizacionDTO> generarCuadroAmortizacion(BigDecimal balanceActual, BigDecimal tasaInteresAnual, BigDecimal cuotaFija) {
        List<CuotaAmortizacionDTO> cuadro = new ArrayList<>();
        BigDecimal balance = balanceActual;

        if (balance.compareTo(BigDecimal.ZERO) <= 0 || cuotaFija.compareTo(BigDecimal.ZERO) <= 0) {
            return cuadro;
        }

        BigDecimal tasaMensual = (tasaInteresAnual != null && tasaInteresAnual.compareTo(BigDecimal.ZERO) > 0)
                ? tasaInteresAnual.divide(new BigDecimal("1200"), 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int mesDeProyeccion = 1;
        int limiteSeguridad = 360;

        while (balance.compareTo(BigDecimal.ZERO) > 0 && mesDeProyeccion <= limiteSeguridad) {
            BigDecimal pagoInteres = balance.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);

            if (pagoInteres.compareTo(cuotaFija) >= 0 && tasaMensual.compareTo(BigDecimal.ZERO) > 0) {
                cuadro.add(new CuotaAmortizacionDTO(mesDeProyeccion, pagoInteres, BigDecimal.ZERO, cuotaFija, balance));
                break;
            }

            BigDecimal pagoCapital = cuotaFija.subtract(pagoInteres).setScale(2, RoundingMode.HALF_UP);

            if (balance.subtract(pagoCapital).compareTo(BigDecimal.ZERO) <= 0 || balance.compareTo(cuotaFija) <= 0) {
                pagoCapital = balance;
                cuotaFija = pagoCapital.add(pagoInteres);
                balance = BigDecimal.ZERO;
            } else {
                balance = balance.subtract(pagoCapital);
            }

            cuadro.add(new CuotaAmortizacionDTO(mesDeProyeccion, pagoInteres, pagoCapital, cuotaFija, balance));
            mesDeProyeccion++;
        }

        return cuadro;
    }

    public List<PrestamoEmpleado> findAll() {
        return prestamoEmpleadoRepository.findAllFetchEmpleado();
    }

    public List<PrestamoEmpleado> findByEmpleadoAndEstado(Empleado empleado) {
        return prestamoEmpleadoRepository.findByEmpleadoAndEstado(empleado, EstadoPrestamo.APROBADO);
    }

    public List<AbonoPrestamo> obtenerHistorialAbonos(Long idPrestamo) {
        return abonoPrestamoRepository.findByPrestamo_IdPrestamoOrderByFechaAbonoDesc(idPrestamo);
    }

    public boolean existsByEmpleado(Empleado empleado){
        return prestamoEmpleadoRepository.existsByEmpleado(empleado);
    }

    public boolean existsByEmpleadoAndEstado(Empleado empleado, EstadoPrestamo estado) {
        return prestamoEmpleadoRepository.existsByEmpleadoAndEstado(empleado, estado);
    }

    public boolean existsByEmpleadoAndEstadoIn(Empleado empleado, List<EstadoPrestamo> estados) {
        return prestamoEmpleadoRepository.existsByEmpleadoAndEstadoIn(empleado, estados);
    }
}