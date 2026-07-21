package com.agroveterinaria.entity;


import com.agroveterinaria.enums.TipoConcepto;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "detalle_nomina")
public class DetalleNomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalleNomina;

    @NotNull(message = "La nómina asociada es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nomina", nullable = false)
    private Nomina nomina;

    @NotBlank(message = "La descripción del concepto es obligatoria")
    private String descripcion;

    @NotNull(message = "El tipo de concepto es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoConcepto tipo;

    @NotNull(message = "El monto es obligatorio")
    @PositiveOrZero(message = "El monto no puede ser negativo")
    private BigDecimal monto;

    @NotNull(message = "La cantidad es obligatoria")
    @PositiveOrZero(message = "La cantidad no puede ser negativa")
    private BigDecimal cantidad;

}
