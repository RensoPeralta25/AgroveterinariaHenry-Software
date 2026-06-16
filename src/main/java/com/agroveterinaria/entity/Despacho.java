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
@Table(name = "despacho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despacho")
    private Long idDespacho;

    @Column(name = "fecha_hora_salida_programada", nullable = false)
    private LocalDateTime fechaHoraSalidaProgramada;

    @Column(name = "fecha_hora_entrega")
    private LocalDateTime fechaHoraEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transporte", nullable = false)
    private Transporte transporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transferencia")
    private Transferencia transferencia;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "despacho_venta",
            joinColumns = @JoinColumn(name = "id_despacho"),
            inverseJoinColumns = @JoinColumn(name = "id_venta")
    )
    private List<Venta> ventas = new ArrayList<>();

    @OneToMany(mappedBy = "despacho", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleDespacho> detalles = new ArrayList<>();

    public void addDetalle(DetalleDespacho detalle) {
        detalles.add(detalle);
        detalle.setDespacho(this);
    }
}