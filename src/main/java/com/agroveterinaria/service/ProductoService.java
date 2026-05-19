package com.agroveterinaria.service;

import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadMedida;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// De prueba en lo que llega la persistencia
@Service
public class ProductoService {

    private final List<Producto> inventario = new ArrayList<>();

    private long nextId = 1L;

    public ProductoService() {
        Producto p1 = new Producto();
        p1.setIdProducto(nextId++);
        p1.setNombre("Amoxicilina 500mg");
        p1.setPrecioUnitario(new BigDecimal("350.50"));
        p1.setPresentacion(new BigDecimal("100"));
        p1.setCategoria(CategoriaProducto.MEDICAMENTO);
        p1.setUnidadMedida(UnidadMedida.UNIDAD);

        Producto p2 = new Producto();
        p2.setIdProducto(nextId++);
        p2.setNombre("Alimento Premium Perros");
        p2.setPrecioUnitario(new BigDecimal("2500.00"));
        p2.setPresentacion(new BigDecimal("15"));
        p2.setCategoria(CategoriaProducto.ALIMENTO);
        p2.setUnidadMedida(UnidadMedida.LIBRA);

        inventario.add(p1);
        inventario.add(p2);
    }

    public Collection<Producto> findAll() {
        return new ArrayList<>(inventario);
    }

    public Producto save(Producto producto) {
        if (producto.getIdProducto() == null) {
            producto.setIdProducto(nextId++);
            inventario.add(producto);
        } else {
            inventario.removeIf(p -> p.getIdProducto().equals(producto.getIdProducto()));
            inventario.add(producto);
        }
        return producto;
    }

    public void delete(Producto producto) {
        if (producto != null && producto.getIdProducto() != null) {
            inventario.removeIf(p -> p.getIdProducto().equals(producto.getIdProducto()));
        }
    }
}