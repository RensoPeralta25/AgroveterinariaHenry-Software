package com.agroveterinaria.service;

import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.repository.ProveedorRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarTodos() {
        return proveedorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Proveedor> buscarPorId(Long idProveedor) {
        return proveedorRepository.findById(idProveedor);
    }

    @Transactional
    public Proveedor guardar(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public void eliminarPorId(Long idProveedor) {
        proveedorRepository.deleteById(idProveedor);
    }
}
