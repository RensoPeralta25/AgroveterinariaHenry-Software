package com.agroveterinaria.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MotivoSalida {
    RENUNCIA("Renunciado"),
    DESPIDO_JUSTIFICADO("Despido Justificado"),
    DESAHUCIO("Desahuciado"),;

    private final String descripcion;
}
