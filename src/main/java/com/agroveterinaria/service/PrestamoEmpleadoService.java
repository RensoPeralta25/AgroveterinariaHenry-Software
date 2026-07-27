package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.PrestamoEmpleado;
import com.agroveterinaria.enums.EstadoAnticipo;
import com.agroveterinaria.enums.EstadoPrestamo;
import com.agroveterinaria.repository.AnticipoSalarioRepository;
import com.agroveterinaria.repository.DetalleNominaRepository;
import com.agroveterinaria.repository.PrestamoEmpleadoRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Service
@RolesAllowed("ADMINISTRADOR")
@Transactional
public class PrestamoEmpleadoService {
    private final PrestamoEmpleadoRepository prestamoEmpleadoRepository;
    private final DetalleNominaRepository detalleNominaRepository;
    private final AnticipoSalarioRepository anticipoSalarioRepository;
    private final ConfiguracionNominaService configuracionNominaService;


    public PrestamoEmpleado save(PrestamoEmpleado prestamo) {
        if (prestamo.getIdPrestamo() == null) {

            boolean tieneAnticipoActivo = anticipoSalarioRepository.existsByEmpleadoAndEstadoIn(
                    prestamo.getEmpleado(),
                    Arrays.asList(EstadoAnticipo.PENDIENTE, EstadoAnticipo.APROBADO)
            );

            if (tieneAnticipoActivo) {
                throw new IllegalStateException("El empleado ya posee un Anticipo pendiente o aprobado. Debe saldarse antes de solicitar un Préstamo.");
            }

            BigDecimal porcentajeMaximo = configuracionNominaService.getPorcentajeMaximoPrestamo();

            BigDecimal limiteLegal = prestamo.getEmpleado().getSalario().multiply(porcentajeMaximo);

            if (prestamo.getCuotaPeriodica().compareTo(limiteLegal) > 0) {
                BigDecimal porcentajeVisual = porcentajeMaximo.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

                throw new IllegalArgumentException(
                        "Error: La cuota de RD$ " + prestamo.getCuotaPeriodica() + " supera el límite legal del " +
                                porcentajeVisual + "% del salario del empleado (Máximo permitido: RD$ " +
                                limiteLegal.setScale(2, RoundingMode.HALF_UP) + ")."
                );
            }
        }

        return prestamoEmpleadoRepository.save(prestamo);
    }

    public List<PrestamoEmpleado> findAll() {
        return prestamoEmpleadoRepository.findAll();
    }

    public List<PrestamoEmpleado> findByEmpleadoAndEstado(Empleado empleado) {
        return prestamoEmpleadoRepository.findByEmpleadoAndEstado(empleado, EstadoPrestamo.ACTIVO);
    }

    public void validarIntegridadPrestamos(Empleado empleado) {
        BigDecimal totalPagado = detalleNominaRepository.sumarPagosDePrestamosPorEmpleado(empleado.getIdEmpleado());

        List<PrestamoEmpleado> todosLosPrestamos = prestamoEmpleadoRepository.findByEmpleado(empleado);
        BigDecimal totalPrestado = todosLosPrestamos.stream()
                .map(PrestamoEmpleado::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balancePendienteActual = todosLosPrestamos.stream()
                .map(PrestamoEmpleado::getBalancePendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balanceCalculado = totalPrestado.subtract(totalPagado);
        
        if (balancePendienteActual.compareTo(balanceCalculado) != 0) {
            throw new IllegalStateException(
                    "ALERTA DE AUDITORÍA: Los balances de préstamo de " + empleado.getPersona().getNombre() +
                            " han sido alterados externamente. " +
                            "Deuda real calculada: RD$ " + balanceCalculado +
                            ", pero el sistema registra: RD$ " + balancePendienteActual);
        }
    }

    public boolean existsByEmpleado(Empleado empleado){ return prestamoEmpleadoRepository.existsByEmpleado(empleado); }

    public boolean existsByEmpleadoAndEstado(Empleado empleadoSeleccionado, EstadoPrestamo estadoPrestamo) {
        return prestamoEmpleadoRepository.existsByEmpleadoAndEstado(empleadoSeleccionado, estadoPrestamo);
    }
}
