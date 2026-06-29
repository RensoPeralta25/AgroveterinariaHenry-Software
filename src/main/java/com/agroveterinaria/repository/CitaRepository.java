package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Cita;
import com.agroveterinaria.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByClienteIdCliente(Long idCliente);

    List<Cita> findByPacienteIdMascota(Long idMascota);

    long countByFechaHoraBetweenAndRealizadoFalse(LocalDateTime inicio, LocalDateTime fin);

    boolean existsByVeterinarioAndFechaHora(Empleado veterinario, LocalDateTime fechaHora);

    boolean existsByVeterinarioAndFechaHoraAndIdCitaNot(Empleado veterinario, LocalDateTime fechaHora, Long idCita);
}
