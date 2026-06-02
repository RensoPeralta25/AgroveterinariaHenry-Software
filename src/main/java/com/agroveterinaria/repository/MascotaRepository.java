package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByClienteIdCliente(Long idCliente);

    List<Mascota> findByIdMascota(Long idMascota);
}
