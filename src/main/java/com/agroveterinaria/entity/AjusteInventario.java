package com.agroveterinaria.entity;

import com.agroveterinaria.enums.TipoAjuste;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ajuste_inventario")
public class AjusteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ajuste")
    private Long idAjuste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen", nullable = false)
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ajuste", nullable = false)
    private TipoAjuste tipoAjuste;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(nullable = false)
    private String justificacion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}