package com.agroveterinaria.dto.despacho;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DespachoResumenDTO {
    private Long idDespacho;
    private String codigo;
    private String tipo;
    private String destinatario;
    private String direccionEntrega;
    private LocalDateTime fechaProgramadaRaw;
    private String fechaProgramadaFormateada;
    private String estado;
}