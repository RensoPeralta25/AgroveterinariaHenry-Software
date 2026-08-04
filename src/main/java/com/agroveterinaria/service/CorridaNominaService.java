package com.agroveterinaria.service;

import com.agroveterinaria.dto.nomina.NovedadNominaDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.*;
import com.agroveterinaria.repository.*;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed({"ADMINISTRADOR", "RECURSOS_HUMANOS"})
public class CorridaNominaService {
    private static final BigDecimal FACTOR_QUINCENA = new BigDecimal("2");
    private static final BigDecimal FACTOR_SEMANAL = BigDecimal.valueOf(52)
            .divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

    private final CorridaNominaRepository corridaRepository;
    private final DetalleNominaRepository detalleNominaRepository;
    private final GastoOperativoRepository gastoOperativoRepository;
    private final VacacionEmpleadoService vacacionEmpleadoService;
    private final EmpleadoService empleadoService;
    private final PrestamoEmpleadoService prestamoEmpleadoService;
    private final EmbargoSalarialService embargoSalarialService;
    private final ConfiguracionNominaService configuracionNominaService;
    private final DiaFeriadoService diaFeriadoService;
    private final PeriodoFiscalService periodoFiscalService;
    private final AnticipoSalarioRepository anticipoSalarioRepository;
    private final AusenciaService ausenciaService;
    private final HistorialDevengadoAnualService historialDevengadoAnualService;
    private final VentaRepository ventaRepository;
    private final DevolucionVentaRepository devolucionVentaRepository;
    private final CompraRepository compraRepository;

    public List<CorridaNomina> findAllConNominas() {
        return corridaRepository.findAllConNominas();
    }

