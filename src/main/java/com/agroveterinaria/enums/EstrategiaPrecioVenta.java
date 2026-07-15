package com.agroveterinaria.enums;

import lombok.Getter;

@Getter
public enum EstrategiaPrecioVenta {
    NORMAL("Normal"),
    TODO_PRECIO_EMPAQUE("Precio de empaque"),
    TODO_PRECIO_FRACCION("Precio de fraccion");

    private final String etiqueta;

    EstrategiaPrecioVenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public static EstrategiaPrecioVenta fromEtiqueta(String etiqueta) {
        for (EstrategiaPrecioVenta estrat : EstrategiaPrecioVenta.values()) {
            if (estrat.getEtiqueta().equalsIgnoreCase(etiqueta)) return estrat;
        }
        return null;
    }
}
