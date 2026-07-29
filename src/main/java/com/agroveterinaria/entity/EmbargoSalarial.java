package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoEmbargo;
import com.agroveterinaria.enums.TipoEmbargo;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @NotNull(message = "El monto de la cuota ordinaria es obligatorio")
    @Positive(message = "La cuota ordinaria debe ser mayor que cero")
    private BigDecimal montoCuotaOrdinaria;

    @NotNull(message = "El saldo pendiente en mora no puede ser nulo")
    @PositiveOrZero(message = "El saldo pendiente en mora no puede ser un valor negativo")
    private BigDecimal saldoPendienteMora = BigDecimal.ZERO;

    @NotNull(message = "La fecha de notificación es obligatoria")
    private LocalDate fechaNotificacion;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    private EstadoEmbargo estado = EstadoEmbargo.ACTIVO;

    @NotNull(message = "El tipo de embargo es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoEmbargo tipoEmbargo;

    @OneToMany(mappedBy = "embargoSalarial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CuotaExtraEmbargo> cuotasExtras = new ArrayList<>();

    public void addCuotaExtra(CuotaExtraEmbargo cuota) {
        cuotasExtras.add(cuota);
        cuota.setEmbargoSalarial(this);
    }

    public void removeCuotaExtra(CuotaExtraEmbargo cuota) {
        cuotasExtras.remove(cuota);
        cuota.setEmbargoSalarial(null);
    }
}
