package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoRegistro;
import com.agroveterinaria.enums.TipoAusencia;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "ausencia")
public class Ausencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ausencia")
    private Long id;

    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @NotNull(message = "El tipo de ausencia es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoAusencia tipoAusencia;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estadoRegistro = EstadoRegistro.CERRADA;

    @Size(max = 255, message = "El nombre del archivo es de maximo ")
    private String nombreArchivo;

    @JdbcTypeCode(Types.BINARY)
    private byte[] documentoAdjunto;

    private boolean aplicadaEnNomina = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nomina_aplicada")
    private Nomina nominaAplicada;

    @NotNull(message = "La fecha de registro es obligatoria")
    private LocalDateTime fechaRegistro;

    @NotNull(message = "Los dias descontados acumulados son obligatorios")
    private Integer diasDescontadosAcumulados = 0;

    @Transient
    private Integer diasADescontarEnEstaCorrida = 0;

}