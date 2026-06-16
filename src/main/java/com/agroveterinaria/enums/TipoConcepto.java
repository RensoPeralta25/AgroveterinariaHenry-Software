package com.agroveterinaria.enums;

public enum TipoConcepto {
    // Ingresos
    SALARIO_BASE,
    HORAS_EXTRAS,
    COMISIONES,
    BONIFICACIONES,
    PAGO_VACACIONES,
    SUELDO_13,
    DIETAS_Y_VIATICOS,

    // Deducciones
    SEGURO_FAMILIAR_SALUD,
    FONDO_PENSIONES,
    IMPUESTO_RENTA,
    ANTICIPO_SALARIO,
    PRESTAMO_EMPRESA,
    AUSENCIAS_NO_PAGADAS,
    EMBARGO_SALARIAL,
    OTRAS_DEDUCCIONES;

    public boolean esIngreso() {
        return this == SALARIO_BASE || this == HORAS_EXTRAS ||
                this == COMISIONES  || this == BONIFICACIONES ||
                this == PAGO_VACACIONES || this == SUELDO_13 ||
                this == DIETAS_Y_VIATICOS;
    }

    public boolean esDeduccion() {
        return !esIngreso();
    }
}
