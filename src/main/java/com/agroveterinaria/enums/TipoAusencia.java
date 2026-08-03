package com.agroveterinaria.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoAusencia {
    INJUSTIFICADA(false, true, "Ausencia injustificada"),
    LICENCIA_VOLUNTARIA(false, true, "Permiso sin disfrute de sueldo"),
    SUSPENSION_DISCIPLINARIA(false, true, "Suspensión disciplinaria"),
    MATERNIDAD(false, false, "Licencia por maternidad"),
    ACCIDENTE_LABORAL(false, false, "Riesgo Laboral"),
    ENFERMEDAD_COMUN(false, true, "Enfermedad común"),
    PATERNIDAD(true, false, "Licencia por paternidad"),
    MATRIMONIO(true, false, "Licencia por matrimonio"),
    LUTO(true, false, "Licencia por fallecimiento");

    private final boolean generaPagoEmpleador;
    private final boolean reduceTiempoEfectivo;
    private final String descripcion;
}