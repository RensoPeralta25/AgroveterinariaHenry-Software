package com.agroveterinaria.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoPrestamo {
    PENDIENTE("Pendiente"),
    APROBADO("Aprobado"),
    SALDADO("Saldado");

    private final String descripcion;

}