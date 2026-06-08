package com.agroveterinaria.service;

import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Ruta;
import com.agroveterinaria.entity.Vehiculo;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadMedida;
import com.agroveterinaria.repository.ProductoRepository;
import com.agroveterinaria.repository.ProveedorRepository;
import com.agroveterinaria.repository.RutaRepository;
import com.agroveterinaria.repository.VehiculoRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sound.sampled.Port;
import java.util.List;
import java.util.Optional;

@Service
@RolesAllowed("ADMINISTRADOR")
public class RutaService {

    private final RutaRepository rutaRepository;

    public RutaService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    @Transactional(readOnly = true)
    public List<Ruta> listarTodos() {
        return rutaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Ruta> buscarPorId(Long idRuta) {
        return rutaRepository.findById(idRuta);
    }

    @Transactional
    public Ruta guardar(Ruta ruta) {
        return rutaRepository.save(ruta);
    }

    @Transactional
    public void eliminarPorId(Long idRuta) {
        rutaRepository.deleteById(idRuta);
    }

    @Transactional
    public void eliminar(Ruta ruta) {
        rutaRepository.delete(ruta);
    }

}