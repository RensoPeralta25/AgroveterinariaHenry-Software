package com.agroveterinaria.enums;

public enum TipoConcepto {
    // Ingresos
    SALARIO_BASE,
    HORAS_EXTRAS,
    COMISIONES_REGULARES,
    COMISIONES_EXTRAORDINARIAS,
    BONIFICACIONES,
    PAGO_VACACIONES,
    SUELDO_13,
    DIETAS_Y_VIATICOS,
    REEMBOLSO_LICENCIA,

    // Deducciones
    SEGURO_FAMILIAR_SALUD,
    FONDO_PENSIONES,
    IMPUESTO_RENTA,
    ANTICIPO_SALARIO,
    PRESTAMO_EMPRESA,
    DESCUENTO_AUSENCIA,
    INFOTEP,
    EMBARGO_SALARIAL;

    public boolean esIngreso() {
        return this == SALARIO_BASE || this == HORAS_EXTRAS ||
                this == COMISIONES_REGULARES || this == COMISIONES_EXTRAORDINARIAS  ||
                this == BONIFICACIONES || this == PAGO_VACACIONES ||
                this == SUELDO_13 || this == DIETAS_Y_VIATICOS || this == REEMBOLSO_LICENCIA;
    }

    public boolean esDeduccion() {
        return !esIngreso();
    }
}
