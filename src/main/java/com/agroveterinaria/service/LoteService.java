package com.agroveterinaria.service;

import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.repository.LoteRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RolesAllowed("ADMINISTRADOR")
public class LoteService {

    private final LoteRepository loteRepository;

    public LoteService(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    @Transactional(readOnly = true)
    public List<Lote> listarTodos() {
        return loteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Lote> buscarPorId(Long idLote) {
        return loteRepository.findById(idLote);
    }

    @Transactional
    public Lote guardar(Lote lote) {
        return loteRepository.save(lote);
    }

    @Transactional
    public void eliminarPorId(Long idLote) {
        loteRepository.deleteById(idLote);
    }

    @Transactional
    public void eliminar(Lote lote) {
        loteRepository.delete(lote);
    }

    public List<Lote> buscarPorProducto(Producto producto) {
        return loteRepository.findByProducto(producto);
    }

}