package com.agroveterinaria.repository;

import com.agroveterinaria.entity.DevolucionVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DevolucionVentaRepository extends JpaRepository<DevolucionVenta, Long> {

}