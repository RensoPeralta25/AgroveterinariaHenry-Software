package com.agroveterinaria.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoRegistro {
    ABIERTA("Abierta"),
    CERRADA("Cerrada");

    private final String descripcion;
}
