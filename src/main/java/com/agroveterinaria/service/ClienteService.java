package com.agroveterinaria.service;

import com.agroveterinaria.dto.cliente.ClienteDetalleDTO;
import com.agroveterinaria.dto.cliente.ClienteResumenDTO;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Cita;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.TipoCliente;
import com.agroveterinaria.repository.ClienteRepository;
import com.agroveterinaria.repository.TipoClienteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final TipoClienteRepository tipoClienteRepository;
    private final PersonaService personaService;

    public ClienteService (
            ClienteRepository clienteRepository,
            TipoClienteRepository tipoClienteRepository,
            PersonaService personaService
    ) {
        this.clienteRepository = clienteRepository;
        this.tipoClienteRepository = tipoClienteRepository;
        this.personaService = personaService;
    }

    public List<Cliente> findAll() { return clienteRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<ClienteResumenDTO> findResumen() {
        return clienteRepository.findAll().stream()
                .map(this::toResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteDetalleDTO findDetalle(Long idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        ClienteResumenDTO resumen = toResumen(cliente);

        return new ClienteDetalleDTO(
                cliente.getIdCliente(),
                getNombre(cliente),
                getCedula(cliente),
                getTelefono(cliente),
                getDireccion(cliente),
                getTipoClienteNombre(cliente),
                cliente.getLongitud(),
                cliente.getLatitud(),
                resumen.cantidadMascotas(),
                resumen.citasPendientes(),
                resumen.totalVendido(),
                resumen.balancePendiente(),
                resumen.totalNotasCredito(),
                cliente.getMascotas().stream()
                        .map(mascota -> new ClienteDetalleDTO.MascotaResumenDTO(
                                mascota.getNombre(),
                                mascota.getTipoAnimal() != null ? mascota.getTipoAnimal().name() : "",
                                mascota.getRaza(),
                                mascota.getSexo(),
                                mascota.getFechaNacimiento()
                        ))
                        .toList(),
                cliente.getCitas().stream()
                        .sorted(Comparator.comparing(Cita::getFechaHora, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .map(cita -> new ClienteDetalleDTO.CitaResumenDTO(
                                cita.getFechaHora(),
                                cita.getPaciente() != null ? cita.getPaciente().getNombre() : "",
                                cita.getVeterinario() != null && cita.getVeterinario().getPersona() != null
                                        ? cita.getVeterinario().getPersona().getNombre()
                                        : "",
                                cita.getServicio() != null ? cita.getServicio().getNombre() : "",
                                cita.getRealizado()
                        ))
                        .toList(),
                cliente.getVentas().stream()
                        .map(venta -> new ClienteDetalleDTO.VentaResumenDTO(
                                venta.getFechaHoraVenta(),
                                venta.getVendedor() != null && venta.getVendedor().getPersona() != null
                                        ? venta.getVendedor().getPersona().getNombre()
                                        : "",
                                venta.getEstado() != null ? venta.getEstado().getEtiqueta() : "",
                                valueOrZero(venta.getMontoTotal())
                        ))
                        .toList(),
                cliente.getCobros().stream()
                        .map(cobro -> new ClienteDetalleDTO.CobroResumenDTO(
                                cobro.getIdCobro(),
                                valueOrZero(cobro.getMontoTotal()),
                                cobro.getMetodoPago() != null ? cobro.getMetodoPago().getEtiqueta() : ""
                        ))
                        .toList(),
                cliente.getNotasDeCredito().stream()
                        .map(nota -> new ClienteDetalleDTO.NotaCreditoResumenDTO(
                                nota.getIdNotaCredito(),
                                valueOrZero(nota.getMonto())
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> findById(Long idCliente) {
        return clienteRepository.findById(idCliente);
    }

    @Transactional(readOnly = true)
    public List<TipoCliente> findTiposCliente() {
        return tipoClienteRepository.findAll(Sort.by("nombreTipoCliente"));
    }

    public Cliente save(Cliente cliente){
        validar(cliente);

        Persona personaForm = cliente.getPersona();
        if (personaForm != null && personaForm.getCedula() != null) {
            personaService
                    .findByCedula(personaForm.getCedula())
                    .ifPresentOrElse(
                            cliente::setPersona,
                            () -> {
                                Persona personaGuardada = personaService.save(personaForm);
                                cliente.setPersona(personaGuardada);
                            }
                    );
        }
        return clienteRepository.save(cliente);
    }

    public Cliente update(Cliente cliente){
        validar(cliente);

        Cliente clienteExistente = clienteRepository.findById(cliente.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Persona personaExistente = clienteExistente.getPersona();
        Persona personaForm = cliente.getPersona();

        if (personaExistente != null && personaForm != null) {
            personaExistente.setNombre(personaForm.getNombre());
            personaExistente.setTelefono(personaForm.getTelefono());
            personaExistente.setDireccion(personaForm.getDireccion());
            personaService.save(personaExistente);
            cliente.setPersona(personaExistente);
        }

        clienteExistente.setPersona(cliente.getPersona());
        clienteExistente.setTipoCliente(cliente.getTipoCliente());
        clienteExistente.setLongitud(cliente.getLongitud());
        clienteExistente.setLatitud(cliente.getLatitud());

        return clienteRepository.save(clienteExistente);
    }

    public void delete(Cliente cliente){
        clienteRepository.delete(cliente);
    }

    public void deleteById(Long idCliente){
        clienteRepository.deleteById(idCliente);
    }

    public void validar(Cliente cliente){
        if (cliente.getPersona() != null && cliente.getPersona().getCedula() != null) {
            String cedula = cliente.getPersona().getCedula();
            Optional<Cliente> clienteExistente = clienteRepository.findByPersonaCedula(cedula);

            if (clienteExistente.isPresent()) {
                if (cliente.getIdCliente() == null) {
                    throw new IllegalArgumentException("Error: Ya existe un cliente registrado con la cédula " + cedula);
                }

                else if (!clienteExistente.get().getIdCliente().equals(cliente.getIdCliente())) {
                    throw new IllegalArgumentException("Error: La cédula " + cedula + " ya le pertenece a otro cliente.");
                }
            }
        }

    }

    private ClienteResumenDTO toResumen(Cliente cliente) {
        int citasPendientes = (int) cliente.getCitas().stream()
                .filter(cita -> !Boolean.TRUE.equals(cita.getRealizado()))
                .count();

        LocalDateTime proximaCita = cliente.getCitas().stream()
                .filter(cita -> !Boolean.TRUE.equals(cita.getRealizado()))
                .map(Cita::getFechaHora)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        BigDecimal totalVendido = cliente.getVentas().stream()
                .map(venta -> valueOrZero(venta.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCobrado = cliente.getCobros().stream()
                .map(cobro -> valueOrZero(cobro.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNotasCredito = cliente.getNotasDeCredito().stream()
                .map(nota -> valueOrZero(nota.getMonto()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balancePendiente = totalVendido.subtract(totalCobrado).subtract(totalNotasCredito);

        return new ClienteResumenDTO(
                cliente.getIdCliente(),
                getNombre(cliente),
                getCedula(cliente),
                getTelefono(cliente),
                getDireccion(cliente),
                getTipoClienteNombre(cliente),
                cliente.getMascotas().size(),
                citasPendientes,
                proximaCita,
                cliente.getVentas().size(),
                totalVendido,
                balancePendiente,
                totalNotasCredito
        );
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String getNombre(Cliente cliente) {
        return cliente.getPersona() != null ? cliente.getPersona().getNombre() : "";
    }

    private String getCedula(Cliente cliente) {
        return cliente.getPersona() != null ? cliente.getPersona().getCedula() : "";
    }

    private String getTelefono(Cliente cliente) {
        return cliente.getPersona() != null ? cliente.getPersona().getTelefono() : "";
    }

    private String getDireccion(Cliente cliente) {
        return cliente.getPersona() != null ? cliente.getPersona().getDireccion() : "";
    }

    private String getTipoClienteNombre(Cliente cliente) {
        return cliente.getTipoCliente() != null ? cliente.getTipoCliente().getNombreTipoCliente() : "";
    }
}
