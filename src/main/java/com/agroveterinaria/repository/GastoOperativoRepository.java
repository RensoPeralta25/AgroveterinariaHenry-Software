package com.agroveterinaria.repository;

import com.agroveterinaria.entity.GastoOperativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GastoOperativoRepository extends JpaRepository<GastoOperativo, Long> {
}