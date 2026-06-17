package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByStatus(com.agroveterinaria.enums.StatusEntidad status);
}
