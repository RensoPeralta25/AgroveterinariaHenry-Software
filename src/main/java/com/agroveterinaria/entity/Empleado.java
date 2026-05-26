package com.agroveterinaria.entity;

import com.agroveterinaria.enums.RolEmpleado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
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

    @OneToOne
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @OneToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ElementCollection(targetClass = RolEmpleado.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "empleado_rol", joinColumns = @JoinColumn(name = "id_empleado"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    private Set<RolEmpleado> cargos = new HashSet<>();

    private BigDecimal salario;

}
