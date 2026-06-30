package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.*;
import com.agroveterinaria.repository.CorridaNominaRepository;
import com.agroveterinaria.repository.DetalleNominaRepository;
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
    private final VacacionEmpleadoService vacacionEmpleadoService;
    private final EmpleadoService empleadoService;
    private final PrestamoEmpleadoService prestamoEmpleadoService;
    private final EmbargoSalarialService embargoSalarialService;
    private final ConfiguracionNominaService configuracionNominaService;
    private final DiaFeriadoService diaFeriadoService;

    public List<CorridaNomina> findAllConNominas() {
        return corridaRepository.findAllConNominas();
    }

    public CorridaNomina generarCorrida(PeriodoNomina periodo, LocalDate fecha, TipoCorrida tipo, PeriodoFiscal periodoFiscal, Empleado empleadoEspecifico) {
        validarDisponibilidadDePeriodo(periodo, fecha);

        if (tipo == TipoCorrida.VACACIONES_ANTICIPADAS) {
            if (empleadoEspecifico == null) {
                throw new IllegalStateException("Debe seleccionar un empleado para generar vacaciones anticipadas.");
            }

            List<VacacionEmpleado> pendientes = vacacionEmpleadoService.findByEmpleadoAndPagadoPorAdelantadoFalse(empleadoEspecifico);
            if (pendientes == null || pendientes.isEmpty()) {
                throw new IllegalStateException("El empleado seleccionado no tiene vacaciones aprobadas pendientes de pago.");
            }
        }

        if (corridaRepository.existsByEstado(EstadoCorrida.PENDIENTE)) {
            throw new IllegalStateException("Acción denegada: Existe una corrida de nómina PENDIENTE en el sistema. Debe aprobarla o eliminarla antes de generar una nueva.");
        }

        if (corridaRepository.existsByPeriodoAndFechaEmisionAndTipo(periodo, fecha, tipo)) {
            throw new IllegalStateException("Ya existe una corrida de tipo " + tipo +
                    " para el período " + periodo + " en la fecha " + fecha+ "."
            );
        }

        if (tipo == TipoCorrida.REGALIA_PASCUAL) {
            validarRegaliaPascual(fecha);
        } else if (tipo == TipoCorrida.BONIFICACION) {
            if (periodoFiscal == null) {
                throw new IllegalStateException("Para generar bonificaciones, debe seleccionar un Período Fiscal.");
            }
            validarBonificacion(fecha, periodoFiscal);
        }

        CorridaNomina corrida = new CorridaNomina(periodo, fecha);
        corrida.setTipo(tipo);

        if (periodoFiscal != null) {
            corrida.setPeriodoFiscal(periodoFiscal);
        }

        List<Empleado> empleados = (empleadoEspecifico != null)
                ? List.of(empleadoEspecifico)
                : empleadoService.findByActivoTrue();
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

            boolean tienePagoVacaciones = nomina.getDetalles().stream()
                    .anyMatch(d -> d.getTipo() == TipoConcepto.PAGO_VACACIONES);

            if (tienePagoVacaciones) {
                List<VacacionEmpleado> vacacionesNoPagadas = vacacionEmpleadoService.findByEmpleadoAndPagadoPorAdelantadoFalse(nomina.getEmpleado());
                for (VacacionEmpleado vacacion : vacacionesNoPagadas) {
                    vacacion.setPagadoPorAdelantado(true);
                    vacacionEmpleadoService.save(vacacion);
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
        List<EmbargoSalarial> embargos = embargoSalarialService.findByEmpleadoAndActivoTrueOrderByFechaNotificacionAsc(empleado);

        if (embargos.isEmpty()) return;

        BigDecimal divisorEmbargo = configuracionNominaService.getDivisorLimiteEmbargo();
        BigDecimal limiteLegal = empleado.getSalario().divide(divisorEmbargo, 2, java.math.RoundingMode.HALF_UP);

        BigDecimal totalDemandado = embargos.stream()
                .map(EmbargoSalarial::getMontoDescuento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean excedeLimite = totalDemandado.compareTo(limiteLegal) > 0;

        boolean usarProrrateo = empleado.isProrratearEmbargos();

        BigDecimal totalEmbargado = BigDecimal.ZERO;

        for (EmbargoSalarial embargo : embargos) {
            BigDecimal montoADescontar = embargo.getMontoDescuento();

            if (excedeLimite) {
                if (usarProrrateo) {
                    BigDecimal proporcion = montoADescontar.divide(totalDemandado, 6, java.math.RoundingMode.HALF_UP);
                    montoADescontar = limiteLegal.multiply(proporcion).setScale(2, java.math.RoundingMode.HALF_UP);
                } else {
                    if (totalEmbargado.add(montoADescontar).compareTo(limiteLegal) > 0) {
                        montoADescontar = limiteLegal.subtract(totalEmbargado);
                    }
                }
            }
            
            if (montoADescontar.compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.EMBARGO_SALARIAL,
                        embargo.getTipo().getDescripcion() + ": " + embargo.getEntidadDemandante(), montoADescontar, 1.0));

                totalEmbargado = totalEmbargado.add(montoADescontar);
            }
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

        List<VacacionEmpleado> vacaciones = vacacionEmpleadoService.encontrarVacacionesEnPeriodo(empleado, inicioPeriodo, finPeriodo);
        BigDecimal montoTotalDescontarSalario = BigDecimal.ZERO;
        BigDecimal totalDevengado = BigDecimal.ZERO;

        if (!vacaciones.isEmpty()) {
            BigDecimal salarioDiario = calcularSalarioDiario(empleado);
            List<LocalDate> feriadosDelPeriodo = diaFeriadoService.obtenerFechasFeriadasEnRango(inicioPeriodo, finPeriodo);

            for (VacacionEmpleado vacacion : vacaciones) {
                LocalDate inicioReal = vacacion.getFechaInicio().isAfter(inicioPeriodo) ? vacacion.getFechaInicio() : inicioPeriodo;
                LocalDate finReal = vacacion.getFechaFin().isBefore(finPeriodo) ? vacacion.getFechaFin() : finPeriodo;

                long diasFisicosAusente = 0;
                LocalDate diaIterador = inicioReal;

                while (!diaIterador.isAfter(finReal)) {
                    boolean esDomingo = diaIterador.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
                    boolean esFeriado = feriadosDelPeriodo.contains(diaIterador);

                    if (!esDomingo && !esFeriado) {
                        diasFisicosAusente++;
                    }
                    diaIterador = diaIterador.plusDays(1);
                }

                montoTotalDescontarSalario = montoTotalDescontarSalario.add(salarioDiario.multiply(BigDecimal.valueOf(diasFisicosAusente)));

                boolean inicianEnEstePeriodo = !vacacion.getFechaInicio().isBefore(inicioPeriodo) && !vacacion.getFechaInicio().isAfter(finPeriodo);

                if (!vacacion.isPagadoPorAdelantado() && inicianEnEstePeriodo) {
                    BigDecimal montoVacacion = salarioDiario.multiply(BigDecimal.valueOf(vacacion.getCantidadDiasAPagar()));

                    detalles.add(crearDetalle(nomina, TipoConcepto.PAGO_VACACIONES,
                            "Vacaciones Ordinarias (" + vacacion.getCantidadDiasAPagar() + " días pagados)", montoVacacion, 1.0));

                    totalDevengado = totalDevengado.add(montoVacacion);
                }
            }
        }

        BigDecimal montoSalarioRestante = salarioDelPeriodo.subtract(montoTotalDescontarSalario);
        if (montoSalarioRestante.compareTo(BigDecimal.ZERO) > 0) {
            detalles.add(crearDetalle(nomina, TipoConcepto.SALARIO_BASE, "Salario base", montoSalarioRestante, 1.0));
            totalDevengado = totalDevengado.add(montoSalarioRestante);
        }

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

        cobrarEmbargosActivos(empleado, nomina, detalles);
        cobrarPrestamosActivos(empleado, nomina, detalles);
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
        int aniosSenior = configuracionNominaService.getAniosBonificacionSenior();
        BigDecimal diasTope = configuracionNominaService.getDiasBonificacionTope();
        BigDecimal diasBase = configuracionNominaService.getDiasBonificacionBase();

        int diasDelAnio = fechaCierreFiscal.lengthOfYear();

        Period tiempoLaborando = Period.between(fechaIngreso, fechaCierreFiscal);
        int anios = tiempoLaborando.getYears();

        if (anios >= aniosSenior) {
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
        List<VacacionEmpleado> vacacionesPendientes = vacacionEmpleadoService.findByEmpleadoAndPagadoPorAdelantadoFalse(empleado);
        BigDecimal totalDevengado = BigDecimal.ZERO;

        for (VacacionEmpleado vacacion : vacacionesPendientes) {
            BigDecimal salarioDiario = calcularSalarioDiario(empleado);
            BigDecimal montoVacacion = salarioDiario.multiply(BigDecimal.valueOf(vacacion.getCantidadDiasAPagar()));

            detalles.add(crearDetalle(nomina, TipoConcepto.PAGO_VACACIONES,
                    "Anticipo Vacaciones (" + vacacion.getCantidadDiasDescanso() + " días)", montoVacacion, 1.0));

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

            cobrarEmbargosActivos(empleado, nomina, detalles);
        }
    }

    private void validarRegaliaPascual(LocalDate fecha) {
        if (fecha.getMonth() != java.time.Month.DECEMBER) {
            throw new IllegalStateException("La Regalía Pascual solo puede generarse en el mes de diciembre.");
        }
        if (fecha.getDayOfMonth() > 20) {
            throw new IllegalStateException("La fecha límite legal para pagar la Regalía Pascual es el 20 de diciembre.");
        }
        if (corridaRepository.existeCorridaAnualPorTipo(TipoCorrida.REGALIA_PASCUAL, fecha.getYear())) {
            throw new IllegalStateException("Ya existe una Regalía Pascual pagada en este año.");
        }

        long corridasOrdinarias = corridaRepository.countByTipoAndAnio(TipoCorrida.ORDINARIA, fecha.getYear());
        if (corridasOrdinarias < 11) {
            throw new IllegalStateException("No se puede generar la regalía porque faltan nóminas ordinarias por procesar.");
        }
    }

    private void validarBonificacion(LocalDate fecha, PeriodoFiscal periodoFiscal) {
        LocalDate fechaCierre = LocalDate.of(periodoFiscal.getAnio(), 12, 31);
        LocalDate fechaMinima = fechaCierre.plusDays(90);
        LocalDate fechaMaxima = fechaCierre.plusDays(120);

        if (fecha.isBefore(fechaMinima) || fecha.isAfter(fechaMaxima)) {
            throw new IllegalStateException("La bonificación debe pagarse entre el 31 de marzo y el 30 de abril posterior al cierre fiscal.");
        }

        if (corridaRepository.existsByTipoAndPeriodoFiscal(TipoCorrida.BONIFICACION, periodoFiscal)) {
            throw new IllegalStateException("Ya se pagó la bonificación para este período fiscal.");
        }
    }

    public void validarDisponibilidadDePeriodo(PeriodoNomina periodoRequerido, LocalDate fecha) {
        LocalDate hoy = LocalDate.now();

        if (fecha.getYear() > hoy.getYear() || (fecha.getYear() == hoy.getYear() && fecha.getMonthValue() > hoy.getMonthValue())) {
            throw new IllegalStateException("El sistema no permite generar nóminas para meses futuros.");
        }

        LocalDate limiteAntiguedad = hoy.minusMonths(3).withDayOfMonth(1);
        if (fecha.isBefore(limiteAntiguedad)) {
            throw new IllegalStateException("No se pueden generar nóminas con más de 3 meses de antigüedad por políticas de cierre contable.");
        }

        LocalDate inicioMes = fecha.withDayOfMonth(1);
        LocalDate finMes = fecha.withDayOfMonth(fecha.lengthOfMonth());
        LocalDate mitadMes = fecha.withDayOfMonth(15);

        boolean existeMensual = corridaRepository.existsByPeriodoAndFechaEmisionBetween(PeriodoNomina.MES, inicioMes, finMes);
        if (existeMensual) {
            throw new IllegalStateException("Ya existe una nómina mensual generada para este mes.");
        }

        if (periodoRequerido == PeriodoNomina.MES) {
            boolean existeQuincena = corridaRepository.existsByPeriodoAndFechaEmisionBetween(PeriodoNomina.QUINCENA, inicioMes, finMes);
            if (existeQuincena) {
                throw new IllegalStateException("No puede generar una nómina mensual porque ya existen quincenas procesadas en este mes.");
            }

        } else if (periodoRequerido == PeriodoNomina.QUINCENA) {
            boolean esPrimeraQuincena = fecha.getDayOfMonth() <= 15;

            if (esPrimeraQuincena) {
                boolean existeQ1 = corridaRepository.existsByPeriodoAndFechaEmisionBetween(PeriodoNomina.QUINCENA, inicioMes, mitadMes);
                if (existeQ1) {
                    throw new IllegalStateException("La primera quincena de este mes ya fue generada.");
                }
            } else {
                boolean existeQ1 = corridaRepository.existsByPeriodoAndFechaEmisionBetween(PeriodoNomina.QUINCENA, inicioMes, mitadMes);
                if (!existeQ1) {
                    throw new IllegalStateException("No puede generar la segunda quincena sin haber procesado la primera. Genere una nómina Mensual.");
                }

                boolean existeQ2 = corridaRepository.existsByPeriodoAndFechaEmisionBetween(PeriodoNomina.QUINCENA, mitadMes.plusDays(1), finMes);
                if (existeQ2) {
                    throw new IllegalStateException("La segunda quincena de este mes ya fue generada.");
                }
            }
        }
    }
}
