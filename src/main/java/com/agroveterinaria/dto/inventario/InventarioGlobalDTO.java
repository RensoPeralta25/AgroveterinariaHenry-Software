package com.agroveterinaria.dto.inventario;

import com.agroveterinaria.entity.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventarioGlobalDTO {
    private Producto producto;
    private BigDecimal totalGlobal;
}