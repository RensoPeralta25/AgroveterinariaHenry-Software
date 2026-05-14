package com.agroveterinaria.entity;

import com.agroveterinaria.enums.StatusEntidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Proveedor {

    private Long idProveedor;
    private String rnc;
    private String nombre;
    private String direccion;
    private String telefono;
    private StatusEntidad status;
    private String numPersonaContacto;

    public Proveedor() {
    }

    public Proveedor(Long idProveedor, String rnc, String nombre, String direccion, String telefono,
            StatusEntidad status, String numPersonaContacto) {
        this.idProveedor = idProveedor;
        this.rnc = rnc;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.status = status;
        this.numPersonaContacto = numPersonaContacto;
    }
}
