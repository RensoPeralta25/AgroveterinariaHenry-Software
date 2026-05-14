package com.agroveterinaria.entity;

import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadMedida;
import com.vaadin.pro.licensechecker.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Producto {

    private Long idProducto;
    private String nombre;
    private double precioUnitario;
    private CategoriaProducto categoria;
    private Long presentacion;
    private UnidadMedida unidadMedida;

    public Producto(){}

    public Producto(Long idProducto, String nombre, double precioUnitario, CategoriaProducto categoria, Long presentacion, UnidadMedida unidadMedida) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precioUnitario = precioUnitario;
        this.categoria = categoria;
        this.presentacion = presentacion;
        this.unidadMedida = unidadMedida;
    }
}
