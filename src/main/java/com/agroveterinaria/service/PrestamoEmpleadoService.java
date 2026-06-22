package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.PrestamoEmpleado;
import com.agroveterinaria.enums.EstadoPrestamo;
import com.agroveterinaria.repository.DetalleNominaRepository;
import com.agroveterinaria.repository.PrestamoEmpleadoRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@Service
@RolesAllowed("ADMINISTRADOR")
@Transactional
public class PrestamoEmpleadoService {
    private final PrestamoEmpleadoRepository prestamoEmpleadoRepository;
    private final DetalleNominaRepository detalleNominaRepository;

    public PrestamoEmpleado save(PrestamoEmpleado prestamo) {
        return prestamoEmpleadoRepository.save(prestamo);
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
}
