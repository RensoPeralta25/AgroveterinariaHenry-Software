package com.agroveterinaria.service;

import com.agroveterinaria.dto.nomina.NovedadNominaDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.*;
import com.agroveterinaria.repository.AnticipoSalarioRepository;
import com.agroveterinaria.repository.CorridaNominaRepository;
import com.agroveterinaria.repository.DetalleNominaRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class CorridaNominaService {
    private static final BigDecimal FACTOR_QUINCENA = new BigDecimal("2");
    private static final BigDecimal FACTOR_SEMANAL = BigDecimal.valueOf(52)
            .divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

    private final CorridaNominaRepository corridaRepository;
    private final DetalleNominaRepository detalleNominaRepository;
    private final VacacionEmpleadoService vacacionEmpleadoService;
    private final EmpleadoService empleadoService;
    private final PrestamoEmpleadoService prestamoEmpleadoService;
    private final EmbargoSalarialService embargoSalarialService;
    private final ConfiguracionNominaService configuracionNominaService;
    private final DiaFeriadoService diaFeriadoService;
    private final PeriodoFiscalService periodoFiscalService;
    private final AnticipoSalarioRepository anticipoSalarioRepository;

    public List<CorridaNomina> findAllConNominas() {
        return corridaRepository.findAllConNominas();
    }

    public CorridaNomina generarCorrida(PeriodoNomina periodo, LocalDate fecha, TipoCorrida tipo, PeriodoFiscal periodoFiscal, Empleado empleadoEspecifico, List<NovedadNominaDTO> novedades) {
        validarDisponibilidadDePeriodo(periodo, fecha, tipo);

        if (tipo == TipoCorrida.VACACIONES_ANTICIPADAS) {
            if (empleadoEspecifico == null) {
                throw new IllegalStateException("Debe seleccionar un empleado para generar vacaciones anticipadas.");
            }

            List<VacacionEmpleado> pendientes = vacacionEmpleadoService.findByEmpleadoYNoPagadas(empleadoEspecifico).stream()
                    .filter(v -> v.getEstado() == EstadoVacacion.APROBADA)
                    .collect(Collectors.toList());

            if (pendientes.isEmpty()) {
                throw new IllegalStateException("El empleado seleccionado no tiene vacaciones aprobadas pendientes de pago.");
            }

            boolean tieneVacacionEnRango = false;

            for (VacacionEmpleado vac : pendientes) {
                long diasAnticipacion = ChronoUnit.DAYS.between(LocalDate.now(), vac.getFechaInicio());
                if (diasAnticipacion >= 0 && diasAnticipacion <= 14) {
                    tieneVacacionEnRango = true;
                    break;
                }
            }

            if (!tieneVacacionEnRango) {
                throw new IllegalStateException("El empleado tiene vacaciones pendientes, pero ninguna está dentro del rango legal para pago (0 a 14 días antes del inicio).");
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
        } else {
            periodoFiscalService.buscarPorFecha(fecha)
                    .ifPresent(periodoActivo -> corrida.setPeriodoFiscal(periodoActivo));
        }

        List<Empleado> empleados = (empleadoEspecifico != null)
                ? List.of(empleadoEspecifico)
                : empleadoService.findByStatus(StatusEntidad.ACTIVO);
        Set<Nomina> nominas = new LinkedHashSet<>();

        Map<Long, NovedadNominaDTO> mapaNovedades = (novedades != null)
                ? novedades.stream().collect(Collectors.toMap(n -> n.getEmpleado().getIdEmpleado(), n -> n))
                : Collections.emptyMap();

        for (Empleado empleado : empleados) {
            Nomina nomina = new Nomina(empleado, corrida);
            Set<DetalleNomina> detalles = new LinkedHashSet<>();

            NovedadNominaDTO novedadDelEmpleado = mapaNovedades.get(empleado.getIdEmpleado());

            if (novedadDelEmpleado != null && corrida.getTipo() == TipoCorrida.ORDINARIA) {
                int maxDiasPermitidos = switch (corrida.getPeriodo()) {
                    case MES -> corrida.getFechaEmision().lengthOfMonth();
                    case SEMANAL -> 6;
                    case QUINCENA -> (corrida.getFechaEmision().getDayOfMonth() <= 15) ? 15 : (corrida.getFechaEmision().lengthOfMonth() - 15);
                };
                validarIntegridadNovedades(novedadDelEmpleado, periodo, maxDiasPermitidos, empleado);
            }

            switch (corrida.getTipo()) {
                case ORDINARIA:
                    procesarNominaOrdinaria(empleado, nomina, detalles, periodo, corrida.getFechaEmision(), novedadDelEmpleado);
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

            BigDecimal totalDescontadoAnticipo = nomina.getDetalles().stream()
                    .filter(d -> d.getTipo() == TipoConcepto.ANTICIPO_SALARIO)
                    .map(DetalleNomina::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDevengado = nomina.getDetalles().stream()
                    .filter(d -> d.getTipo().esIngreso())
                    .map(DetalleNomina::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDeducciones = nomina.getDetalles().stream()
                    .filter(d -> !d.getTipo().esIngreso())
                    .map(DetalleNomina::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netoReal = totalDevengado.subtract(totalDeducciones);

            if (netoReal.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("El sueldo neto del empleado "
                        + nomina.getEmpleado().getPersona().getNombre() + " no puede ser negativo (RD$ " + netoReal + ").");
            }

            double diasAusencias = nomina.getDetalles().stream()
                    .filter(d -> d.getTipo() == TipoConcepto.AUSENCIAS_NO_PAGADAS)
                    .map(d -> d.getCantidad().doubleValue())
                    .findFirst().orElse(0.0);

            int maxDiasPermitidos = switch (corrida.getPeriodo()) {
                case MES -> corrida.getFechaEmision().lengthOfMonth();
                case SEMANAL -> 6;
                case QUINCENA -> (corrida.getFechaEmision().getDayOfMonth() <= 15) ? 15 : (corrida.getFechaEmision().lengthOfMonth() - 15);
            };

            if (diasAusencias > maxDiasPermitidos) {
                throw new IllegalStateException("Las ausencias de "
                        + nomina.getEmpleado().getPersona().getNombre() + " (" + diasAusencias + " días) superan el límite del período (" + maxDiasPermitidos + " días).");
            }

            if (totalDescontadoAnticipo.compareTo(BigDecimal.ZERO) > 0) {
                List<AnticipoSalario> anticipos = anticipoSalarioRepository.findByEmpleadoIdEmpleadoAndEstado(nomina.getEmpleado().getIdEmpleado(), EstadoAnticipo.APROBADO);
                BigDecimal remanenteCobrado = totalDescontadoAnticipo;

                for (AnticipoSalario anticipo : anticipos) {
                    if (remanenteCobrado.compareTo(BigDecimal.ZERO) <= 0) break;

                    BigDecimal abonoAlAnticipo = remanenteCobrado.min(anticipo.getSaldoPendiente());
                    anticipo.setMontoDescontado(anticipo.getMontoDescontado().add(abonoAlAnticipo));
                    anticipo.setSaldoPendiente(anticipo.getSaldoPendiente().subtract(abonoAlAnticipo));
                    remanenteCobrado = remanenteCobrado.subtract(abonoAlAnticipo);

                    if (anticipo.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
                        anticipo.setSaldoPendiente(BigDecimal.ZERO);
                        anticipo.setEstado(EstadoAnticipo.SALDADO);
                    }
                    anticipoSalarioRepository.save(anticipo);
                }
            }

            if (totalDescontado.compareTo(BigDecimal.ZERO) > 0) {
                List<PrestamoEmpleado> prestamos = prestamoEmpleadoService.findByEmpleadoAndEstado(nomina.getEmpleado());
                BigDecimal remanenteCobrado = totalDescontado;

                for (PrestamoEmpleado prestamo : prestamos) {
                    if (remanenteCobrado.compareTo(BigDecimal.ZERO) <= 0) break;

                    BigDecimal abonoAlPrestamo = remanenteCobrado.min(prestamo.getBalanceCapitalPendiente());
                    prestamoEmpleadoService.procesarCuotaMensual(prestamo, abonoAlPrestamo);
                    remanenteCobrado = remanenteCobrado.subtract(abonoAlPrestamo);
                }
            }

            int mesActual = corrida.getFechaEmision().getMonthValue();
            int anioActual = corrida.getFechaEmision().getYear();

            List<EmbargoSalarial> embargos = embargoSalarialService.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(nomina.getEmpleado())
                    .stream()
                    .filter(e -> !e.getFechaNotificacion().isAfter(corrida.getFechaEmision()))
                    .collect(Collectors.toList());


            if (corrida.getTipo() == TipoCorrida.REGALIA_PASCUAL || corrida.getTipo() == TipoCorrida.BONIFICACION) {
                embargos = embargos.stream()
                        .filter(e -> e.getTipoEmbargo() == TipoEmbargo.PENSION_ALIMENTICIA)
                        .collect(Collectors.toList());
            }

            for (EmbargoSalarial embargo : embargos) {
                BigDecimal cuotaOrdinaria;
                if (corrida.getTipo() == TipoCorrida.REGALIA_PASCUAL || corrida.getTipo() == TipoCorrida.BONIFICACION) {
                    cuotaOrdinaria = embargo.getMontoCuotaOrdinaria() != null ? embargo.getMontoCuotaOrdinaria() : BigDecimal.ZERO;
                } else {
                    cuotaOrdinaria = calcularCuotaEsperadaPorPeriodo(embargo.getMontoCuotaOrdinaria(), corrida.getPeriodo());
                }

                BigDecimal moraAcumulada = embargo.getSaldoPendienteMora() != null ? embargo.getSaldoPendienteMora() : BigDecimal.ZERO;
                BigDecimal cuotasExtras = calcularCuotasExtras(embargo, mesActual, anioActual);
                BigDecimal montoRequerido = cuotaOrdinaria.add(moraAcumulada).add(cuotasExtras);

                String descripcionEsperada = embargo.getTipoEmbargo().name() + ": " + embargo.getEntidadDemandante() + " (Ref: #" + embargo.getIdEmbargo() + ")";
                BigDecimal montoCobrado = nomina.getDetalles().stream()
                        .filter(d -> d.getTipo() == TipoConcepto.EMBARGO_SALARIAL && d.getDescripcion().equals(descripcionEsperada))
                        .map(DetalleNomina::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal diferenciaNoCobrada = montoRequerido.subtract(montoCobrado);

                embargo.setSaldoPendienteMora(diferenciaNoCobrada);
                embargoSalarialService.save(embargo);

                if (cuotasExtras.compareTo(BigDecimal.ZERO) > 0) {
                    embargo.getCuotasExtras().stream()
                            .filter(c -> c.getMesAplicacion() == mesActual && c.getUltimoAnioCobrado() < anioActual)
                            .forEach(c -> c.setUltimoAnioCobrado(anioActual));
                }
            }

            boolean tienePagoVacaciones = nomina.getDetalles().stream()
                    .anyMatch(d -> d.getTipo() == TipoConcepto.PAGO_VACACIONES);

            if (tienePagoVacaciones) {
                List<VacacionEmpleado> vacacionesNoPagadas = vacacionEmpleadoService.findByEmpleadoYNoPagadas(nomina.getEmpleado());
                for (VacacionEmpleado vacacion : vacacionesNoPagadas) {
                    if (vacacion.getEstado() == EstadoVacacion.APROBADA) {
                        vacacionEmpleadoService.marcarComoPagada(vacacion);
                    }
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

    private BigDecimal calcularSalarioDiario(Empleado empleado) {
        BigDecimal divisorOficial = configuracionNominaService.getDivisorMensualDiario();
        BigDecimal salarioBase = empleado.getSalario();

        LocalDate hoy = LocalDate.now();
        LocalDate fechaInicioCalculo = hoy.minusYears(1);

        if (empleado.getFechaIngreso().isAfter(fechaInicioCalculo)) {
            fechaInicioCalculo = empleado.getFechaIngreso();
        }

        long mesesTrabajados = ChronoUnit.MONTHS.between(fechaInicioCalculo, hoy);
        if (mesesTrabajados <= 0)
            mesesTrabajados = 1;

        BigDecimal totalComisionesRegulares = detalleNominaRepository.sumarTotalPorConceptoYRangoDeFechas(
                empleado,
                TipoConcepto.COMISIONES_REGULARES,
                fechaInicioCalculo,
                hoy
        );

        if (totalComisionesRegulares == null)
            totalComisionesRegulares = BigDecimal.ZERO;


        BigDecimal promedioMensualComisiones = totalComisionesRegulares.divide(
                BigDecimal.valueOf(mesesTrabajados), 2, RoundingMode.HALF_UP);

        BigDecimal salarioComputable = salarioBase.add(promedioMensualComisiones);

        return salarioComputable.divide(divisorOficial, 2, RoundingMode.HALF_UP);
    }

    private void procesarNominaOrdinaria(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, PeriodoNomina periodo, LocalDate fechaEmision, NovedadNominaDTO novedad) {
        LocalDate inicioPeriodo, finPeriodo;

        if (periodo == PeriodoNomina.QUINCENA) {
            if (fechaEmision.getDayOfMonth() <= 15) {
                inicioPeriodo = fechaEmision.withDayOfMonth(1);
                finPeriodo = fechaEmision.withDayOfMonth(15);
            } else {
                inicioPeriodo = fechaEmision.withDayOfMonth(16);
                finPeriodo = fechaEmision.withDayOfMonth(fechaEmision.lengthOfMonth());
            }
        } else if (periodo == PeriodoNomina.SEMANAL) {
            inicioPeriodo = fechaEmision.minusDays(6);
            finPeriodo = fechaEmision;
        } else {
            inicioPeriodo = fechaEmision.withDayOfMonth(1);
            finPeriodo = fechaEmision.withDayOfMonth(fechaEmision.lengthOfMonth());
        }

        BigDecimal salarioDelPeriodo = empleado.getSalario();
        if (periodo == PeriodoNomina.QUINCENA) {
            salarioDelPeriodo = salarioDelPeriodo.divide(FACTOR_QUINCENA, 2, RoundingMode.HALF_UP);
        } else if (periodo == PeriodoNomina.SEMANAL) {
            salarioDelPeriodo = salarioDelPeriodo.divide(FACTOR_SEMANAL, 2, RoundingMode.HALF_UP);
        }

        List<VacacionEmpleado> vacaciones = vacacionEmpleadoService.encontrarVacacionesEnPeriodo(empleado, inicioPeriodo, finPeriodo);
        BigDecimal montoTotalDescontarSalario = BigDecimal.ZERO;
        BigDecimal totalDevengado = BigDecimal.ZERO;

        BigDecimal divisorOficial = configuracionNominaService.getDivisorMensualDiario();
        BigDecimal salarioBaseDiario = empleado.getSalario().divide(divisorOficial, 2, java.math.RoundingMode.HALF_UP);

        if (!vacaciones.isEmpty()) {
            BigDecimal salarioDiarioComputable = calcularSalarioDiario(empleado);

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

                montoTotalDescontarSalario = montoTotalDescontarSalario.add(salarioBaseDiario.multiply(BigDecimal.valueOf(diasFisicosAusente)));

                boolean inicianEnEstePeriodo = !vacacion.getFechaInicio().isBefore(inicioPeriodo) && !vacacion.getFechaInicio().isAfter(finPeriodo);

                if (vacacion.getEstado() == EstadoVacacion.APROBADA && inicianEnEstePeriodo) {
                    BigDecimal montoVacacion = salarioDiarioComputable.multiply(BigDecimal.valueOf(vacacion.getCantidadDiasAPagar()));

                    detalles.add(crearDetalle(nomina, TipoConcepto.PAGO_VACACIONES,
                            "Vacaciones Ordinarias (" + vacacion.getCantidadDiasAPagar() + " días pagados)", montoVacacion, 1.0));

                    totalDevengado = totalDevengado.add(montoVacacion);
                }
            }
        }

        if (novedad != null && novedad.getAusenciasNoPagadasDias() != null && novedad.getAusenciasNoPagadasDias() > 0) {
            BigDecimal deduccionAusencias = salarioBaseDiario.multiply(BigDecimal.valueOf(novedad.getAusenciasNoPagadasDias()));

            montoTotalDescontarSalario = montoTotalDescontarSalario.add(deduccionAusencias);

            detalles.add(crearDetalle(nomina, TipoConcepto.AUSENCIAS_NO_PAGADAS,
                    "Ausencias no pagadas (" + novedad.getAusenciasNoPagadasDias() + " días)", deduccionAusencias, novedad.getAusenciasNoPagadasDias().doubleValue()));
        }

        BigDecimal montoSalarioRestante = salarioDelPeriodo.subtract(montoTotalDescontarSalario);
        if (montoSalarioRestante.compareTo(BigDecimal.ZERO) > 0) {
            detalles.add(crearDetalle(nomina, TipoConcepto.SALARIO_BASE, "Salario base", montoSalarioRestante, 1.0));
            totalDevengado = totalDevengado.add(montoSalarioRestante);
        }

        if (novedad != null) {
            if (novedad.getHorasExtras() != null && novedad.getHorasExtras() > 0) {
                BigDecimal valorHoras = configuracionNominaService.calcularHorasExtras(novedad.getHorasExtras().doubleValue());
                detalles.add(crearDetalle(nomina, TipoConcepto.HORAS_EXTRAS, "Horas extras", valorHoras, novedad.getHorasExtras().doubleValue()));
                totalDevengado = totalDevengado.add(valorHoras);
            }

            if (novedad.getComisionesRegulares() != null && novedad.getComisionesRegulares().compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.COMISIONES_REGULARES, "Comisiones Regulares", novedad.getComisionesRegulares(), 1.0));
                totalDevengado = totalDevengado.add(novedad.getComisionesRegulares());
            }

            if (novedad.getComisionesExtraordinarias() != null && novedad.getComisionesExtraordinarias().compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.COMISIONES_EXTRAORDINARIAS, "Comisiones Extraordinarias / Bonos", novedad.getComisionesExtraordinarias(), 1.0));
                totalDevengado = totalDevengado.add(novedad.getComisionesExtraordinarias());
            }

            if (novedad.getDietasViaticos() != null && novedad.getDietasViaticos().compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.DIETAS_Y_VIATICOS, "Dietas y viáticos", novedad.getDietasViaticos(), 1.0));
                totalDevengado = totalDevengado.add(novedad.getDietasViaticos());
            }
        }

        if (totalDevengado.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal afp = configuracionNominaService.calcularAFP(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.FONDO_PENSIONES, "AFP", afp, 1.0));

            BigDecimal sfs = configuracionNominaService.calcularSFS(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.SEGURO_FAMILIAR_SALUD, "SFS", sfs, 1.0));

            BigDecimal deduccionesTss = afp.add(sfs);
            BigDecimal baseGravable = totalDevengado.subtract(deduccionesTss);

            BigDecimal isr = configuracionNominaService.calcularISR(baseGravable, periodo);

            if (isr.compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.IMPUESTO_RENTA, "ISR", isr, 1.0));
            }

            BigDecimal deduccionesLey = afp.add(sfs).add(isr);
            BigDecimal salarioNeto = totalDevengado.subtract(deduccionesLey);

            BigDecimal porcentajeLimite = configuracionNominaService.getPorcentajeLimiteEmbargo();
            BigDecimal limiteEmbargable = salarioNeto.multiply(porcentajeLimite);

            ejecutarDeduccionesPorPrioridad(empleado, nomina, detalles, limiteEmbargable);
        }

    }

    private void ejecutarDeduccionesPorPrioridad(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, BigDecimal limiteDisponible) {
        List<EmbargoSalarial> embargosActivos = embargoSalarialService.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(empleado)
                .stream()
                .filter(e -> !e.getFechaNotificacion().isAfter(nomina.getCorrida().getFechaEmision()))
                .collect(Collectors.toList());


        int mesActual = nomina.getCorrida().getFechaEmision().getMonthValue();
        int anioActual = nomina.getCorrida().getFechaEmision().getYear();

        PeriodoNomina periodoActual = nomina.getCorrida().getPeriodo();

        Map<Integer, List<EmbargoSalarial>> embargosPorPrioridad = embargosActivos.stream()
                .collect(Collectors.groupingBy(e -> e.getTipoEmbargo().getPrioridad()));

        List<Integer> nivelesPrioridad = embargosPorPrioridad.keySet().stream().sorted().toList();

        for (Integer prioridad : nivelesPrioridad) {
            if (limiteDisponible.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            List<EmbargoSalarial> embargosNivel = embargosPorPrioridad.get(prioridad);

            BigDecimal totalRequeridoNivel = embargosNivel.stream()
                    .map(e -> calcularMontoRequeridoEmbargo(e, mesActual, anioActual, periodoActual))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalRequeridoNivel.compareTo(BigDecimal.ZERO) == 0) continue;

            if (totalRequeridoNivel.compareTo(limiteDisponible) <= 0) {
                for (EmbargoSalarial embargo : embargosNivel) {
                    BigDecimal montoACobrar = calcularMontoRequeridoEmbargo(embargo, mesActual,  anioActual, periodoActual);
                    if (montoACobrar.compareTo(BigDecimal.ZERO) > 0) {
                        String descripcionUnica = embargo.getTipoEmbargo().getDescripcion() + ": " + embargo.getEntidadDemandante() + " (Ref: #" + embargo.getIdEmbargo() + ")";
                        detalles.add(crearDetalle(nomina, TipoConcepto.EMBARGO_SALARIAL,
                                descripcionUnica, montoACobrar, 1.0));
                        limiteDisponible = limiteDisponible.subtract(montoACobrar);
                    }
                }
            } else {
                BigDecimal fondoOriginalNivel = limiteDisponible;
                BigDecimal fondoRestante = limiteDisponible;

                for (int i = 0; i < embargosNivel.size(); i++) {
                    EmbargoSalarial embargo = embargosNivel.get(i);
                    BigDecimal deudaEmbargo = calcularMontoRequeridoEmbargo(embargo, mesActual, anioActual,periodoActual);

                    if (deudaEmbargo.compareTo(BigDecimal.ZERO) == 0) continue;

                    BigDecimal proporcion = deudaEmbargo.divide(totalRequeridoNivel, 6, RoundingMode.HALF_UP);
                    BigDecimal montoAsignado = fondoOriginalNivel.multiply(proporcion).setScale(2, RoundingMode.HALF_UP);

                    if (i == embargosNivel.size() - 1 || montoAsignado.compareTo(fondoRestante) > 0) {
                        montoAsignado = fondoRestante.min(deudaEmbargo);
                    }

                    if (montoAsignado.compareTo(BigDecimal.ZERO) > 0) {
                        String descripcionUnica = embargo.getTipoEmbargo().name() + ": " + embargo.getEntidadDemandante() + " (Ref: #" + embargo.getIdEmbargo() + ")";
                        detalles.add(crearDetalle(nomina, TipoConcepto.EMBARGO_SALARIAL,
                                descripcionUnica, montoAsignado, 1.0));
                        fondoRestante = fondoRestante.subtract(montoAsignado);
                    }
                }
                limiteDisponible = BigDecimal.ZERO;
            }
        }

        if (limiteDisponible.compareTo(BigDecimal.ZERO) >= 0) {
            limiteDisponible = procesarPrestamosConLimite(empleado, nomina, detalles, limiteDisponible);
        }

        if (limiteDisponible.compareTo(BigDecimal.ZERO) > 0) {
            procesarAnticiposConLimite(empleado, nomina, detalles, limiteDisponible);
        }
    }

    private BigDecimal calcularMontoRequeridoEmbargo(EmbargoSalarial embargo, int mesActual, int anioActual, PeriodoNomina periodo) {
        BigDecimal cuotaMensual = embargo.getMontoCuotaOrdinaria() != null ? embargo.getMontoCuotaOrdinaria() : BigDecimal.ZERO;
        BigDecimal cuotaOrdinaria = calcularCuotaEsperadaPorPeriodo(cuotaMensual, periodo);
        BigDecimal mora = embargo.getSaldoPendienteMora() != null ? embargo.getSaldoPendienteMora() : BigDecimal.ZERO;
        BigDecimal extras = calcularCuotasExtras(embargo, mesActual, anioActual);
        return cuotaOrdinaria.add(mora).add(extras);
    }

    private BigDecimal calcularCuotasExtras(EmbargoSalarial embargo, int mesActual, int anioActual) {
        return embargo.getCuotasExtras().stream()
                .filter(cuota -> cuota.getMesAplicacion() == mesActual && cuota.getUltimoAnioCobrado() < anioActual)
                .map(CuotaExtraEmbargo::getMontoExtra)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal procesarPrestamosConLimite(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, BigDecimal limiteDisponible) {
        List<PrestamoEmpleado> prestamos = prestamoEmpleadoService.findByEmpleadoAndEstado(empleado);
        LocalDate fechaNomina = nomina.getCorrida().getFechaEmision();

        for (PrestamoEmpleado prestamo : prestamos) {
            if (prestamo.getFechaAprobacion() != null && prestamo.getFechaAprobacion().isAfter(fechaNomina)) {
                continue;
            }

            BigDecimal cuotaEsperadaPeriodo = calcularCuotaEsperadaPorPeriodo(prestamo.getCuotaPeriodica(), nomina.getCorrida().getPeriodo());

            BigDecimal interesProyectado = prestamo.getBalanceCapitalPendiente()
                    .multiply(prestamo.getTasaInteres().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                            .divide(new BigDecimal("12"), 8, RoundingMode.HALF_UP));
            BigDecimal balanceTotalAlCierre = prestamo.getBalanceCapitalPendiente().add(interesProyectado);

            BigDecimal montoRequerido = cuotaEsperadaPeriodo.min(balanceTotalAlCierre);
            if (limiteDisponible.compareTo(montoRequerido) >= 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.PRESTAMO_EMPRESA,
                        "Cuota Préstamo: " + prestamo.getConcepto(), montoRequerido, 1.0));

                limiteDisponible = limiteDisponible.subtract(montoRequerido);
            } else {
                detalles.add(crearDetalle(nomina, TipoConcepto.PRESTAMO_EMPRESA,
                        "Préstamo omitido (Fondos insuficientes): " + prestamo.getConcepto(), BigDecimal.ZERO, 1.0));
            }
        }
        return limiteDisponible;
    }

    private void procesarAnticiposConLimite(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, BigDecimal limiteDisponible) {
        List<AnticipoSalario> anticipos = anticipoSalarioRepository.findByEmpleadoIdEmpleadoAndEstado(empleado.getIdEmpleado(), EstadoAnticipo.APROBADO);

        for (AnticipoSalario anticipo : anticipos) {
            if (limiteDisponible.compareTo(BigDecimal.ZERO) <= 0) break;
            if (anticipo.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal montoACobrar = anticipo.getCuotaDescuento()
                    .min(anticipo.getSaldoPendiente())
                    .min(limiteDisponible);

            if (montoACobrar.compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.ANTICIPO_SALARIO,
                        "Descuento Automático Anticipo", montoACobrar, 1.0));
                limiteDisponible = limiteDisponible.subtract(montoACobrar);
            }
        }
    }

    private void procesarRegaliaPascual(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, LocalDate fechaCorrida) {
        BigDecimal promedioAnual = calcularSueldo13(empleado, fechaCorrida);
        detalles.add(crearDetalle(nomina, TipoConcepto.SUELDO_13, "Sueldo 13 (Regalía Pascual)", promedioAnual, 1.0));
        BigDecimal limiteEmbargable = promedioAnual.multiply(configuracionNominaService.getPorcentajeLimiteEmbargo());
        ejecutarDeduccionesEspeciales(empleado, nomina, detalles, limiteEmbargable, TipoEmbargo.PENSION_ALIMENTICIA);
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

        BigDecimal salarioNeto = bonificacion.subtract(isr);
        BigDecimal limiteEmbargable = salarioNeto.multiply(configuracionNominaService.getPorcentajeLimiteEmbargo());

        ejecutarDeduccionesEspeciales(empleado, nomina, detalles, limiteEmbargable, TipoEmbargo.PENSION_ALIMENTICIA);
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

        BigDecimal totalGanado = detalleNominaRepository.sumarSalarioOrdinarioDelAnio(empleado, EstadoCorrida.APROBADA,
                inicioAnio, finAnio, conceptosOrdinarios);

        return totalGanado.divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP);
    }

    private void procesarVacacionesAnticipadas(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles) {
        List<VacacionEmpleado> vacacionesPendientes = vacacionEmpleadoService.findByEmpleadoYNoPagadas(empleado).stream()
                .filter(v -> v.getEstado() == EstadoVacacion.APROBADA)
                .collect(Collectors.toList());
        BigDecimal totalDevengado = BigDecimal.ZERO;

        for (VacacionEmpleado vacacion : vacacionesPendientes) {
            long diasAnticipacion = ChronoUnit.DAYS.between(LocalDate.now(), vacacion.getFechaInicio());

            if (diasAnticipacion >= 0 && diasAnticipacion <= 14) {
                BigDecimal salarioDiario = calcularSalarioDiario(empleado);
                BigDecimal montoVacacion = salarioDiario.multiply(BigDecimal.valueOf(vacacion.getCantidadDiasAPagar()));

                detalles.add(crearDetalle(nomina, TipoConcepto.PAGO_VACACIONES,
                        "Anticipo Vacaciones (" + vacacion.getCantidadDiasDescanso() + " días)", montoVacacion, 1.0));

                totalDevengado = totalDevengado.add(montoVacacion);
            }
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

            BigDecimal deduccionesLey = afp.add(sfs).add(isr);
            BigDecimal salarioNeto = totalDevengado.subtract(deduccionesLey);

            BigDecimal porcentajeLimite = configuracionNominaService.getPorcentajeLimiteEmbargo();
            BigDecimal limiteEmbargable = salarioNeto.multiply(porcentajeLimite);

            ejecutarDeduccionesPorPrioridad(empleado, nomina, detalles, limiteEmbargable);
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
        LocalDate fechaCierre = periodoFiscal.getFechaCierre();
        LocalDate fechaMinima = fechaCierre.plusDays(90);
        LocalDate fechaMaxima = fechaCierre.plusDays(120);

        if (fecha.isBefore(fechaMinima) || fecha.isAfter(fechaMaxima)) {
            throw new IllegalStateException("La bonificación debe pagarse entre el 90 y 120 días posterior al cierre fiscal.");
        }

        if (corridaRepository.existsByTipoAndPeriodoFiscal(TipoCorrida.BONIFICACION, periodoFiscal)) {
            throw new IllegalStateException("Ya se pagó la bonificación para este período fiscal.");
        }
    }

    public void validarDisponibilidadDePeriodo(PeriodoNomina periodoRequerido, LocalDate fecha, TipoCorrida tipo) {
        LocalDate hoy = LocalDate.now();

        if (fecha.getYear() > hoy.getYear() || (fecha.getYear() == hoy.getYear() && fecha.getMonthValue() > hoy.getMonthValue())) {
            throw new IllegalStateException("El sistema no permite generar nóminas para meses futuros.");
        }

        LocalDate limiteAntiguedad = hoy.minusMonths(3).withDayOfMonth(1);
        if (fecha.isBefore(limiteAntiguedad)) {
            throw new IllegalStateException("No se pueden generar nóminas con más de 3 meses de antigüedad por políticas de cierre contable.");
        }

        if (tipo != TipoCorrida.ORDINARIA) {
            return;
        }

        LocalDate inicioMes = fecha.withDayOfMonth(1);
        LocalDate finMes = fecha.withDayOfMonth(fecha.lengthOfMonth());
        LocalDate mitadMes = fecha.withDayOfMonth(15);

        boolean existeMensual = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(TipoCorrida.ORDINARIA, PeriodoNomina.MES, inicioMes, finMes);
        if (existeMensual) {
            throw new IllegalStateException("Ya existe una nómina mensual generada para este mes.");
        }

        if (periodoRequerido == PeriodoNomina.MES) {
            boolean existeQuincena = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(TipoCorrida.ORDINARIA, PeriodoNomina.QUINCENA, inicioMes, finMes);
            boolean existeSemanal = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(TipoCorrida.ORDINARIA, PeriodoNomina.SEMANAL, inicioMes, finMes);
            if (existeQuincena || existeSemanal) {
                throw new IllegalStateException("No puede generar una nómina mensual porque ya existen quincenas o semanas procesadas en este mes.");
            }

        } else if (periodoRequerido == PeriodoNomina.QUINCENA) {
            boolean esPrimeraQuincena = fecha.getDayOfMonth() <= 15;

            if (esPrimeraQuincena) {
                boolean existeQ1 = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(TipoCorrida.ORDINARIA, PeriodoNomina.QUINCENA, inicioMes, mitadMes);
                if (existeQ1) {
                    throw new IllegalStateException("La primera quincena de este mes ya fue generada.");
                }
            } else {
                boolean existeQ1 = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(TipoCorrida.ORDINARIA, PeriodoNomina.QUINCENA, inicioMes, mitadMes);
                if (!existeQ1) {
                    throw new IllegalStateException("No puede generar la segunda quincena sin haber procesado la primera. Genere una nómina Mensual.");
                }

                boolean existeQ2 = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(TipoCorrida.ORDINARIA, PeriodoNomina.QUINCENA, mitadMes.plusDays(1), finMes);
                if (existeQ2) {
                    throw new IllegalStateException("La segunda quincena de este mes ya fue generada.");
                }
            }

            LocalDate inicioQ = (fecha.getDayOfMonth() <= 15) ? inicioMes : mitadMes.plusDays(1);
            LocalDate finQ = (fecha.getDayOfMonth() <= 15) ? mitadMes : finMes;
            boolean existeSemanal = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(TipoCorrida.ORDINARIA, PeriodoNomina.SEMANAL, inicioQ, finQ);
            if (existeSemanal) {
                throw new IllegalStateException("Existen cortes semanales generados en esta quincena. Borre los semanales para procesar vía quincena.");
            }
        } else if (periodoRequerido == PeriodoNomina.SEMANAL) {
            LocalDate inicioQ = (fecha.getDayOfMonth() <= 15) ? inicioMes : mitadMes.plusDays(1);
            LocalDate finQ = (fecha.getDayOfMonth() <= 15) ? mitadMes : finMes;
            boolean existeQuincena = corridaRepository.existsByTipoAndPeriodoAndFechaEmisionBetween(
                    TipoCorrida.ORDINARIA, PeriodoNomina.QUINCENA, inicioQ, finQ);
            if(existeQuincena) {
                throw new IllegalStateException("No puede generar nómina semanal porque la quincena correspondiente ya fue procesada.");
            }

            Optional<CorridaNomina> ultimaCorridaOpt = corridaRepository.findTopByPeriodoAndEstadoAndTipoOrderByFechaEmisionDesc(
                    PeriodoNomina.SEMANAL, EstadoCorrida.APROBADA, TipoCorrida.ORDINARIA);

            boolean tratarComoArranque = false;

            if (ultimaCorridaOpt.isPresent()) {
                LocalDate fechaUltimaEmision = ultimaCorridaOpt.get().getFechaEmision();
                LocalDate fechaEsperada = fechaUltimaEmision.plusDays(7);

                if (!fecha.equals(fechaEsperada)) {
                    boolean huboCambioModalidad = corridaRepository.existsByTipoAndFechaEmisionBetween(
                            TipoCorrida.ORDINARIA, fechaUltimaEmision.plusDays(1), fecha.minusDays(1)
                    );

                    if (huboCambioModalidad) {
                        tratarComoArranque = true;
                    } else {
                        throw new IllegalStateException("La última nómina semanal se emitió el "
                                + fechaUltimaEmision + ". La siguiente corrida debe emitirse exactamente el "
                                + fechaEsperada + ".");
                    }
                }
            } else {
                tratarComoArranque = true;
            }

            if (tratarComoArranque) {
                if (fecha.getDayOfMonth() > 7) {
                    throw new IllegalStateException("Al iniciar (o reiniciar) la modalidad semanal, la primera corrida del mes debe emitirse dentro de los primeros 7 días (del 1 al 7).");
                }
            }
        }
    }

    private BigDecimal calcularCuotaEsperadaPorPeriodo(BigDecimal cuotaMensual, PeriodoNomina periodo) {
        if (cuotaMensual == null) return BigDecimal.ZERO;

        return switch (periodo) {
            case QUINCENA -> cuotaMensual.divide(FACTOR_QUINCENA, 2, RoundingMode.HALF_UP);
            case SEMANAL -> cuotaMensual.divide(FACTOR_SEMANAL, 2, RoundingMode.HALF_UP);
            default -> cuotaMensual;
        };
    }

    private void ejecutarDeduccionesEspeciales(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, BigDecimal limiteDisponible, TipoEmbargo tipoPermitido) {
        List<EmbargoSalarial> embargos = embargoSalarialService.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(empleado)
                .stream()
                .filter(e -> !e.getFechaNotificacion().isAfter(nomina.getCorrida().getFechaEmision()))
                .filter(e -> e.getTipoEmbargo() == tipoPermitido)
                .toList();

        int mesActual = nomina.getCorrida().getFechaEmision().getMonthValue();
        int anioActual = nomina.getCorrida().getFechaEmision().getYear();

        for (EmbargoSalarial embargo : embargos) {
            if (limiteDisponible.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal cuotaEsperada = embargo.getMontoCuotaOrdinaria() != null ? embargo.getMontoCuotaOrdinaria() : BigDecimal.ZERO;
            BigDecimal mora = embargo.getSaldoPendienteMora() != null ? embargo.getSaldoPendienteMora() : BigDecimal.ZERO;
            BigDecimal extras = calcularCuotasExtras(embargo, mesActual, anioActual);

            BigDecimal montoRequerido = cuotaEsperada.add(mora).add(extras);

            if (montoRequerido.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal montoACobrar = montoRequerido.min(limiteDisponible);

                String descripcionUnica = embargo.getTipoEmbargo().getDescripcion() + ": " + embargo.getEntidadDemandante() + " (Ref: #" + embargo.getIdEmbargo() + ")";
                detalles.add(crearDetalle(nomina, TipoConcepto.EMBARGO_SALARIAL,
                        descripcionUnica, montoACobrar, 1.0));

                limiteDisponible = limiteDisponible.subtract(montoACobrar);
            }
        }
    }

    private void validarIntegridadNovedades(NovedadNominaDTO novedad, PeriodoNomina periodo, int maxDiasAusencia, Empleado empleado) {
        String nombre = empleado.getPersona().getNombre();

        if (novedad.getHorasExtras() != null) {
            if (novedad.getHorasExtras() < 0) {
                throw new IllegalStateException("Las horas extras de " + nombre + " no pueden ser negativas.");
            }

            int maxHoras = switch(periodo) {
                case SEMANAL -> configuracionNominaService.getMaxHorasExtrasSemanal();
                case QUINCENA -> configuracionNominaService.getMaxHorasExtrasQuincenal();
                case MES -> configuracionNominaService.getMaxHorasExtrasMensual();
            };

            if (novedad.getHorasExtras() > maxHoras) {
                throw new IllegalStateException("Las horas extras de " + nombre + " (" + novedad.getHorasExtras() + ") superan el límite legal/lógico del período (" + maxHoras + " hrs).");
            }
        }

        if (novedad.getComisionesRegulares() != null && novedad.getComisionesRegulares().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Las comisiones de " + nombre + " no pueden ser negativas.");
        }
        if (novedad.getComisionesExtraordinarias() != null && novedad.getComisionesExtraordinarias().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Los bonos extra de " + nombre + " no pueden ser negativos.");
        }
        if (novedad.getDietasViaticos() != null && novedad.getDietasViaticos().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Los viáticos de " + nombre + " no pueden ser negativos.");
        }

        if (novedad.getAusenciasNoPagadasDias() != null) {
            if (novedad.getAusenciasNoPagadasDias() < 0) {
                throw new IllegalStateException("Las ausencias de " + nombre + " no pueden ser negativas.");
            }
            if (novedad.getAusenciasNoPagadasDias() > maxDiasAusencia) {
                throw new IllegalStateException("Las ausencias de " + nombre + " superan los días totales del período.");
            }
        }
    }
}
