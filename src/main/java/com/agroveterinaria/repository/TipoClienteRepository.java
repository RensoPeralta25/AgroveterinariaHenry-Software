package com.agroveterinaria.repository;

import com.agroveterinaria.entity.TipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoClienteRepository extends JpaRepository<TipoCliente, Long> {
}
