package com.agroveterinaria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipo_cliente")
public class TipoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_cliente")
    private Long idTipoCliente;

    @NotBlank(message = "El nombre del tipo de cliente es obligatorio")
    @Column(name = "nombre_tipo_cliente", nullable = false, unique = true, length = 60)
    private String nombreTipoCliente;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @NotNull(message = "El descuento es obligatorio")
    @Digits(integer = 3, fraction = 2, message = "El descuento solo puede tener hasta 2 decimales")
    @Column(name = "descuento", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;
}
