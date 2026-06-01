package com.agroveterinaria.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @NotNull
    @OneToOne
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "latitud")
    private Double latitud;

    @OneToMany(mappedBy = "cliente")
    private List<Mascota> mascotas;

    @OneToMany(mappedBy = "cliente")
    private List<Cita> citas;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_tipo_cliente", nullable = false)
    private TipoCliente tipoCliente;

    @OneToMany(mappedBy = "cliente")
    private List<Venta> ventas;

    @OneToMany(mappedBy = "cliente")
    private List<Cobro> cobros;

    @OneToMany(mappedBy = "cliente")
    private List<NotaDeCredito> notasDeCredito;
}
