package com.agroveterinaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vacacion_empleado")
public class VacacionEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de finalización es obligatoria")
    private LocalDate fechaFin;

    @Min(value = 1, message = "La cantidad de días debe ser al menos 1")
    private int cantidadDiasDescanso;

    @Min(value = 0, message = "La cantidad de días a pagar no puede ser negativa")
    @Column(name = "cantidad_dias_a_pagar")
    private int cantidadDiasAPagar;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_empleado_aprobador")
    private Empleado aprobadoPor;
    
    private boolean pagado = false;
}