    public CorridaNomina generarCorrida(PeriodoNomina periodo, LocalDate fechaInicio, LocalDate fechaFin,
                                        LocalDate fechaEmision, TipoCorrida tipo, PeriodoFiscal periodoFiscal,
                                        Empleado empleadoEspecifico, List<NovedadNominaDTO> novedades) {
        validarDisponibilidadDePeriodo(periodo, fechaInicio, fechaFin, fechaEmision, tipo);

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

        if (tipo == TipoCorrida.REGALIA_PASCUAL) {
            validarRegaliaPascual(fechaEmision);
        } else if (tipo == TipoCorrida.BONIFICACION) {
            if (periodoFiscal == null) {
                throw new IllegalStateException("Para generar bonificaciones, debe seleccionar un Período Fiscal.");
            }
            validarBonificacion(fechaEmision, periodoFiscal);
        }

        CorridaNomina corrida = new CorridaNomina(periodo,fechaInicio, fechaFin, fechaEmision);
        corrida.setTipo(tipo);

        if (periodoFiscal != null) {
            corrida.setPeriodoFiscal(periodoFiscal);
        } else {
            periodoFiscalService.buscarPorFecha(fechaEmision)
                    .ifPresent(periodoActivo -> corrida.setPeriodoFiscal(periodoActivo));
        }

        BigDecimal fondoBonificacion = BigDecimal.ZERO;
        BigDecimal sumaIdealBonificaciones = BigDecimal.ZERO;
        Map<Long, BigDecimal> bonificacionesIdeales = new HashMap<>();
        List<Empleado> empleados = (empleadoEspecifico != null)
                ? List.of(empleadoEspecifico)
                : empleadoService.findByStatus(StatusEntidad.ACTIVO);

        if (tipo == TipoCorrida.REGALIA_PASCUAL) {
            validarRegaliaPascual(fechaEmision);
        } else if (tipo == TipoCorrida.BONIFICACION) {
            if (corrida.getPeriodoFiscal() == null) {
                throw new IllegalStateException("Para generar bonificaciones, debe seleccionar un Período Fiscal.");
            }
            validarBonificacion(fechaEmision, corrida.getPeriodoFiscal());

            LocalDateTime inicioM = corrida.getPeriodoFiscal().getFechaInicio().atStartOfDay();
            LocalDateTime finM = corrida.getPeriodoFiscal().getFechaCierre().atTime(23, 59, 59);

            BigDecimal ventas = ventaRepository.sumarMontoEntre(inicioM, finM, List.of(EstadoVenta.PENDIENTE, EstadoVenta.CERRADA));
            BigDecimal devoluciones = devolucionVentaRepository.sumarMontoEntre(inicioM, finM, EstadoDevolucion.COMPLETADA);
            BigDecimal compras = compraRepository.sumarTotalEntre(inicioM, finM, EstadoRecepcion.BORRADOR);

            List<TipoGasto> gastosExcluidos = List.of(TipoGasto.PRESTAMO_EMPLEADO, TipoGasto.ANTICIPO_SALARIO, TipoGasto.NOMINA);
            BigDecimal gastosAdmin = gastoOperativoRepository.sumarMontoRealesEntre(
                    corrida.getPeriodoFiscal().getFechaInicio(),
                    corrida.getPeriodoFiscal().getFechaCierre(),
                    gastosExcluidos
            );

            List<TipoConcepto> conceptosDeIngreso = Arrays.stream(TipoConcepto.values())
                    .filter(TipoConcepto::esIngreso)
                    .collect(Collectors.toList());

            BigDecimal nominaBrutaTotal = detalleNominaRepository.sumarNominaBrutaTotalPorPeriodo(
                    corrida.getPeriodoFiscal(),
                    conceptosDeIngreso
            );

            BigDecimal utilidadNeta = nvl(ventas)
                    .subtract(nvl(devoluciones))
                    .subtract(nvl(compras))
                    .subtract(nvl(gastosAdmin))
                    .subtract(nvl(nominaBrutaTotal));

            if (utilidadNeta.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("La empresa no generó utilidades netas en el período fiscal (Utilidad: RD$ " + utilidadNeta + "). Legalmente no procede pago de bonificación.");
            }

            BigDecimal porcentajeReparticion = configuracionNominaService.getPorcentajeUtilidadesBonificacion();
            fondoBonificacion = utilidadNeta.multiply(porcentajeReparticion);

            for (Empleado emp : empleados) {
                BigDecimal ideal = calcularMontoBonificacion(emp, corrida.getPeriodoFiscal());
                bonificacionesIdeales.put(emp.getIdEmpleado(), ideal);
                sumaIdealBonificaciones = sumaIdealBonificaciones.add(ideal);
            }
        }

        Set<Nomina> nominas = new LinkedHashSet<>();

        Map<Long, NovedadNominaDTO> mapaNovedades = (novedades != null)
                ? novedades.stream().collect(Collectors.toMap(n -> n.getEmpleado().getIdEmpleado(), n -> n))
                : Collections.emptyMap();

        for (Empleado empleado : empleados) {
            Nomina nomina = new Nomina(empleado, corrida);
            Set<DetalleNomina> detalles = new LinkedHashSet<>();

            NovedadNominaDTO novedadDelEmpleado = mapaNovedades.get(empleado.getIdEmpleado());

            if (novedadDelEmpleado != null && corrida.getTipo() == TipoCorrida.ORDINARIA) {
                int maxDiasPermitidos = (int) ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
                validarIntegridadNovedades(novedadDelEmpleado, periodo, maxDiasPermitidos, empleado);
            }

            switch (corrida.getTipo()) {
                case ORDINARIA:
                    procesarNominaOrdinaria(empleado, nomina, detalles, periodo, fechaInicio, fechaFin, novedadDelEmpleado);
                    break;
                case REGALIA_PASCUAL:
                    procesarRegaliaPascual(empleado, nomina, detalles, corrida.getFechaEmision());
                    break;
                case BONIFICACION:
                    BigDecimal ideal = bonificacionesIdeales.getOrDefault(empleado.getIdEmpleado(), BigDecimal.ZERO);
                    BigDecimal bonificacionFinal = ideal;

                    if (sumaIdealBonificaciones.compareTo(fondoBonificacion) > 0 && sumaIdealBonificaciones.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal proporcion = ideal.divide(sumaIdealBonificaciones, 6, RoundingMode.HALF_UP);
                        bonificacionFinal = fondoBonificacion.multiply(proporcion).setScale(2, RoundingMode.HALF_UP);
                    }

                    procesarBonificacion(empleado, nomina, detalles, bonificacionFinal);
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

    private BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public CorridaNomina aprobarCorrida(CorridaNomina corrida) {
        validarEstadoPendiente(corrida);
        corrida.setEstado(EstadoCorrida.APROBADA);
        corridaRepository.saveAndFlush(corrida);

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
                    cuotaOrdinaria = calcularCuotaEsperadaPorPeriodo(embargo.getMontoCuotaOrdinaria(), corrida.getPeriodo(), corrida.getFechaInicio(), corrida.getFechaFin());
                }

                BigDecimal moraAcumulada = embargo.getSaldoPendienteMora() != null ? embargo.getSaldoPendienteMora() : BigDecimal.ZERO;
                BigDecimal cuotasExtras = calcularCuotasExtras(embargo, mesActual, anioActual);
                BigDecimal montoRequerido = cuotaOrdinaria.add(moraAcumulada).add(cuotasExtras);

                String descripcionEsperada = embargo.getTipoEmbargo().getDescripcion() + ": " + embargo.getEntidadDemandante() + " (Ref: #" + embargo.getIdEmbargo() + ")";
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

            if (corrida.getTipo() == TipoCorrida.ORDINARIA) {
                LocalDate inicioPeriodo = corrida.getFechaInicio();
                LocalDate finPeriodo = corrida.getFechaFin();

                List<Ausencia> ausenciasPendientes = ausenciaService.obtenerAusenciasPendientes(nomina.getEmpleado().getIdEmpleado());

                BigDecimal ingresosComputables = nomina.getDetalles().stream()
                        .filter(d -> d.getTipo() == TipoConcepto.SALARIO_BASE ||
                                d.getTipo() == TipoConcepto.COMISIONES_REGULARES ||
                                d.getTipo() == TipoConcepto.PAGO_VACACIONES)
                        .map(DetalleNomina::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal deduccionesPorAusencia = nomina.getDetalles().stream()
                        .filter(d -> d.getTipo() == TipoConcepto.DESCUENTO_AUSENCIA)
                        .map(DetalleNomina::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalComputableParaRegalia = ingresosComputables.subtract(deduccionesPorAusencia);

                if (totalComputableParaRegalia.compareTo(BigDecimal.ZERO) < 0) {
                    totalComputableParaRegalia = BigDecimal.ZERO;
                }

                BigDecimal divisorOficial = configuracionNominaService.getDivisorMensualDiario();
                BigDecimal salarioBaseDiario = nomina.getEmpleado().getSalario().divide(divisorOficial, 2, RoundingMode.HALF_UP);
                BigDecimal compensacionVirtual = BigDecimal.ZERO;

                for (Ausencia ausencia : ausenciasPendientes) {
                    if (!ausencia.getTipoAusencia().isGeneraPagoEmpleador()) {
                        int diasEnEstePeriodo = (int) ausenciaService.calcularDiasAusenciaEnRango(ausencia, inicioPeriodo, finPeriodo);
                        ausencia.setDiasADescontarEnEstaCorrida(Math.max(diasEnEstePeriodo, 0));
                    }

                    if (!ausencia.getTipoAusencia().isGeneraPagoEmpleador() && !ausencia.getTipoAusencia().isReduceTiempoEfectivo()) {
                        long diasProtegidos = ausenciaService.calcularDiasAusenciaEnRango(ausencia, inicioPeriodo, finPeriodo);
                        if (diasProtegidos > 0) {
                            BigDecimal salarioSimulado = salarioBaseDiario.multiply(BigDecimal.valueOf(diasProtegidos));
                            compensacionVirtual = compensacionVirtual.add(salarioSimulado);
                        }
                    }
                }

                totalComputableParaRegalia = totalComputableParaRegalia.add(compensacionVirtual);

                ausenciaService.marcarAusenciasComoAplicadas(ausenciasPendientes, nomina);

                historialDevengadoAnualService.registrarOActualizarDevengado(
                        nomina.getEmpleado(),
                        anioActual,
                        mesActual,
                        totalComputableParaRegalia
                );
            }
        }

        BigDecimal totalNeto = corrida.getTotalGeneral();

        if (totalNeto.compareTo(BigDecimal.ZERO) > 0) {
            GastoOperativo gastoNomina = new GastoOperativo();
            gastoNomina.setTipoGasto(TipoGasto.NOMINA);
            gastoNomina.setFecha(corrida.getFechaEmision());
            gastoNomina.setMonto(totalNeto);

            gastoNomina.setNotas(corrida.getTipo().getDescripcion() +
                    ": " + corrida.getFechaInicio() + " - " + corrida.getFechaFin());

            gastoNomina = gastoOperativoRepository.save(gastoNomina);
            corrida.setGastoAsociado(gastoNomina);
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

    private void procesarNominaOrdinaria(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, PeriodoNomina periodo, LocalDate inicioPeriodo, LocalDate finPeriodo, NovedadNominaDTO novedad) {
        BigDecimal salarioBaseDiario = empleado.getSalario().divide(configuracionNominaService.getDivisorMensualDiario(), 2, RoundingMode.HALF_UP);
        BigDecimal salarioDelPeriodo = empleado.getSalario();

        if (periodo == PeriodoNomina.QUINCENA) {
            salarioDelPeriodo = salarioDelPeriodo.divide(FACTOR_QUINCENA, 2, RoundingMode.HALF_UP);

            if (inicioPeriodo.getDayOfMonth() > 1 && inicioPeriodo.getDayOfMonth() <= 15) {
                long diasFaltantes = inicioPeriodo.getDayOfMonth() - 1;
                salarioDelPeriodo = salarioDelPeriodo.subtract(salarioBaseDiario.multiply(BigDecimal.valueOf(diasFaltantes)));
            }

        } else if (periodo == PeriodoNomina.SEMANAL) {
            salarioDelPeriodo = salarioDelPeriodo.divide(FACTOR_SEMANAL, 2, RoundingMode.HALF_UP);

            if (inicioPeriodo.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
                long diasFaltantes = inicioPeriodo.getDayOfWeek().getValue() - 1;
                salarioDelPeriodo = salarioDelPeriodo.subtract(salarioBaseDiario.multiply(BigDecimal.valueOf(diasFaltantes)));
            }

        } else if (periodo == PeriodoNomina.MES) {
            if (inicioPeriodo.getDayOfMonth() > 1) {
                long diasFaltantes = inicioPeriodo.getDayOfMonth() - 1;
                salarioDelPeriodo = salarioDelPeriodo.subtract(salarioBaseDiario.multiply(BigDecimal.valueOf(diasFaltantes)));
            }
        }

        BigDecimal totalDevengado = BigDecimal.ZERO;

        List<VacacionEmpleado> vacaciones = vacacionEmpleadoService.encontrarVacacionesEnPeriodo(empleado, inicioPeriodo, finPeriodo);
        if (!vacaciones.isEmpty()) {
            BigDecimal salarioDiarioComputable = calcularSalarioDiario(empleado);
            List<LocalDate> feriadosDelPeriodo = diaFeriadoService.obtenerFechasFeriadasEnRango(inicioPeriodo, finPeriodo);
            long totalDiasFisicosVacaciones = 0;

            for (VacacionEmpleado vacacion : vacaciones) {
                LocalDate inicioReal = vacacion.getFechaInicio().isAfter(inicioPeriodo) ? vacacion.getFechaInicio() : inicioPeriodo;
                LocalDate finReal = vacacion.getFechaFin().isBefore(finPeriodo) ? vacacion.getFechaFin() : finPeriodo;

                LocalDate diaIterador = inicioReal;
                while (!diaIterador.isAfter(finReal)) {
                    boolean esDomingo = diaIterador.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
                    boolean esFeriado = feriadosDelPeriodo.contains(diaIterador);

                    if (!esDomingo && !esFeriado) {
                        totalDiasFisicosVacaciones++;
                    }
                    diaIterador = diaIterador.plusDays(1);
                }

                boolean inicianEnEstePeriodo = !vacacion.getFechaInicio().isBefore(inicioPeriodo) && !vacacion.getFechaInicio().isAfter(finPeriodo);
                if (vacacion.getEstado() == EstadoVacacion.APROBADA && inicianEnEstePeriodo) {
                    BigDecimal montoVacacion = salarioDiarioComputable.multiply(BigDecimal.valueOf(vacacion.getCantidadDiasAPagar()));
                    detalles.add(crearDetalle(nomina, TipoConcepto.PAGO_VACACIONES,
                            "Vacaciones Ordinarias (" + vacacion.getCantidadDiasAPagar() + " días pagados)", montoVacacion, 1.0));
                    totalDevengado = totalDevengado.add(montoVacacion);
                }
            }

            if (totalDiasFisicosVacaciones > 0) {
                BigDecimal deduccionVacaciones = salarioBaseDiario.multiply(BigDecimal.valueOf(totalDiasFisicosVacaciones));
                salarioDelPeriodo = salarioDelPeriodo.subtract(deduccionVacaciones);
            }
        }

        if (salarioDelPeriodo.compareTo(BigDecimal.ZERO) > 0) {
            detalles.add(crearDetalle(nomina, TipoConcepto.SALARIO_BASE, "Salario base ordinario", salarioDelPeriodo, 1.0));
            totalDevengado = totalDevengado.add(salarioDelPeriodo);
        }

        List<Ausencia> ausenciasPendientes = ausenciaService.obtenerAusenciasPendientes(empleado.getIdEmpleado());
        long totalDiasAusenciaADescontar = 0;

        for (Ausencia ausencia : ausenciasPendientes) {
            if (!ausencia.getTipoAusencia().isGeneraPagoEmpleador()) {

                int diasEnEstePeriodo = (int) ausenciaService.calcularDiasAusenciaEnRango(ausencia, inicioPeriodo, finPeriodo);

                if (diasEnEstePeriodo > 0) {
                    totalDiasAusenciaADescontar += diasEnEstePeriodo;
                    ausencia.setDiasADescontarEnEstaCorrida(diasEnEstePeriodo);
                } else {
                    ausencia.setDiasADescontarEnEstaCorrida(0);
                }
            }
        }

        if (totalDiasAusenciaADescontar > 0) {
            BigDecimal deduccionAusencias = salarioBaseDiario.multiply(BigDecimal.valueOf(totalDiasAusenciaADescontar));

            if (deduccionAusencias.compareTo(salarioDelPeriodo) > 0) {
                deduccionAusencias = salarioDelPeriodo;
            }

            detalles.add(crearDetalle(nomina, TipoConcepto.DESCUENTO_AUSENCIA, "Descuento por Ausencias (" + totalDiasAusenciaADescontar + " días)", deduccionAusencias, (double) totalDiasAusenciaADescontar));
            totalDevengado = totalDevengado.subtract(deduccionAusencias);
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

            if (novedad.getReembolsoLicencias() != null && novedad.getReembolsoLicencias().compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.REEMBOLSO_LICENCIA, "Reembolso por Licencia Tardía", novedad.getReembolsoLicencias(), 1.0));
                totalDevengado = totalDevengado.add(novedad.getReembolsoLicencias());
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

        LocalDate fechaInicio = nomina.getCorrida().getFechaInicio();
        LocalDate fechaFin = nomina.getCorrida().getFechaFin();

        Map<Integer, List<EmbargoSalarial>> embargosPorPrioridad = embargosActivos.stream()
                .collect(Collectors.groupingBy(e -> e.getTipoEmbargo().getPrioridad()));

        List<Integer> nivelesPrioridad = embargosPorPrioridad.keySet().stream().sorted().toList();

        for (Integer prioridad : nivelesPrioridad) {
            if (limiteDisponible.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            List<EmbargoSalarial> embargosNivel = embargosPorPrioridad.get(prioridad);

            BigDecimal totalRequeridoNivel = embargosNivel.stream()
                    .map(e -> calcularMontoRequeridoEmbargo(e, mesActual, anioActual, periodoActual, fechaInicio, fechaFin))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalRequeridoNivel.compareTo(BigDecimal.ZERO) == 0) continue;

            if (totalRequeridoNivel.compareTo(limiteDisponible) <= 0) {
                for (EmbargoSalarial embargo : embargosNivel) {
                    BigDecimal montoACobrar = calcularMontoRequeridoEmbargo(embargo, mesActual,  anioActual, periodoActual, fechaInicio, fechaFin);
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
                    BigDecimal deudaEmbargo = calcularMontoRequeridoEmbargo(embargo, mesActual, anioActual, periodoActual, fechaInicio, fechaFin);

                    if (deudaEmbargo.compareTo(BigDecimal.ZERO) == 0) continue;

                    BigDecimal proporcion = deudaEmbargo.divide(totalRequeridoNivel, 6, RoundingMode.HALF_UP);
                    BigDecimal montoAsignado = fondoOriginalNivel.multiply(proporcion).setScale(2, RoundingMode.HALF_UP);

                    if (i == embargosNivel.size() - 1 || montoAsignado.compareTo(fondoRestante) > 0) {
                        montoAsignado = fondoRestante.min(deudaEmbargo);
                    }

                    if (montoAsignado.compareTo(BigDecimal.ZERO) > 0) {
                        String descripcionUnica = embargo.getTipoEmbargo().getDescripcion() + ": " + embargo.getEntidadDemandante() + " (Ref: #" + embargo.getIdEmbargo() + ")";
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

    private BigDecimal calcularMontoRequeridoEmbargo(EmbargoSalarial embargo, int mesActual, int anioActual, PeriodoNomina periodo, LocalDate fechaInicio, LocalDate fechaFin) {
        BigDecimal cuotaMensual = embargo.getMontoCuotaOrdinaria() != null ? embargo.getMontoCuotaOrdinaria() : BigDecimal.ZERO;
        BigDecimal cuotaOrdinaria = calcularCuotaEsperadaPorPeriodo(cuotaMensual, periodo, fechaInicio, fechaFin);

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

            BigDecimal cuotaEsperadaPeriodo = calcularCuotaEsperadaPorPeriodo(
                    prestamo.getCuotaPeriodica(),
                    nomina.getCorrida().getPeriodo(),
                    nomina.getCorrida().getFechaInicio(),
                    nomina.getCorrida().getFechaFin()
            );
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

    private void procesarBonificacion(Empleado empleado, Nomina nomina, Set<DetalleNomina> detalles, BigDecimal montoBonificacion) {
        if (montoBonificacion.compareTo(BigDecimal.ZERO) <= 0) return;

        detalles.add(crearDetalle(nomina, TipoConcepto.BONIFICACIONES, "Bonificación Anual", montoBonificacion, 1.0));

        BigDecimal porcentajeInfotep = configuracionNominaService.getPorcentajeInfotepBonificacion();
        BigDecimal retencionInfotep = montoBonificacion.multiply(porcentajeInfotep).setScale(2, RoundingMode.HALF_UP);

        if (retencionInfotep.compareTo(BigDecimal.ZERO) > 0) {
            detalles.add(crearDetalle(nomina, TipoConcepto.INFOTEP, "Retención INFOTEP (0.5%)", retencionInfotep, 1.0));
        }

        BigDecimal baseIsr = montoBonificacion.subtract(retencionInfotep);

        BigDecimal isr = configuracionNominaService.calcularISR(baseIsr, PeriodoNomina.MES);
        if (isr.compareTo(BigDecimal.ZERO) > 0) {
            detalles.add(crearDetalle(nomina, TipoConcepto.IMPUESTO_RENTA, "ISR Bonificación", isr, 1.0));
        }

        BigDecimal salarioNeto = baseIsr.subtract(isr);
        BigDecimal limiteEmbargable = salarioNeto.multiply(configuracionNominaService.getPorcentajeLimiteEmbargo());

        ejecutarDeduccionesEspeciales(empleado, nomina, detalles, limiteEmbargable, TipoEmbargo.PENSION_ALIMENTICIA);
    }

    private BigDecimal calcularMontoBonificacion(Empleado empleado, PeriodoFiscal periodoFiscal) {
        LocalDate fechaIngreso = empleado.getFechaIngreso();
        LocalDate fechaCierreFiscal = periodoFiscal.getFechaCierre();

        if (fechaIngreso.isAfter(fechaCierreFiscal)) {
            return BigDecimal.ZERO;
        }

        int anioFiscal = periodoFiscal.getAnio();
        BigDecimal totalDevengado = historialDevengadoAnualService.sumarDevengadoAnualPorEmpleado(empleado.getIdEmpleado(), anioFiscal);

        if (totalDevengado == null || totalDevengado.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal salarioMensualPromedio = totalDevengado.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        BigDecimal divisorDiario = configuracionNominaService.getDivisorMensualDiario();
        BigDecimal salarioDiarioComputable = salarioMensualPromedio.divide(divisorDiario, 2, RoundingMode.HALF_UP);

        int aniosAntiguedad = Period.between(fechaIngreso, fechaCierreFiscal).getYears();
        BigDecimal diasTope = (aniosAntiguedad >= configuracionNominaService.getAniosBonificacionSenior())
                ? configuracionNominaService.getDiasBonificacionTope()
                : configuracionNominaService.getDiasBonificacionBase();

        return salarioDiarioComputable.multiply(diasTope).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularSueldo13(Empleado empleado, LocalDate fechaCorrida) {
        int anio = fechaCorrida.getYear();

        BigDecimal totalGanado = historialDevengadoAnualService.sumarDevengadoAnualPorEmpleado(empleado.getIdEmpleado(), anio);

        if (totalGanado == null) {
            return BigDecimal.ZERO;
        }

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

    public void validarDisponibilidadDePeriodo(PeriodoNomina periodoRequerido, LocalDate fechaInicio, LocalDate fechaFin, LocalDate fechaEmision, TipoCorrida tipo) {
        if (tipo != TipoCorrida.ORDINARIA) return;

        LocalDate hoy = LocalDate.now();

        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("La nómina ordinaria requiere fecha de inicio y fin.");
        }

        if (fechaFin.isAfter(hoy)) {
            throw new IllegalStateException("No se permite generar nóminas que incluyan fechas futuras (" + fechaFin + "), ya que esos días aún no se han trabajado.");
        }

        if (fechaEmision.isAfter(hoy)) {
            throw new IllegalStateException("La fecha de emisión de la nómina (" + fechaEmision + ") no puede ser mayor al día de hoy.");
        }

        if (fechaInicio.isBefore(hoy.minusMonths(3).withDayOfMonth(1))) {
            throw new IllegalStateException("No se pueden generar nóminas con más de 3 meses de antigüedad por políticas de cierre contable.");
        }

        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalStateException("La fecha de inicio no puede ser mayor a la fecha de fin.");
        }

        boolean existeSolapamiento = corridaRepository.existeSolapamiento(fechaInicio, fechaFin, TipoCorrida.ORDINARIA);
        if (existeSolapamiento) {
            throw new IllegalStateException("Ya existe una nómina procesada que choca con los días comprendidos entre el "
                    + fechaInicio + " y el " + fechaFin + ".");
        }

        Optional<CorridaNomina> ultimaNominaOpt = corridaRepository.findTopByTipoAndEstadoOrderByFechaFinDesc(TipoCorrida.ORDINARIA, EstadoCorrida.APROBADA);

        if (ultimaNominaOpt.isPresent()) {
            CorridaNomina ultima = ultimaNominaOpt.get();
            LocalDate inicioEsperado = ultima.getFechaFin().plusDays(1);

            if (periodoRequerido == PeriodoNomina.SEMANAL && inicioEsperado.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                inicioEsperado = inicioEsperado.plusDays(1);
            }

            if (!fechaInicio.isEqual(inicioEsperado)) {
                throw new IllegalStateException("Secuencia rota. La última nómina cerró el " + ultima.getFechaFin() +
                        ". Para no dejar días huérfanos, esta nómina DEBE iniciar exactamente el " + inicioEsperado + ".");
            }

            boolean cerroMesCompleto = ultima.getFechaFin().equals(ultima.getFechaFin().withDayOfMonth(ultima.getFechaFin().lengthOfMonth()));

            if (!cerroMesCompleto && periodoRequerido != ultima.getPeriodo()) {
                throw new IllegalStateException("No puede cambiar la modalidad de pago a mitad de mes. " +
                        "Debe generar una nómina " + ultima.getPeriodo().name() + " para cerrar el mes actual antes de cambiar a " + periodoRequerido.name() + ".");
            }
        } else {
            if (periodoRequerido == PeriodoNomina.SEMANAL && fechaInicio.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
                throw new IllegalStateException("La primera nómina semanal del sistema debe iniciar un lunes.");
            }
            if (periodoRequerido == PeriodoNomina.QUINCENA && fechaInicio.getDayOfMonth() != 1 && fechaInicio.getDayOfMonth() != 16) {
                throw new IllegalStateException("La primera quincena del sistema debe iniciar un día 1 o 16.");
            }
        }

        if (periodoRequerido == PeriodoNomina.QUINCENA && fechaInicio.getDayOfMonth() > 15) {
            boolean existeQ1 = corridaRepository.existePrimeraQuincenaEnMes(TipoCorrida.ORDINARIA, PeriodoNomina.QUINCENA, fechaInicio.getMonthValue(), fechaInicio.getYear());
            if (!existeQ1) {
                throw new IllegalStateException("No puede generar la segunda quincena sin haber procesado la primera de este mes.");
            }
        }
    }

    private BigDecimal calcularCuotaEsperadaPorPeriodo(BigDecimal cuotaMensual, PeriodoNomina periodo, LocalDate fechaInicio, LocalDate fechaFin) {
        if (cuotaMensual == null) return BigDecimal.ZERO;

        BigDecimal cuotaBase = switch (periodo) {
            case QUINCENA -> cuotaMensual.divide(FACTOR_QUINCENA, 2, RoundingMode.HALF_UP);
            case SEMANAL -> cuotaMensual.divide(FACTOR_SEMANAL, 2, RoundingMode.HALF_UP);
            default -> cuotaMensual;
        };

        if (periodo == PeriodoNomina.SEMANAL && fechaInicio != null && fechaFin != null) {
            long diasPeriodo = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;

            if (diasPeriodo < 6) {
                BigDecimal factorProrrateo = BigDecimal.valueOf(diasPeriodo)
                        .divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP);

                cuotaBase = cuotaBase.multiply(factorProrrateo).setScale(2, RoundingMode.HALF_UP);
            }
        }

        return cuotaBase;
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

        if (novedad.getReembolsoLicencias() != null && novedad.getReembolsoLicencias().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El reembolso de " + nombre + " no puede ser negativo.");
        }
    }

    public LocalDate obtenerProximaFechaInicioOrdinaria(PeriodoNomina periodoRequerido) {
        return corridaRepository.findTopByTipoAndEstadoOrderByFechaFinDesc(TipoCorrida.ORDINARIA, EstadoCorrida.APROBADA)
                .map(c -> {
                    LocalDate proximo = c.getFechaFin().plusDays(1);
                    if (periodoRequerido == PeriodoNomina.SEMANAL && proximo.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                        return proximo.plusDays(1);
                    }
                    return proximo;
                })
                .orElse(null);
    }
}
