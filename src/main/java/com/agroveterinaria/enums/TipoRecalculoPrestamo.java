package com.agroveterinaria.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoRecalculoPrestamo {
    REDUCIR_CUOTA("Reducir cuota"),
    REDUCIR_PLAZO("Reducir plazo"),;

    private final String descripcion;
}