package com.agroveterinaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "configuracion_nomina")
public class ConfiguracionNomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La clave de configuración es obligatoria")
    private String clave;

    @NotNull(message = "El valor es obligatorio")
    @PositiveOrZero(message = "El valor de configuración no puede ser negativo")
    private BigDecimal valor;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;
}
