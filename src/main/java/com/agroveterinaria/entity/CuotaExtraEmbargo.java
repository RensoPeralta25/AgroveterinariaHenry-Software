package com.agroveterinaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cuota_extra_embargo")
public class CuotaExtraEmbargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El embargo salarial es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embargo_salarial_id")
    private EmbargoSalarial embargoSalarial;

    @NotNull(message = "El mes de aplicación es obligatorio")
    @Min(value = 1, message = "El mes de aplicación debe ser 1 (Enero) o mayor")
    @Max(value = 12, message = "El mes de aplicación no puede ser mayor a 12 (Diciembre)")
    private Integer mesAplicacion;

    @NotNull(message = "El monto extra es obligatorio")
    @Positive(message = "El monto extra debe ser mayor a cero")
    private BigDecimal montoExtra;

    @NotBlank(message = "El concepto de la cuota extra no puede estar vacío")
    @Size(max = 100, message = "El concepto no debe exceder los 100 caracteres")
    private String concepto;

    @NotNull(message = "El ultimo anio cobrado es obligatorio")
    private Integer ultimoAnioCobrado = 0;

}