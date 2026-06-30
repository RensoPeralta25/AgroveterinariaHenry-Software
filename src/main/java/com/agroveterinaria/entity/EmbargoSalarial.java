package com.agroveterinaria.entity;

import com.agroveterinaria.enums.TipoEmbargo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "embargo_salarial")
public class EmbargoSalarial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEmbargo;

    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @NotBlank(message = "La entidad demandante es obligatoria")
    @Size(max = 255, message = "El nombre de la entidad no puede exceder los 255 caracteres")
    private String entidadDemandante;

    @NotNull(message = "El monto de descuento es obligatorio")
    @Positive(message = "El monto del embargo debe ser mayor a cero")
    private BigDecimal montoDescuento;

    @NotNull(message = "La fecha de notificación es obligatoria")
    private LocalDate fechaNotificacion;

    private boolean activo = true;

    @Version
    private Long version;

    @NotNull(message = "El tipo de embargo es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoEmbargo tipo;
}
