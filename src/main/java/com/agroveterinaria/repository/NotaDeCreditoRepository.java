package com.agroveterinaria.repository;

import com.agroveterinaria.entity.NotaDeCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotaDeCreditoRepository extends JpaRepository<NotaDeCredito, Long> {

}