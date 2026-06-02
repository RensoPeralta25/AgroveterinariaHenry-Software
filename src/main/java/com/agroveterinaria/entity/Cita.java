package com.agroveterinaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long idCita;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_mascota", nullable = false)
    private Mascota paciente;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_veterinario", nullable = false)
    private Empleado veterinario;

    @NotNull
    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @NotNull
    @Column(name = "realizado")
    private Boolean realizado;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_servicio", nullable = false)
    private Producto servicio;

}
