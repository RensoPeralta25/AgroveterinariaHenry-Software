package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoVehiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vehiculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    private Long idVehiculo;

    @NotBlank(message = "La placa es obligatoria")
    @Column(name = "placa", unique = true, length = 20)
    private String placa;

    @NotBlank(message = "La marca es obligatoria")
    @Column(name = "marca", length = 50)
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Column(name = "modelo", length = 50)
    private String modelo;

    @NotNull(message = "El año es obligatorio")
    @Column(name = "anio_fabricacion")
    private Integer anioFabricacion;

    @NotNull(message = "La capacidad de carga es obligatoria")
    @Column(name = "capacidad_carga_kg", precision = 10, scale = 2)
    private BigDecimal capacidadCargaKg;

    @NotBlank(message = "El tipo de combustible es obligatorio")
    @Column(name = "tipo_combustible", length = 30)
    private String tipoCombustible;

    @Column(name = "fecha_vencimiento_seguro")
    private LocalDate fechaVencimientoSeguro;

    @Column(name = "fecha_vencimiento_matricula")
    private LocalDate fechaVencimientoMatricula;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoVehiculo estado = EstadoVehiculo.DISPONIBLE;
}