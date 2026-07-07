package com.agroveterinaria.entity;

import com.agroveterinaria.enums.EstadoDevolucion;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devolucion_venta")
@Getter
@Setter
@NoArgsConstructor
public class DevolucionVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_devolucion_venta")
    private Long idDevolucionVenta;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @NotBlank(message = "Debe especificar la razón o motivo de la devolución")
    @Column(name = "razon_devolucion", nullable = false, length = 255)
    private String razonDevolucion;

    @NotNull(message = "El monto total no puede ser nulo")
    @Digits(integer = 12, fraction = 2, message = "El monto debe tener un formato financiero válido")
    @Column(name = "monto_total", nullable = false)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @NotNull(message = "El estado de la devolución es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDevolucion estado = EstadoDevolucion.COMPLETADA;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @NotNull(message = "El empleado que registra es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nota_credito")
    private NotaDeCredito notaDeCredito;

    @OneToMany(mappedBy = "devolucionVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleDevVenta> detalles = new ArrayList<>();


    public void agregarDetalle(DetalleDevVenta detalle) {
        detalles.add(detalle);
        detalle.setDevolucionVenta(this);
    }

    public void removerDetalle(DetalleDevVenta detalle) {
        detalles.remove(detalle);
        detalle.setDevolucionVenta(null);
    }
}