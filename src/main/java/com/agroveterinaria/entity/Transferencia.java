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
@Table(name = "transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia")
    private Long idTransferencia;

    @Column(name = "fecha_hora_salida_programada", nullable = false)
    private LocalDateTime fechaHoraSalidaProgramada;

    @Column(name = "fecha_hora_llegada_programada")
    private LocalDateTime fechaHoraLlegadaProgramada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transporte")
    private Transporte transporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen_origen", nullable = false)
    private Almacen almacenOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen_destino", nullable = false)
    private Almacen almacenDestino;

    @OneToMany(mappedBy = "transferencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleTransferencia> detalles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private com.agroveterinaria.enums.EstadoTransferencia estado = com.agroveterinaria.enums.EstadoTransferencia.PENDIENTE_DESPACHO;

    public void addDetalle(DetalleTransferencia detalle) {
        detalles.add(detalle);
        detalle.setTransferencia(this);
    }
}
