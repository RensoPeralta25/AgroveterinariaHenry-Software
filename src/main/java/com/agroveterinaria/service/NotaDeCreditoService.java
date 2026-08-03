package com.agroveterinaria.service;

import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.NotaDeCredito;
import com.agroveterinaria.repository.ClienteRepository;
import com.agroveterinaria.repository.CobroRepository;
import com.agroveterinaria.repository.DevolucionVentaRepository;
import com.agroveterinaria.repository.NotaDeCreditoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotaDeCreditoService {

    private final NotaDeCreditoRepository notaRepository;
    private final ClienteRepository clienteRepository;
    private final CobroRepository cobroRepository;
    private final DevolucionVentaRepository devolucionRepository;

    public NotaDeCreditoService(
            NotaDeCreditoRepository notaRepository,
            ClienteRepository clienteRepository,
            CobroRepository cobroRepository,
            DevolucionVentaRepository devolucionRepository
    ) {
        this.notaRepository = notaRepository;
        this.clienteRepository = clienteRepository;
        this.cobroRepository = cobroRepository;
        this.devolucionRepository = devolucionRepository;
    }

    @Transactional(readOnly = true)
    public List<NotaDeCredito> listarTodas() {
        return notaRepository.findAllByOrderByFechaEmisionDesc();
    }

    @Transactional(readOnly = true)
    public List<NotaDeCredito> listarDisponibles(Long idCliente) {
        if (idCliente == null) {
            return List.of();
        }
        return notaRepository.findByClienteIdClienteAndSaldoDisponibleGreaterThanOrderByFechaEmisionAsc(
                idCliente,
                BigDecimal.ZERO
        );
    }

    @Transactional
    public NotaDeCredito crear(Long idCliente, BigDecimal monto, LocalDateTime fechaEmision, String motivo) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));
        BigDecimal montoNormalizado = validarMonto(monto);

        NotaDeCredito nota = new NotaDeCredito();
        nota.setCliente(cliente);
        nota.setMonto(montoNormalizado);
        nota.setSaldoDisponible(montoNormalizado);
        nota.setFechaEmision(fechaEmision != null ? fechaEmision : LocalDateTime.now());
        nota.setMotivo(normalizarMotivo(motivo));
        return notaRepository.save(nota);
    }

    @Transactional
    public NotaDeCredito editar(
            Long idNotaCredito,
            Long idCliente,
            BigDecimal nuevoMonto,
            LocalDateTime fechaEmision,
            String motivo
    ) {
        NotaDeCredito nota = notaRepository.buscarPorIdParaActualizar(idNotaCredito)
                .orElseThrow(() -> new IllegalArgumentException("La nota de crédito no existe."));
        BigDecimal montoNormalizado = validarMonto(nuevoMonto);
        BigDecimal utilizado = nota.getMontoUtilizado();

        if (montoNormalizado.compareTo(utilizado) < 0) {
            throw new IllegalArgumentException(
                    "El monto no puede ser menor que el crédito ya utilizado (" + utilizado + ")."
            );
        }

        boolean provieneDeDevolucion = devolucionRepository.existsByNotaDeCredito(nota);
        if (provieneDeDevolucion && montoNormalizado.compareTo(nota.getMonto()) != 0) {
            throw new IllegalStateException(
                    "El monto de una nota emitida por devolución debe coincidir con esa devolución. "
                            + "Puedes editar su fecha o motivo."
            );
        }

        if (idCliente != null && !idCliente.equals(nota.getCliente().getIdCliente())) {
            if (utilizado.compareTo(BigDecimal.ZERO) > 0 || provieneDeDevolucion) {
                throw new IllegalStateException(
                        "No se puede cambiar el cliente de una nota utilizada o vinculada a una devolución."
                );
            }
            Cliente cliente = clienteRepository.findById(idCliente)
                    .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));
            nota.setCliente(cliente);
        }

        nota.setMonto(montoNormalizado);
        nota.setSaldoDisponible(montoNormalizado.subtract(utilizado).setScale(2, RoundingMode.HALF_UP));
        nota.setFechaEmision(fechaEmision != null ? fechaEmision : nota.getFechaEmision());
        nota.setMotivo(normalizarMotivo(motivo));
        return notaRepository.save(nota);
    }

    @Transactional
    public void eliminar(Long idNotaCredito) {
        NotaDeCredito nota = notaRepository.findById(idNotaCredito)
                .orElseThrow(() -> new IllegalArgumentException("La nota de crédito no existe."));

        if (cobroRepository.existsByNotaDeCredito(nota)) {
            throw new IllegalStateException(
                    "No se puede eliminar una nota que ya fue aplicada a una venta."
            );
        }
        if (devolucionRepository.existsByNotaDeCredito(nota)) {
            throw new IllegalStateException(
                    "Esta nota fue generada por una devolución. Anula la devolución para revertir también el inventario."
            );
        }
        notaRepository.delete(nota);
    }

    @Transactional(readOnly = true)
    public boolean provieneDeDevolucion(NotaDeCredito nota) {
        return nota != null && devolucionRepository.existsByNotaDeCredito(nota);
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto de la nota debe ser mayor que cero.");
        }
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarMotivo(String motivo) {
        String valor = motivo != null ? motivo.trim() : "";
        if (valor.length() > 255) {
            throw new IllegalArgumentException("El motivo no puede exceder 255 caracteres.");
        }
        return valor.isBlank() ? null : valor;
    }
}
