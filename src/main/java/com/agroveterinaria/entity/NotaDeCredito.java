package com.agroveterinaria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nota_credito")
public class NotaDeCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_credito")
    private Long idNotaCredito;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @NotNull
    @Digits(integer = 12, fraction = 2, message = "El monto solo puede tener hasta 2 decimales")
    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @NotNull
    @Digits(integer = 12, fraction = 2, message = "El saldo solo puede tener hasta 2 decimales")
    @Column(name = "saldo_disponible", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoDisponible;

    @NotNull
    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "motivo", length = 255)
    private String motivo;

    public BigDecimal getMontoUtilizado() {
        BigDecimal montoSeguro = monto != null ? monto : BigDecimal.ZERO;
        BigDecimal saldoSeguro = saldoDisponible != null ? saldoDisponible : BigDecimal.ZERO;
        return montoSeguro.subtract(saldoSeguro).max(BigDecimal.ZERO);
    }
}
