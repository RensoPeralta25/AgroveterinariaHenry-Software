package com.agroveterinaria.dto.nomina;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuotaAmortizacionDTO {
    private int numeroCuota;
    private BigDecimal pagoInteres;
    private BigDecimal pagoCapital;
    private BigDecimal cuotaTotal;
    private BigDecimal balanceRestante;
}