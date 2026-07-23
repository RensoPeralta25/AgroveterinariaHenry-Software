package com.agroveterinaria.repository;

import com.agroveterinaria.entity.CuotaExtraEmbargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CuotaExtraEmbargoRepository extends JpaRepository<CuotaExtraEmbargo, Long> {
}