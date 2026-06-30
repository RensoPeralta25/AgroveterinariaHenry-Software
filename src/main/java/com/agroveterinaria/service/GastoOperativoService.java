package com.agroveterinaria.service;

import com.agroveterinaria.entity.GastoOperativo;
import com.agroveterinaria.repository.GastoOperativoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GastoOperativoService {

    private final GastoOperativoRepository repository;

    public GastoOperativoService(GastoOperativoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GastoOperativo> listarTodos() {
        return repository.findAllByOrderByFechaDesc();
    }

    @Transactional
    public GastoOperativo guardar(GastoOperativo gasto) {
        return repository.save(gasto);
    }

    @Transactional
    public void eliminar(Long idGasto) {
        try {
            repository.deleteById(idGasto);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No se puede eliminar este gasto porque ya está enlazado a un transporte o a un mantenimiento en el sistema.");
        }
    }
}