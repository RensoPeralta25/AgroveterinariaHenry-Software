package com.agroveterinaria.service;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.EstadoPrestamo;
import com.agroveterinaria.enums.PeriodoNomina;
import com.agroveterinaria.enums.TipoConcepto;
import com.agroveterinaria.repository.CorridaNominaRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
@Transactional
@RolesAllowed("ADMINISTRADOR")
public class CorridaNominaService {
    private final CorridaNominaRepository corridaRepository;
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

    public CorridaNomina generarCorrida(PeriodoNomina periodo, LocalDate fecha) {
        CorridaNomina corrida = new CorridaNomina(periodo, fecha);
        List<Empleado> empleados = empleadoService.findAll();
        Set<Nomina> nominas = new LinkedHashSet<>();

        for (Empleado empleado : empleados) {
            prestamoEmpleadoService.validarIntegridadPrestamos(empleado);
            Nomina nomina = new Nomina(empleado, corrida);
            Set<DetalleNomina> detalles = new LinkedHashSet<>();

            BigDecimal salarioBase = empleado.getSalario();
            detalles.add(crearDetalle(nomina, TipoConcepto.SALARIO_BASE, "Salario base", salarioBase, 1.0));

            List<PrestamoEmpleado> prestamos = prestamoEmpleadoService.findByEmpleadoAndEstado(empleado);
            for (PrestamoEmpleado prestamo : prestamos) {
                BigDecimal montoACobrar = prestamo.getCuotaPeriodica().min(prestamo.getBalancePendiente());

                nomina.getDetalles().add(crearDetalle(nomina, TipoConcepto.PRESTAMO_EMPRESA,
                        "Cuota Préstamo: " + prestamo.getConcepto(), montoACobrar, 1.0));
            }

            List<EmbargoSalarial> embargos = embargoSalarialService.findByEmpleadoAndActivoTrue(empleado);
            for (EmbargoSalarial embargo : embargos) {
                nomina.getDetalles().add(crearDetalle(nomina, TipoConcepto.EMBARGO_SALARIAL,
                        "Embargo: " + embargo.getEntidadDemandante(), embargo.getMontoDescuento(), 1.0));
            }

            BigDecimal totalDevengado = salarioBase;

            BigDecimal afp = configuracionNominaService.calcularAFP(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.FONDO_PENSIONES, "AFP (2.87%)", afp, 1.0));

            BigDecimal sfs = configuracionNominaService.calcularSFS(totalDevengado);
            detalles.add(crearDetalle(nomina, TipoConcepto.SEGURO_FAMILIAR_SALUD, "SFS (3.04%)", sfs, 1.0));

            BigDecimal isr = configuracionNominaService.calcularISR(totalDevengado, periodo);
            if (isr.compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(crearDetalle(nomina, TipoConcepto.IMPUESTO_RENTA, "ISR", isr, 1.0));
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
}
