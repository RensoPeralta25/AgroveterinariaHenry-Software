package com.agroveterinaria.service;

import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Transporte;
import com.agroveterinaria.repository.LoteRepository;
import com.agroveterinaria.repository.TransporteRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TransporteService {

    private final TransporteRepository transporteRepository;

    public TransporteService(TransporteRepository transporteRepository) {
        this.transporteRepository = transporteRepository;
    }

    @Transactional(readOnly = true)
    public List<Transporte> listarTodos() {
        return transporteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Transporte> buscarPorId(Long idTransporte) {
        return transporteRepository.findById(idTransporte);
    }

    @Transactional
    public Transporte guardar(Transporte transporte) {
        return transporteRepository.save(transporte);
    }

    @Transactional
    public void eliminarPorId(Long idTransporte) {
        transporteRepository.deleteById(idTransporte);
    }

    @Transactional
    public void eliminar(Transporte transporte) {
        transporteRepository.delete(transporte);
    }
}