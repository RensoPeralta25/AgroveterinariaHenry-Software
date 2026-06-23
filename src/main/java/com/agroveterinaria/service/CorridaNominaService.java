package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.*;
import com.agroveterinaria.repository.CorridaNominaRepository;
import com.agroveterinaria.repository.DetalleNominaRepository;
import com.agroveterinaria.repository.VacacionEmpleadoRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class CorridaNominaService {
    private final CorridaNominaRepository corridaRepository;
    private final DetalleNominaRepository detalleNominaRepository;
    private final VacacionEmpleadoRepository vacacionEmpleadoRepository;
    private final EmpleadoService empleadoService;
    private final PrestamoEmpleadoService prestamoEmpleadoService;
    private final EmbargoSalarialService embargoSalarialService;
    private final ConfiguracionNominaService configuracionNominaService;

    public List<CorridaNomina> findAllConNominas() {
        return corridaRepository.findAllConNominas();
    }

    public boolean existeCorridaEnPeriodo(PeriodoNomina periodo, LocalDate fecha) {
        LocalDate inicio = fecha.withDayOfMonth(1);
        LocalDate fin = fecha.withDayOfMonth(fecha.lengthOfMonth());
        return corridaRepository.existsByPeriodoAndFechaEmisionBetween(periodo, inicio, fin);
    }

    public CorridaNomina generarCorrida(PeriodoNomina periodo, LocalDate fecha, TipoCorrida tipo) {
        CorridaNomina corrida = new CorridaNomina(periodo, fecha);
        corrida.setTipo(tipo);
        List<Empleado> empleados = empleadoService.findByActivoTrue();
        Set<Nomina> nominas = new LinkedHashSet<>();

        for (Empleado empleado : empleados) {
            if (corrida.getTipo() != TipoCorrida.REGALIA_PASCUAL) {
                prestamoEmpleadoService.validarIntegridadPrestamos(empleado);
            }

            Nomina nomina = new Nomina(empleado, corrida);
            Set<DetalleNomina> detalles = new LinkedHashSet<>();

            switch (corrida.getTipo()) {
                case ORDINARIA:
                    procesarNominaOrdinaria(empleado, nomina, detalles, periodo, corrida.getFechaEmision());
                    break;
                case REGALIA_PASCUAL:
                    procesarRegaliaPascual(empleado, nomina, detalles, corrida.getFechaEmision());
                    break;
                case BONIFICACION:
                    if (corrida.getPeriodoFiscal() == null) {
                        throw new IllegalStateException("Para generar bonificaciones, debe seleccionar un Período Fiscal.");
                    }
                    procesarBonificacion(empleado, nomina, detalles, corrida.getPeriodoFiscal());
                    break;
                case VACACIONES_ANTICIPADAS:
                    procesarVacacionesAnticipadas(empleado, nomina, detalles);
                    break;
            }

            nomina.setDetalles(detalles);
            nomina.calcularSueldoNeto();
            nominas.add(nomina);
        }

        corrida.setNominas(nominas);
        return corridaRepository.save(corrida);
    }

    public CorridaNomina aprobarCorrida(CorridaNomina corrida) {
        validarEstadoPendiente(corrida);
        corrida.setEstado(EstadoCorrida.APROBADA);

        for (Nomina nomina : corrida.getNominas()) {
            BigDecimal totalDescontado = nomina.getDetalles().stream()
                    .filter(d -> d.getTipo() == TipoConcepto.PRESTAMO_EMPRESA)
                    .map(DetalleNomina::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalDescontado.compareTo(BigDecimal.ZERO) > 0) {
                List<PrestamoEmpleado> prestamos = prestamoEmpleadoService.findByEmpleadoAndEstado(nomina.getEmpleado());
                BigDecimal remanenteCobrado = totalDescontado;

                for (PrestamoEmpleado prestamo : prestamos) {
                    if (remanenteCobrado.compareTo(BigDecimal.ZERO) <= 0) break;

                    BigDecimal abonoAlPrestamo = remanenteCobrado.min(prestamo.getBalancePendiente());
                    prestamo.setBalancePendiente(prestamo.getBalancePendiente().subtract(abonoAlPrestamo));
                    remanenteCobrado = remanenteCobrado.subtract(abonoAlPrestamo);

                    if (prestamo.getBalancePendiente().compareTo(BigDecimal.ZERO) == 0) {
                        prestamo.setEstado(EstadoPrestamo.SALDADO);
                    }
                    prestamoEmpleadoService.save(prestamo);
                }
            }
        }

        if (corrida.getTipo() == TipoCorrida.VACACIONES_ANTICIPADAS) {
            for (Nomina nomina : corrida.getNominas()) {
                List<VacacionEmpleado> vacaciones = vacacionEmpleadoRepository.findByEmpleadoAndPagadoPorAdelantadoFalse(nomina.getEmpleado());
                for (VacacionEmpleado vacacion : vacaciones) {
                    vacacion.setPagadoPorAdelantado(true);
                    vacacionEmpleadoRepository.save(vacacion);
                }
            }
        }

        return corridaRepository.save(corrida);
    }

    public void delete(CorridaNomina corrida) {
        validarEstadoPendiente(corrida);
        corridaRepository.delete(corrida);
    }

    private DetalleNomina crearDetalle(Nomina nomina, TipoConcepto tipo,
                                       String descripcion, BigDecimal monto, Double cantidad) {
        DetalleNomina detalle = new DetalleNomina();
        detalle.setNomina(nomina);
        detalle.setTipo(tipo);
        detalle.setDescripcion(descripcion);
        detalle.setMonto(monto);
        detalle.setCantidad(BigDecimal.valueOf(cantidad));
        return detalle;
    }

    private void validarEstadoPendiente(CorridaNomina corrida) {
        if (corrida == null || corrida.getEstado() != EstadoCorrida.PENDIENTE) {
            throw new IllegalStateException("La corrida de nómina ya está aprobada, por lo que no se pueden realizar acciones sobre esta");
        }
    }

    private void cobrarPrestamosActivos(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles) {
        List<PrestamoEmpleado> prestamos = prestamoEmpleadoService.findByEmpleadoAndEstado(empleado);
        for (PrestamoEmpleado prestamo : prestamos) {
            BigDecimal montoACobrar = prestamo.getCuotaPeriodica().min(prestamo.getBalancePendiente());
            detalles.add(crearDetalle(nomina, TipoConcepto.PRESTAMO_EMPRESA,
                    "Cuota Préstamo: " + prestamo.getConcepto(), montoACobrar, 1.0));
        }
    }

    private void cobrarEmbargosActivos(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles) {
        List<EmbargoSalarial> embargos = embargoSalarialService.findByEmpleadoAndActivoTrue(empleado);
        for (EmbargoSalarial embargo : embargos) {
            detalles.add(crearDetalle(nomina, TipoConcepto.EMBARGO_SALARIAL,
                    "Embargo: " + embargo.getEntidadDemandante(), embargo.getMontoDescuento(), 1.0));
        }
    }

    private BigDecimal calcularSalarioDiario(Empleado empleado) {
        BigDecimal divisorOficial = configuracionNominaService.getDivisorMensualDiario();
        return empleado.getSalario().divide(divisorOficial, 2, java.math.RoundingMode.HALF_UP);
    }

    private void procesarNominaOrdinaria(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, PeriodoNomina periodo, LocalDate fechaEmision) {
        LocalDate inicioPeriodo, finPeriodo;

        if (periodo == PeriodoNomina.QUINCENA) {
            if (fechaEmision.getDayOfMonth() <= 15) {
                inicioPeriodo = fechaEmision.withDayOfMonth(1);
                finPeriodo = fechaEmision.withDayOfMonth(15);
            } else {
                inicioPeriodo = fechaEmision.withDayOfMonth(16);
                finPeriodo = fechaEmision.withDayOfMonth(fechaEmision.lengthOfMonth());
            }
        } else {
            inicioPeriodo = fechaEmision.withDayOfMonth(1);
            finPeriodo = fechaEmision.withDayOfMonth(fechaEmision.lengthOfMonth());
        }

        BigDecimal salarioDelPeriodo = empleado.getSalario();
        if (periodo == PeriodoNomina.QUINCENA) {
            salarioDelPeriodo = salarioDelPeriodo.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
        }

        List<VacacionEmpleado> vacaciones = vacacionEmpleadoRepository.encontrarVacacionesEnPeriodo(empleado, inicioPeriodo, finPeriodo);
        BigDecimal montoTotalVacaciones = BigDecimal.ZERO;
        BigDecimal totalDevengado = BigDecimal.ZERO;

        if (!vacaciones.isEmpty()) {
            BigDecimal salarioDiario = calcularSalarioDiario(empleado);

            for (VacacionEmpleado vacacion : vacaciones) {
                LocalDate inicioReal = vacacion.getFechaInicio().isAfter(inicioPeriodo) ? vacacion.getFechaInicio() : inicioPeriodo;
                LocalDate finReal = vacacion.getFechaFin().isBefore(finPeriodo) ? vacacion.getFechaFin() : finPeriodo;
                long diasSolapados = ChronoUnit.DAYS.between(inicioReal, finReal) + 1;

                BigDecimal montoVacacion = salarioDiario.multiply(BigDecimal.valueOf(diasSolapados));
                montoTotalVacaciones = montoTotalVacaciones.add(montoVacacion);

                if (!vacacion.isPagadoPorAdelantado()) {
                    detalles.add(crearDetalle(nomina, TipoConcepto.PAGO_VACACIONES,
                            "Vacaciones (" + diasSolapados + " días)", montoVacacion, 1.0));
                    totalDevengado = totalDevengado.add(montoVacacion);
                }
            }
        }

        BigDecimal montoSalarioRestante = salarioDelPeriodo.subtract(montoTotalVacaciones);
        if (montoSalarioRestante.compareTo(BigDecimal.ZERO) > 0) {
            detalles.add(crearDetalle(nomina, TipoConcepto.SALARIO_BASE, "Salario base", montoSalarioRestante, 1.0));
            totalDevengado = totalDevengado.add(montoSalarioRestante);
        }

        cobrarEmbargosActivos(empleado, nomina, detalles);
        cobrarPrestamosActivos(empleado, nomina, detalles);

        if (totalDevengado.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal afp = configuracionNominaService.calcularAFP(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.FONDO_PENSIONES, "AFP", afp, 1.0));

            BigDecimal sfs = configuracionNominaService.calcularSFS(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.SEGURO_FAMILIAR_SALUD, "SFS", sfs, 1.0));

            BigDecimal isr = configuracionNominaService.calcularISR(totalDevengado, periodo);
            if (isr.compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.IMPUESTO_RENTA, "ISR", isr, 1.0));
            }
        }
    }

    private void procesarRegaliaPascual(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, LocalDate fechaCorrida) {
        BigDecimal promedioAnual = calcularSueldo13(empleado, fechaCorrida);
        detalles.add(crearDetalle(nomina, TipoConcepto.SUELDO_13, "Sueldo 13 (Regalía Pascual)", promedioAnual, 1.0));
    }

    private void procesarBonificacion(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles,PeriodoFiscal periodoFiscal) {
        if (empleado.getFechaIngreso() == null) {
            throw new IllegalStateException("Error Crítico: El empleado " + empleado.getPersona().getNombre() +
                    " no tiene fecha de ingreso registrada, imposible calcular antigüedad.");
        }

        BigDecimal bonificacion = calcularMontoBonificacion(empleado, periodoFiscal);

        if (bonificacion.compareTo(BigDecimal.ZERO) == 0) return;

        detalles.add(crearDetalle(nomina, TipoConcepto.BONIFICACIONES, "Bonificación Anual", bonificacion, 1.0));

        BigDecimal isr = configuracionNominaService.calcularISR(bonificacion, PeriodoNomina.MES);
        if (isr.compareTo(BigDecimal.ZERO) > 0) {
            detalles.add(crearDetalle(nomina, TipoConcepto.IMPUESTO_RENTA, "ISR Bonificación", isr, 1.0));
        }
    }

    private BigDecimal calcularMontoBonificacion(Empleado empleado, PeriodoFiscal periodoFiscal) {
        LocalDate fechaIngreso = empleado.getFechaIngreso();
        LocalDate fechaCierreFiscal = periodoFiscal.getFechaCierre();

        if (fechaIngreso.isAfter(fechaCierreFiscal)) {
            return BigDecimal.ZERO;
        }

        BigDecimal salarioDiario = calcularSalarioDiario(empleado);
        BigDecimal diasTope = configuracionNominaService.getDiasBonificacionTope();
        BigDecimal diasBase = configuracionNominaService.getDiasBonificacionBase();

        int diasDelAnio = fechaCierreFiscal.lengthOfYear();

        Period tiempoLaborando = Period.between(fechaIngreso, fechaCierreFiscal);
        int anios = tiempoLaborando.getYears();

        if (anios >= 3) {
            return salarioDiario.multiply(diasTope).setScale(2, java.math.RoundingMode.HALF_UP);

        } else if (anios >= 1) {
            return salarioDiario.multiply(diasBase).setScale(2, java.math.RoundingMode.HALF_UP);

        } else {
            long diasTrabajados = ChronoUnit.DAYS.between(fechaIngreso, fechaCierreFiscal);

            BigDecimal factorProporcional = new BigDecimal(diasTrabajados)
                    .divide(new BigDecimal(diasDelAnio), 4, java.math.RoundingMode.HALF_UP);

            BigDecimal diasGanados = diasBase.multiply(factorProporcional);

            return salarioDiario.multiply(diasGanados).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calcularSueldo13(Empleado empleado, LocalDate fechaCorrida) {
        int anio = fechaCorrida.getYear();
        LocalDate inicioAnio = LocalDate.of(anio, 1, 1);
        LocalDate finAnio = LocalDate.of(anio, 12, 31);

        List<TipoConcepto> conceptosOrdinarios = List.of(
                TipoConcepto.SALARIO_BASE,
                TipoConcepto.COMISIONES_REGULARES,
                TipoConcepto.PAGO_VACACIONES
        );

        BigDecimal totalGanado = detalleNominaRepository.sumarSalarioOrdinarioDelAnio(empleado, inicioAnio, finAnio, conceptosOrdinarios);

        return totalGanado.divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP);
    }

    private void procesarVacacionesAnticipadas(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles) {
        List<VacacionEmpleado> vacacionesPendientes = vacacionEmpleadoRepository.findByEmpleadoAndPagadoPorAdelantadoFalse(empleado);
        BigDecimal totalDevengado = BigDecimal.ZERO;

        for (VacacionEmpleado vacacion : vacacionesPendientes) {
            BigDecimal salarioDiario = calcularSalarioDiario(empleado);
            BigDecimal montoVacacion = salarioDiario.multiply(BigDecimal.valueOf(vacacion.getCantidadDias()));

            detalles.add(crearDetalle(nomina, TipoConcepto.PAGO_VACACIONES,
                    "Anticipo Vacaciones (" + vacacion.getCantidadDias() + " días)", montoVacacion, 1.0));

            totalDevengado = totalDevengado.add(montoVacacion);
        }

        if (totalDevengado.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal afp = configuracionNominaService.calcularAFP(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.FONDO_PENSIONES, "AFP", afp, 1.0));

            BigDecimal sfs = configuracionNominaService.calcularSFS(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.SEGURO_FAMILIAR_SALUD, "SFS", sfs, 1.0));

            BigDecimal isr = configuracionNominaService.calcularISR(totalDevengado, PeriodoNomina.MES);
            if (isr.compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.IMPUESTO_RENTA, "ISR", isr, 1.0));
            }
        }
    }
}
