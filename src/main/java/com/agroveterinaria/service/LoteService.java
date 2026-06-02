package com.agroveterinaria.service;

import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
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

}