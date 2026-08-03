package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.HistorialDevengadoAnual;
import com.agroveterinaria.repository.HistorialDevengadoAnualRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HistorialDevengadoAnualService {

    private final HistorialDevengadoAnualRepository historialRepository;

    @Transactional
    public void registrarOActualizarDevengado(Empleado empleado, int anio, int mes, BigDecimal montoPercibido) {
        Optional<HistorialDevengadoAnual> historialOpt =
                historialRepository.findByEmpleadoIdEmpleadoAndAnioAndMes(empleado.getIdEmpleado(), anio, mes);

        if (historialOpt.isPresent()) {
            HistorialDevengadoAnual historial = historialOpt.get();
            BigDecimal nuevoTotal = historial.getMontoDevengadoReal().add(montoPercibido);
            historial.setMontoDevengadoReal(nuevoTotal);
            historialRepository.save(historial);
        } else {
            HistorialDevengadoAnual nuevoHistorial = new HistorialDevengadoAnual();
            nuevoHistorial.setEmpleado(empleado);
            nuevoHistorial.setAnio(anio);
            nuevoHistorial.setMes(mes);
            nuevoHistorial.setMontoDevengadoReal(montoPercibido);
            historialRepository.save(nuevoHistorial);
        }
    }

    public BigDecimal sumarDevengadoAnualPorEmpleado(Long idEmpleado, int anio) {
        return historialRepository.sumarDevengadoAnualPorEmpleado(idEmpleado, anio);
    }
}