package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {

}
