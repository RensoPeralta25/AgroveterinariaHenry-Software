package com.agroveterinaria.repository;

import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.EstadoVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    @EntityGraph(attributePaths = {"cliente.persona"})
    List<Venta> findByEstadoAndLlevaDespachoTrue(EstadoVenta estado);
}
