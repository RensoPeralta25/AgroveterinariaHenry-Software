package com.agroveterinaria.service;

import com.agroveterinaria.entity.Cita;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Mascota;
import com.agroveterinaria.repository.CitaRepository;
import com.agroveterinaria.repository.MascotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CitaService {

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;

    public CitaService(CitaRepository citaRepository, MascotaRepository mascotaRepository) {
        this.citaRepository = citaRepository;
        this.mascotaRepository = mascotaRepository;
    }

    @Transactional(readOnly = true)
    public List<Cita> findAll() {
        return citaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cita> findByCliente(Long idCliente) {
        return citaRepository.findByClienteIdCliente(idCliente);
    }

    @Transactional(readOnly = true)
    public List<Cita> findByMascota(Long idMascota) {
        return citaRepository.findByPacienteIdMascota(idMascota);
    }

    @Transactional(readOnly = true)
    public List<Mascota> findMascotasByCliente(Long idCliente) {
        if (idCliente == null) {
            return List.of();
        }
        return mascotaRepository.findByClienteIdCliente(idCliente);
    }

    public Cita save(Cita cita) {
        validar(cita);
        if (cita.getRealizado() == null) {
            cita.setRealizado(false);
        }
        return citaRepository.save(cita);
    }

    public void delete(Cita cita) {
        citaRepository.delete(cita);
    }

    public Cita marcarComoRealizada(Long idCita) {
        Cita cita = buscarExistente(idCita);
        cita.setRealizado(true);
        return citaRepository.save(cita);
    }

    public Cita marcarComoPendiente(Long idCita) {
        Cita cita = buscarExistente(idCita);
        cita.setRealizado(false);
        return citaRepository.save(cita);
    }

    private Cita buscarExistente(Long idCita) {
        return citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
    }

    private void validar(Cita cita) {
        if (cita == null) {
            throw new IllegalArgumentException("La cita es obligatoria");
        }
        if (cita.getCliente() == null) {
            throw new IllegalArgumentException("La cita debe tener un cliente asociado");
        }
        if (cita.getPaciente() == null) {
            throw new IllegalArgumentException("La cita debe tener una mascota asociada");
        }
        if (cita.getVeterinario() == null) {
            throw new IllegalArgumentException("La cita debe tener un veterinario asociado");
        }
        if (cita.getServicio() == null) {
            throw new IllegalArgumentException("La cita debe tener un servicio asociado");
        }
        if (cita.getFechaHora() == null) {
            throw new IllegalArgumentException("La fecha y hora de la cita son obligatorias");
        }

        validarMascotaPerteneceAlCliente(cita.getCliente(), cita.getPaciente());
        validarDuplicado(cita);
    }

    private void validarMascotaPerteneceAlCliente(Cliente cliente, Mascota mascota) {
        Cliente propietario = mascota.getCliente();
        if (propietario == null) {
            return;
        }

        Long idCliente = cliente.getIdCliente();
        Long idPropietario = propietario.getIdCliente();

        if (idCliente != null && idPropietario != null && !idCliente.equals(idPropietario)) {
            throw new IllegalArgumentException("La mascota seleccionada no pertenece al cliente de la cita");
        }
    }

    private void validarDuplicado(Cita cita) {
        boolean existeDuplicado = cita.getIdCita() == null
                ? citaRepository.existsByVeterinarioAndFechaHora(cita.getVeterinario(), cita.getFechaHora())
                : citaRepository.existsByVeterinarioAndFechaHoraAndIdCitaNot(
                        cita.getVeterinario(),
                        cita.getFechaHora(),
                        cita.getIdCita()
                );

        if (existeDuplicado) {
            throw new IllegalArgumentException("Ya existe una cita para ese veterinario en la misma fecha y hora");
        }
    }
}
