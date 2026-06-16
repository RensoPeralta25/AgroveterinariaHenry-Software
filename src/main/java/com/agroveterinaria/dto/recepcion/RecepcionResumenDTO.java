package com.agroveterinaria.dto.recepcion;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecepcionResumenDTO {
    private String codigo;
    private String tipo;
    private String origen;
    private LocalDateTime fechaRaw;
    private String fechaFormateada;
    private String estado;
    private com.agroveterinaria.entity.Transporte transporteDespacho;

    private com.agroveterinaria.entity.Compra compraOriginal;
    private com.agroveterinaria.entity.Transferencia transferenciaOriginal;
}