package com.agroveterinaria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ruta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long idRuta;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "distancia_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "tiempo_estimado", nullable = false)
    private Duration tiempoEstimado;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RutaParada> paradas = new ArrayList<>();

    public void agregarParada(String direccion, int orden, Double longitud, Double latitud) {
        RutaParada nuevaParada = new RutaParada(null, this, direccion, orden, longitud, latitud);
        this.paradas.add(nuevaParada);
    }
}