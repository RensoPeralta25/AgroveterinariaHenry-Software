package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.*;
import com.agroveterinaria.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LiquidacionService {

    private final LiquidacionEmpleadoRepository liquidacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final HistorialDevengadoAnualService historialDevengadoService;
    private final PrestamoEmpleadoRepository prestamoRepository;
    private final AnticipoSalarioRepository anticipoRepository;
    private final EmbargoSalarialService embargoService;
    private final ConfiguracionNominaService configuracionService;
    private final CorridaNominaRepository corridaRepository;
    private final GastoOperativoRepository gastoOperativoRepository;
    private final VacacionEmpleadoService vacacionEmpleadoService;

    @Transactional
    public LiquidacionEmpleado generarLiquidacion(Long idEmpleado, MotivoSalida motivo, LocalDate fechaSalida, Integer diasPreavisoTrabajados) {
        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        validarCondicionesParaLiquidar(empleado);

        LiquidacionEmpleado liquidacion = new LiquidacionEmpleado();
        liquidacion.setEmpleado(empleado);
        liquidacion.setFechaLiquidacion(fechaSalida);
        liquidacion.setMotivoSalida(motivo);
        liquidacion.setDiasPreavisoTrabajados(diasPreavisoTrabajados != null ? diasPreavisoTrabajados : 0);

        BigDecimal salarioMensual = empleado.getSalario();
        BigDecimal salarioDiario = salarioMensual.divide(configuracionService.getDivisorMensualDiario(), 8, RoundingMode.HALF_UP);

        BigDecimal devengadoAnio = historialDevengadoService.sumarDevengadoAnualPorEmpleado(idEmpleado, fechaSalida.getYear());
        BigDecimal regalia = devengadoAnio.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        liquidacion.setMontoRegalia(regalia);

        BigDecimal vacaciones = calcularMontoVacaciones(empleado, salarioDiario, fechaSalida);
        liquidacion.setMontoVacaciones(vacaciones);

        int diasLegales = calcularDiasPreavisoLegales(empleado, fechaSalida);
        int diasTrabajados = liquidacion.getDiasPreavisoTrabajados();

        if (diasTrabajados < 0) {
            throw new IllegalArgumentException("Error de integridad: Los días de preaviso trabajados no pueden ser negativos.");
        }
        if (diasTrabajados > diasLegales) {
            throw new IllegalArgumentException("Error de integridad: Los días trabajados (" + diasTrabajados + ") superan el tope legal (" + diasLegales + " días) para la antigüedad de este empleado.");
        }

        int diasOmitidos = diasLegales - diasTrabajados;

        BigDecimal valorPreavisoLegal = salarioDiario.multiply(new BigDecimal(diasOmitidos)).setScale(2, RoundingMode.HALF_UP);

        if (motivo == MotivoSalida.DESAHUCIO) {
            liquidacion.setMontoCesantia(calcularMontoCesantia(empleado, salarioDiario, fechaSalida));
            liquidacion.setMontoPreaviso(valorPreavisoLegal);
        } else if (motivo == MotivoSalida.RENUNCIA) {
            liquidacion.setMontoCesantia(BigDecimal.ZERO);
            liquidacion.setMontoPreaviso(valorPreavisoLegal);
        } else {
            liquidacion.setMontoPreaviso(BigDecimal.ZERO);
            liquidacion.setMontoCesantia(BigDecimal.ZERO);
        }

        BigDecimal totalIngresos = regalia.add(vacaciones).add(liquidacion.getMontoCesantia());
        if (motivo == MotivoSalida.DESAHUCIO) {
            totalIngresos = totalIngresos.add(liquidacion.getMontoPreaviso());
        }
        liquidacion.setTotalIngresos(totalIngresos);

        BigDecimal baseEmbargable = regalia.add(vacaciones);
        BigDecimal topeEmbargo = baseEmbargable.multiply(configuracionService.getPorcentajeLimiteEmbargo()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal descuentoEmbargo = calcularDescuentoEmbargo(empleado, topeEmbargo, fechaSalida);
        liquidacion.setDescuentoEmbargos(descuentoEmbargo);

        BigDecimal disponibleVacaciones = vacaciones.subtract(descuentoEmbargo).max(BigDecimal.ZERO);

        BigDecimal deudaAnticipos = sumarDeudaAnticipos(idEmpleado);
        BigDecimal descuentoAnticipo = deudaAnticipos.min(disponibleVacaciones);
        liquidacion.setDescuentoAnticipos(descuentoAnticipo);

        disponibleVacaciones = disponibleVacaciones.subtract(descuentoAnticipo);

        BigDecimal deudaPrestamos = sumarDeudaPrestamos(idEmpleado);
        BigDecimal descuentoPrestamo = deudaPrestamos.min(disponibleVacaciones);
        liquidacion.setDescuentoPrestamos(descuentoPrestamo);

        BigDecimal totalDeducciones = descuentoEmbargo.add(descuentoAnticipo).add(descuentoPrestamo);
        if (motivo == MotivoSalida.RENUNCIA) {
            totalDeducciones = totalDeducciones.add(liquidacion.getMontoPreaviso());
        }
        liquidacion.setTotalDeducciones(totalDeducciones);

        BigDecimal netoCalculado = totalIngresos.subtract(totalDeducciones);
        liquidacion.setMontoNeto(netoCalculado.max(BigDecimal.ZERO));
        liquidacion.setObservaciones("Liquidación generada automáticamente por " + motivo.name());

        cerrarDeudasYEmbargos(empleado, descuentoAnticipo, descuentoPrestamo);

        String nombreEmpleado = empleado.getPersona().getNombre() + " " + empleado.getPersona().getApellido();

        if (liquidacion.getMontoNeto().compareTo(BigDecimal.ZERO) > 0) {
            GastoOperativo gastoLiquidacion = new GastoOperativo();
            gastoLiquidacion.setTipoGasto(TipoGasto.LIQUIDACION);
            gastoLiquidacion.setFecha(fechaSalida);
            gastoLiquidacion.setMonto(liquidacion.getMontoNeto());
            gastoLiquidacion.setNotas("Pago neto liquidación (" + motivo.name() + ") - " + nombreEmpleado);
            gastoLiquidacion = gastoOperativoRepository.save(gastoLiquidacion);
            liquidacion.setGastoNetoAsociado(gastoLiquidacion);
        }

        if (liquidacion.getDescuentoEmbargos() != null && liquidacion.getDescuentoEmbargos().compareTo(BigDecimal.ZERO) > 0) {
            GastoOperativo gastoEmbargo = new GastoOperativo();
            gastoEmbargo.setTipoGasto(TipoGasto.RETENCION_LEGAL);
            gastoEmbargo.setFecha(fechaSalida);
            gastoEmbargo.setMonto(liquidacion.getDescuentoEmbargos());
            gastoEmbargo.setNotas("Retención de embargo por liquidación - " + nombreEmpleado);
            gastoEmbargo = gastoOperativoRepository.save(gastoEmbargo);
            liquidacion.setGastoEmbargoAsociado(gastoEmbargo);
        }

        return liquidacionRepository.save(liquidacion);
    }

    private BigDecimal calcularMontoVacaciones(Empleado emp, BigDecimal salarioDiario, LocalDate fechaSalida) {
        Period antiguedad = Period.between(emp.getFechaIngreso(), fechaSalida);
        int anios = antiguedad.getYears();
        int meses = antiguedad.getMonths();
        int diasVacaciones = 0;

        switch (meses) {
            case 5: diasVacaciones += 6; break;
            case 6: diasVacaciones += 7; break;
            case 7: diasVacaciones += 8; break;
            case 8: diasVacaciones += 9; break;
            case 9: diasVacaciones += 10; break;
            case 10: diasVacaciones += 11; break;
            case 11: diasVacaciones += 12; break;
            default: break;
        }

        if (anios >= 1) {
            int diasDerechoAnual = (anios >= configuracionService.getAniosVacacionesSenior())
                    ? configuracionService.getDiasPagoVacacionesSenior()
                    : configuracionService.getDiasPagoVacacionesBasico();

            int diasTomados = vacacionEmpleadoService.obtenerDiasVacacionesTomadosUltimoAnio(emp, fechaSalida);

            int diasPendientes = diasDerechoAnual - diasTomados;

            if (diasPendientes > 0) {
                diasVacaciones += diasPendientes;
            }
        }
        return salarioDiario.multiply(new BigDecimal(diasVacaciones)).setScale(2, RoundingMode.HALF_UP);
    }

    public int calcularDiasPreavisoLegales(Empleado emp, LocalDate fechaSalida) {
        Period antiguedad = Period.between(emp.getFechaIngreso(), fechaSalida);
        int anios = antiguedad.getYears();
        int meses = antiguedad.getMonths();

        int limiteMesesTramo2 = configuracionService.getPreavisoMesesMinimoTramo2();
        int limiteMesesTramo1 = configuracionService.getPreavisoMesesMinimoTramo1();

        if (anios >= 1) {
            return configuracionService.getPreavisoDiasTramo3();
        } else if (meses >= limiteMesesTramo2) {
            return configuracionService.getPreavisoDiasTramo2();
        } else if (meses >= limiteMesesTramo1) {
            return configuracionService.getPreavisoDiasTramo1();
        }
        return 0;
    }

    private BigDecimal calcularMontoCesantia(Empleado emp, BigDecimal salarioDiario, LocalDate fechaSalida) {
        Period antiguedad = Period.between(emp.getFechaIngreso(), fechaSalida);
        int anios = antiguedad.getYears();
        int meses = antiguedad.getMonths();
        int diasCesantia = 0;

        int limiteAniosTramo4 = configuracionService.getCesantiaAniosMinimoTramo4();
        int limiteMesesTramo2 = configuracionService.getCesantiaMesesMinimoTramo2();
        int limiteMesesTramo1 = configuracionService.getCesantiaMesesMinimoTramo1();

        if (anios >= 1 && anios < limiteAniosTramo4) {
            diasCesantia += anios * configuracionService.getCesantiaDiasTramo3();
        } else if (anios >= limiteAniosTramo4) {
            diasCesantia += anios * configuracionService.getCesantiaDiasTramo4();
        }

        if (meses >= limiteMesesTramo1 && meses < limiteMesesTramo2) {
            diasCesantia += configuracionService.getCesantiaDiasTramo1();
        } else if (meses >= limiteMesesTramo2) {
            diasCesantia += configuracionService.getCesantiaDiasTramo2();
        }

        return salarioDiario.multiply(new BigDecimal(diasCesantia)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularDescuentoEmbargo(Empleado emp, BigDecimal topeEmbargo, LocalDate fechaSalida) {
        List<EmbargoSalarial> embargos = embargoService.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(emp);

        int mesLiquidacion = fechaSalida.getMonthValue();
        int anioLiquidacion = fechaSalida.getYear();

        BigDecimal totalDeudaEmbargos = embargos.stream()
                .map(e -> {
                    BigDecimal mora = e.getSaldoPendienteMora() != null ? e.getSaldoPendienteMora() : BigDecimal.ZERO;

                    BigDecimal extras = e.getCuotasExtras().stream()
                            .filter(c -> {
                                int mesExtra = c.getMesAplicacion() != null ? c.getMesAplicacion() : -1;
                                int ultimoAnio = c.getUltimoAnioCobrado() != null ? c.getUltimoAnioCobrado() : 0;
                                return mesExtra == mesLiquidacion && ultimoAnio < anioLiquidacion;
                            })
                            .map(c -> c.getMontoExtra() != null ? c.getMontoExtra() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return mora.add(extras);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalDeudaEmbargos.min(topeEmbargo);
    }

    private BigDecimal sumarDeudaPrestamos(Long idEmpleado) {
        List<PrestamoEmpleado> prestamos = prestamoRepository.findByEmpleado_IdEmpleadoAndEstadoIn(
                idEmpleado, Arrays.asList(EstadoPrestamo.APROBADO, EstadoPrestamo.PAUSADO));

        return prestamos.stream()
                .map(PrestamoEmpleado::getBalanceCapitalPendiente)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarDeudaAnticipos(Long idEmpleado) {
        List<AnticipoSalario> anticipos = anticipoRepository.findByEmpleado_IdEmpleadoAndEstadoIn(
                idEmpleado, Arrays.asList(EstadoAnticipo.APROBADO, EstadoAnticipo.PAUSADO));

        return anticipos.stream()
                .map(AnticipoSalario::getSaldoPendiente)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void cerrarDeudasYEmbargos(Empleado empleado, BigDecimal abonoAnticipos, BigDecimal abonoPrestamos) {
        List<AnticipoSalario> anticipos = anticipoRepository.findByEmpleado_IdEmpleadoAndEstadoIn(
                empleado.getIdEmpleado(), Arrays.asList(EstadoAnticipo.APROBADO, EstadoAnticipo.PAUSADO));
        BigDecimal remanenteAnticipo = abonoAnticipos;

        for (AnticipoSalario a : anticipos) {
            if (remanenteAnticipo.compareTo(BigDecimal.ZERO) > 0) {
                if (remanenteAnticipo.compareTo(a.getSaldoPendiente()) >= 0) {
                    remanenteAnticipo = remanenteAnticipo.subtract(a.getSaldoPendiente());
                    a.setSaldoPendiente(BigDecimal.ZERO);
                } else {
                    a.setSaldoPendiente(a.getSaldoPendiente().subtract(remanenteAnticipo));
                    remanenteAnticipo = BigDecimal.ZERO;
                }
            }
            a.setEstado(EstadoAnticipo.CERRADO_POR_LIQUIDACION);
            anticipoRepository.save(a);
        }

        List<PrestamoEmpleado> prestamos = prestamoRepository.findByEmpleado_IdEmpleadoAndEstadoIn(
                empleado.getIdEmpleado(), Arrays.asList(EstadoPrestamo.APROBADO, EstadoPrestamo.PAUSADO));
        BigDecimal remanentePrestamo = abonoPrestamos;

        for (PrestamoEmpleado p : prestamos) {
            if (remanentePrestamo.compareTo(BigDecimal.ZERO) > 0) {
                if (remanentePrestamo.compareTo(p.getBalanceCapitalPendiente()) >= 0) {
                    remanentePrestamo = remanentePrestamo.subtract(p.getBalanceCapitalPendiente());
                    p.setBalanceCapitalPendiente(BigDecimal.ZERO);
                } else {
                    p.setBalanceCapitalPendiente(p.getBalanceCapitalPendiente().subtract(remanentePrestamo));
                    remanentePrestamo = BigDecimal.ZERO;
                }
            }
            p.setEstado(EstadoPrestamo.CERRADO_POR_LIQUIDACION);
            prestamoRepository.save(p);
        }

        List<EmbargoSalarial> embargos = embargoService.findByEmpleadoAndEstadoOrderByFechaNotificacionAsc(empleado);
        for(EmbargoSalarial e : embargos) {
            embargoService.cambiarEstado(e, EstadoEmbargo.CERRADO_POR_LIQUIDACION);
        }
    }

    public Optional<LiquidacionEmpleado> obtenerUltimaLiquidacion(Empleado empleado) {
        return liquidacionRepository.findFirstByEmpleadoOrderByFechaLiquidacionDesc(empleado);
    }

    private void validarCondicionesParaLiquidar(Empleado empleado) {
        if (empleado.getStatus() == com.agroveterinaria.enums.StatusEntidad.INACTIVO) {
            throw new IllegalStateException("Operación denegada: El empleado ya se encuentra INACTIVO.");
        }

        Optional<LiquidacionEmpleado> ultimaLiquidacion = liquidacionRepository.findFirstByEmpleadoOrderByFechaLiquidacionDesc(empleado);
        if (ultimaLiquidacion.isPresent()) {
            if (!ultimaLiquidacion.get().getFechaLiquidacion().isBefore(empleado.getFechaIngreso())) {
                throw new IllegalStateException("Operación denegada: Este empleado ya posee un registro de liquidación para su contrato actual.");
            }
        }

        if (corridaRepository.existsByEstado(com.agroveterinaria.enums.EstadoCorrida.PENDIENTE)) {
            throw new IllegalStateException(
                    "Operación denegada: Existe una corrida de nómina PENDIENTE. " +
                            "Si liquida al empleado ahora, sus préstamos y embargos se cerrarán y la nómina actual se descuadrará. " +
                            "Debe aprobar o eliminar la nómina pendiente antes de procesar una liquidación."
            );
        }
    }
}