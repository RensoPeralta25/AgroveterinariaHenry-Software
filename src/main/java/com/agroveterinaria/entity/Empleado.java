package com.agroveterinaria.entity;

import com.agroveterinaria.enums.RolEmpleado;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "empleado")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Long IdEmpleado;

    @Valid
    @OneToOne
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @NotEmpty(message = "Debe asignar almenos un cargo")
    @ElementCollection(targetClass = RolEmpleado.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "empleado_rol", joinColumns = @JoinColumn(name = "id_empleado"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    private Set<RolEmpleado> cargos = new HashSet<>();

    @NotNull(message = "El salario es obligatorio")
    @PositiveOrZero(message = "El salario no puede ser negativo")
    private BigDecimal salario;

}
