package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Vehiculo;
import com.agroveterinaria.enums.EstadoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByEstado(EstadoVehiculo estado);
}
