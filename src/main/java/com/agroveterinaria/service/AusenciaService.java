package com.agroveterinaria.service;

import com.agroveterinaria.entity.Ausencia;
import com.agroveterinaria.entity.Nomina;
import com.agroveterinaria.entity.VacacionEmpleado;
import com.agroveterinaria.enums.EstadoRegistro;
import com.agroveterinaria.repository.AusenciaRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@RolesAllowed({"ADMINISTRADOR", "RECURSOS_HUMANOS"})
public class AusenciaService {

    private final AusenciaRepository ausenciaRepository;
    private final VacacionEmpleadoService vacacionEmpleadoService;
    private final HistorialDevengadoAnualService historialDevengadoAnualService;
    private final ConfiguracionNominaService configuracionNominaService;

    public List<Ausencia> findAll() {
        return ausenciaRepository.findAll();
    }

    public List<Ausencia> findAllConRelaciones() {
        return ausenciaRepository.findAllConRelaciones();
    }

    @Transactional
    public Ausencia registrarAusencia(Ausencia ausencia) {
        if (ausencia.getEmpleado() == null) throw new IllegalArgumentException("El empleado es obligatorio.");
        if (ausencia.getTipoAusencia() == null) throw new IllegalArgumentException("El tipo de ausencia es obligatorio.");
        if (ausencia.getEstadoRegistro() == null) throw new IllegalArgumentException("El estado del registro es obligatorio.");
        if (ausencia.getFechaInicio() == null) throw new IllegalArgumentException("La fecha de inicio es obligatoria.");

        if (ausencia.getEstadoRegistro() == EstadoRegistro.CERRADA && ausencia.getFechaFin() == null) {
            throw new IllegalArgumentException("Una ausencia CERRADA debe tener obligatoriamente una fecha de fin.");
        }

        if (ausencia.getFechaFin() != null && ausencia.getFechaFin().isBefore(ausencia.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        LocalDate finValidacion = ausencia.getFechaFin() != null ? ausencia.getFechaFin() : ausencia.getFechaInicio();

        long ausenciasSolapadas = ausenciaRepository.countAusenciasSolapadas(
                ausencia.getEmpleado().getIdEmpleado(),
                ausencia.getFechaInicio(),
                ausencia.getFechaFin(),
                ausencia.getId()
        );

        if (ausenciasSolapadas > 0) {
            throw new IllegalStateException("El empleado ya tiene otra ausencia registrada que choca con este rango de fechas.");
        }

        List<VacacionEmpleado> vacaciones = vacacionEmpleadoService.encontrarVacacionesEnPeriodo(
                ausencia.getEmpleado(), ausencia.getFechaInicio(), finValidacion);

        if (!vacaciones.isEmpty()) {
            throw new IllegalStateException("El empleado ya tiene vacaciones aprobadas en este rango de fechas.");
        }

        if (ausencia.getId() != null) {
            Ausencia ausenciaExistente = ausenciaRepository.findById(ausencia.getId())
                    .orElseThrow(() -> new IllegalArgumentException("El registro de ausencia no existe."));

            if (ausenciaExistente.getEstadoRegistro() == EstadoRegistro.CERRADA && ausencia.getEstadoRegistro() == EstadoRegistro.ABIERTA) {
                throw new IllegalStateException("No se puede reabrir a estado ABIERTA una ausencia que ya impactó la nómina.");
            }

            if (ausenciaExistente.isAplicadaEnNomina()) {
                if (!ausenciaExistente.getEmpleado().getIdEmpleado().equals(ausencia.getEmpleado().getIdEmpleado()) ||
                        !ausenciaExistente.getFechaInicio().isEqual(ausencia.getFechaInicio())) {
                    throw new IllegalStateException("Auditoría: No se permite cambiar el empleado ni la fecha de inicio de una ausencia ya procesada en nómina.");
                }

                boolean eraInjustificada = ausenciaExistente.getTipoAusencia().isReduceTiempoEfectivo();
                boolean ahoraEsProtegida = !ausencia.getTipoAusencia().isGeneraPagoEmpleador() && !ausencia.getTipoAusencia().isReduceTiempoEfectivo();

                if (eraInjustificada && ahoraEsProtegida && ausenciaExistente.getDiasDescontadosAcumulados() > 0) {

                    BigDecimal divisorOficial = configuracionNominaService.getDivisorMensualDiario();
                    BigDecimal salarioBaseDiario = ausenciaExistente.getEmpleado().getSalario().divide(divisorOficial, 2, java.math.RoundingMode.HALF_UP);

                    BigDecimal compensacionVirtualPerdida = salarioBaseDiario.multiply(BigDecimal.valueOf(ausenciaExistente.getDiasDescontadosAcumulados()));

                    int anioNomina = ausenciaExistente.getNominaAplicada().getCorrida().getFechaEmision().getYear();
                    int mesNomina = ausenciaExistente.getNominaAplicada().getCorrida().getFechaEmision().getMonthValue();

                    historialDevengadoAnualService.registrarOActualizarDevengado(
                            ausenciaExistente.getEmpleado(),
                            anioNomina,
                            mesNomina,
                            compensacionVirtualPerdida
                    );
                }
            }
        }

        return ausenciaRepository.save(ausencia);
    }

    public void delete(Ausencia ausencia) {
        if (ausencia.isAplicadaEnNomina()) {
            throw new IllegalStateException("Acción denegada: No se puede eliminar una ausencia que ya tiene un impacto histórico en una nómina aprobada.");
        }

        ausenciaRepository.delete(ausencia);
    }

    public List<Ausencia> obtenerAusenciasPendientes(Long idEmpleado) {
        return ausenciaRepository.findAusenciasPendientesPorEmpleado(idEmpleado, EstadoRegistro.ABIERTA);
    }

    @Transactional
    public void marcarAusenciasComoAplicadas(List<Ausencia> ausencias, Nomina nomina) {
        for (Ausencia ausencia : ausencias) {

            int nuevoAcumulado = (ausencia.getDiasDescontadosAcumulados() != null ? ausencia.getDiasDescontadosAcumulados() : 0)
                    + (ausencia.getDiasADescontarEnEstaCorrida() != null ? ausencia.getDiasADescontarEnEstaCorrida() : 0);

            ausencia.setDiasDescontadosAcumulados(nuevoAcumulado);

            if (ausencia.getEstadoRegistro() == EstadoRegistro.CERRADA) {
                int diasTotalesReales = (int) calcularDiasAusenciaEnRango(ausencia, ausencia.getFechaInicio(), ausencia.getFechaFin());
                if (nuevoAcumulado >= diasTotalesReales) {
                    ausencia.setAplicadaEnNomina(true);
                    ausencia.setNominaAplicada(nomina);
                }
            }

            ausenciaRepository.save(ausencia);
        }
    }

    public long sumarDiasAusenciaNoPagadaEnRango(Long idEmpleado, LocalDate inicioPeriodo, LocalDate finPeriodo) {
        List<Ausencia> ausencias = ausenciaRepository.findAusenciasEnRango(idEmpleado, inicioPeriodo, finPeriodo);
        long diasARestar = 0;

        for (Ausencia ausencia : ausencias) {
            if (ausencia.getTipoAusencia().isReduceTiempoEfectivo()) {
                diasARestar += calcularDiasAusenciaEnRango(ausencia, inicioPeriodo, finPeriodo);
            }
        }
        return diasARestar;
    }

    public long calcularDiasAusenciaEnRango(Ausencia ausencia, LocalDate inicioPeriodo, LocalDate finPeriodo) {
        LocalDate inicioReal = ausencia.getFechaInicio().isBefore(inicioPeriodo) ? inicioPeriodo : ausencia.getFechaInicio();

        LocalDate finReal;
        if (ausencia.getEstadoRegistro() == EstadoRegistro.ABIERTA || ausencia.getFechaFin() == null) {
            finReal = finPeriodo;
        } else {
            finReal = ausencia.getFechaFin().isAfter(finPeriodo) ? finPeriodo : ausencia.getFechaFin();
        }

        long dias = 0;
        if (!inicioReal.isAfter(finReal)) {
            LocalDate iterador = inicioReal;
            while (!iterador.isAfter(finReal)) {
                if (iterador.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                    dias++;
                }
                iterador = iterador.plusDays(1);
            }
        }
        return dias;
    }
}