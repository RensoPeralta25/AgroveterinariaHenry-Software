package com.agroveterinaria.service;

import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.repository.AlmacenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AlmacenService {

    private final AlmacenRepository almacenRepository;

    public AlmacenService(AlmacenRepository almacenRepository) {
        this.almacenRepository = almacenRepository;
    }

    @Transactional(readOnly = true)
    public List<Almacen> listarTodos() {
        return almacenRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Almacen> buscarPorId(Long idAlmacen) {
        return almacenRepository.findById(idAlmacen);
    }

    @Transactional
    public Almacen guardar(Almacen almacen) {
        return almacenRepository.save(almacen);
    }

    @Transactional
    public void eliminarPorId(Long idAlmacen) {
        almacenRepository.deleteById(idAlmacen);
    }

    @Transactional
    public void eliminar(Almacen almacen) {
        almacenRepository.delete(almacen);
    }

}