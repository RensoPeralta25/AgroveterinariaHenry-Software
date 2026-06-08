package com.agroveterinaria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recepcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recepcion")
    private Long idRecepcion;

    @Column(name = "fecha_hora_llegada_programada", nullable = false)
    private LocalDateTime fechaHoraLlegadaProgramada;

    @Column(name = "fecha_hora_recepcion")
    private LocalDateTime fechaHoraRecepcion;

    // Relación con Transporte (nullable = true, gracias a nuestra migración reciente)
    // NOTA: Lo he dejado comentado. Si ya tienes tu clase Transporte creada,
    // simplemente quítale los comentarios a este bloque.
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transporte")
    private Transporte transporte;
    */

    @ManyToMany
    @JoinTable(
            name = "recepcion_compra",
            joinColumns = @JoinColumn(name = "id_recepcion"),
            inverseJoinColumns = @JoinColumn(name = "id_compra")
    )
    private List<Compra> compras = new ArrayList<>();

    @OneToMany(mappedBy = "recepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleRecepcion> detalles = new ArrayList<>();

    public void addDetalle(DetalleRecepcion detalle) {
        detalles.add(detalle);
        detalle.setRecepcion(this);
    }
}