package com.agroveterinaria.entity;

import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadMedida;
import com.vaadin.pro.licensechecker.Product;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Producto {

    private Long idProducto;
    private String nombre;
    private BigDecimal precioUnitario;
    private CategoriaProducto categoria;
    private BigDecimal presentacion;
    private UnidadMedida unidadMedida;
    private byte[] foto;

    public Producto(){}

    public Producto(Long idProducto, String nombre, BigDecimal precioUnitario, CategoriaProducto categoria, BigDecimal presentacion, UnidadMedida unidadMedida, byte[] foto) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precioUnitario = precioUnitario;
        this.categoria = categoria;
        this.presentacion = presentacion;
        this.unidadMedida = unidadMedida;
        this.foto = foto;
    }
}
