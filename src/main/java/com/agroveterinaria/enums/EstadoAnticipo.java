package com.agroveterinaria.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoAnticipo {
    PENDIENTE("Pendiente"),
    APROBADO("Aprobado"),
    SALDADO("Saldado"),
    PAUSADO("Pausado"),
    CONDONADO("Condonado"),
    CERRADO_POR_LIQUIDACION("Cerrado (Liq.)");

    private final String descripcion;
}
