package com.agroveterinaria.service;

import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Ruta;
import com.agroveterinaria.entity.Vehiculo;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadMedida;
import com.agroveterinaria.repository.*;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sound.sampled.Port;
import java.util.List;
import java.util.Optional;

@Service
@RolesAllowed("ADMINISTRADOR")
public class RutaService {

    private final RutaRepository rutaRepository;
    private final RecepcionRepository recepcionRepository;

    public RutaService(RutaRepository rutaRepository, RecepcionRepository recepcionRepository) {
        this.rutaRepository = rutaRepository;
        this.recepcionRepository = recepcionRepository;
    }

    @Transactional(readOnly = true)
    public List<Ruta> listarTodos() {
        return rutaRepository.findAllConParadas();
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
    public void eliminar(Long idRuta) {
        Ruta ruta = rutaRepository.findById(idRuta)
                .orElseThrow(() -> new IllegalArgumentException("La ruta seleccionada no existe."));

        if (recepcionRepository.existsByRutaEnTransporte(ruta)) {
            throw new IllegalStateException("No se puede eliminar la ruta porque ya está asociada al transporte de una recepción de mercancía.");
        }

        try {
            rutaRepository.delete(ruta);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No es posible eliminar la ruta debido a restricciones de integridad en el sistema.");
        }
    }

}