package com.agroveterinaria.service;

import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.exception.ProveedorOperacionException;
import com.agroveterinaria.repository.ProveedorRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

@Service
@RolesAllowed("ADMINISTRADOR")
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
        try {
            // Fuerza la validación de las restricciones aquí para poder traducir
            // el error de la base de datos antes de enviarlo a la interfaz.
            return proveedorRepository.saveAndFlush(proveedor);
        } catch (DataIntegrityViolationException error) {
            String mensaje = tieneSqlState(error, "23505")
                    ? "Ya existe un proveedor registrado con ese RNC. Verifique el dato e intente nuevamente."
                    : "No se pudo guardar el proveedor. Verifique los datos ingresados e intente nuevamente.";
            throw new ProveedorOperacionException(
                    mensaje,
                    error);
        }
    }

    @Transactional
    public void eliminarPorId(Long idProveedor) {
        try {
            proveedorRepository.deleteById(idProveedor);
            // Al igual que al guardar, se ejecuta la eliminación en este punto
            // para identificar las relaciones que impiden borrarlo.
            proveedorRepository.flush();
        } catch (EmptyResultDataAccessException error) {
            throw new ProveedorOperacionException("El proveedor que intentas eliminar ya no existe.", error);
        } catch (DataIntegrityViolationException error) {
            String mensaje = tieneSqlState(error, "23503")
                    ? "No se puede eliminar este proveedor porque tiene compras, pagos u otros registros asociados."
                    : "No se pudo eliminar el proveedor. Intenta nuevamente.";
            throw new ProveedorOperacionException(
                    mensaje,
                    error);
        }
    }

    private boolean tieneSqlState(Throwable error, String sqlState) {
        Throwable causa = error;
        while (causa != null) {
            if (causa instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
