package com.agroveterinaria.service;

import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Vehiculo;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.EstadoVehiculo;
import com.agroveterinaria.enums.UnidadMedida;
import com.agroveterinaria.repository.ProductoRepository;
import com.agroveterinaria.repository.ProveedorRepository;
import com.agroveterinaria.repository.VehiculoRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sound.sampled.Port;
import java.util.List;
import java.util.Optional;

@Service
@RolesAllowed("ADMINISTRADOR")
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> listarTodos() {
        return vehiculoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> listarDisponibles() {
        return vehiculoRepository.findByEstado(EstadoVehiculo.DISPONIBLE);
    }

    @Transactional(readOnly = true)
    public Optional<Vehiculo> buscarPorId(Long idVehiculo) {
        return vehiculoRepository.findById(idVehiculo);
    }

    @Transactional
    public Vehiculo guardar(Vehiculo vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    @Transactional
    public void eliminarPorId(Long idVehiculo) {
        vehiculoRepository.deleteById(idVehiculo);
    }

    @Transactional
    public void eliminar(Vehiculo vehiculo) {
        vehiculoRepository.delete(vehiculo);
    }

}