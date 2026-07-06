package com.agroveterinaria.view.cita;

import com.agroveterinaria.entity.Cita;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Empleado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.dom.Element;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Tag("cita-calendar")
@JsModule("./cita-calendar.js")
public class CitaCalendar extends Component {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_DURATION_MINUTES = 45;

    private final Consumer<LocalDateTime> createHandler;
    private final Consumer<Long> editHandler;
    private final BiFunction<Long, LocalDateTime, Boolean> moveHandler;
    private final Element appointmentsData = new Element("script");
    private String lastEventsJson = "[]";

    public CitaCalendar(
            Consumer<LocalDateTime> createHandler,
            Consumer<Long> editHandler,
            BiFunction<Long, LocalDateTime, Boolean> moveHandler
    ) {
        this.createHandler = createHandler;
        this.editHandler = editHandler;
        this.moveHandler = moveHandler;

        addClassName("cita-calendar");
        getElement().getStyle().set("display", "block");
        appointmentsData.setAttribute("type", "application/json");
        appointmentsData.setAttribute("data-cita-appointments", "");
        appointmentsData.setText(lastEventsJson);
        getElement().appendChild(appointmentsData);
        addAttachListener(event -> pushAppointmentsToClient());
    }

    public void setCitas(List<Cita> citas) {
        lastEventsJson = toJson(citas.stream()
                .filter(cita -> cita.getIdCita() != null && cita.getFechaHora() != null)
                .map(this::toCalendarEvent)
                .toList());

        pushAppointmentsToClient();
    }

    private void pushAppointmentsToClient() {
        appointmentsData.setText(lastEventsJson);
        getElement().setProperty("appointments", lastEventsJson);
        getElement().executeJs("this.loadAppointmentsFromDom ? this.loadAppointmentsFromDom() : this.appointments = $0", lastEventsJson);
    }

    @ClientCallable
    public void createAppointment(String startIso) {
        createHandler.accept(parseCalendarDateTime(startIso));
    }

    @ClientCallable
    public void editAppointment(Double idCita) {
        if (idCita != null) {
            editHandler.accept(idCita.longValue());
        }
    }

    @ClientCallable
    public boolean moveAppointment(Double idCita, String startIso) {
        if (idCita == null) {
            return false;
        }

        return moveHandler.apply(idCita.longValue(), parseCalendarDateTime(startIso));
    }

    private Map<String, Object> toCalendarEvent(Cita cita) {
        LocalDateTime inicio = cita.getFechaHora();
        boolean realizado = Boolean.TRUE.equals(cita.getRealizado());
        String cliente = nombreCliente(cita.getCliente());
        String paciente = cita.getPaciente() != null ? cita.getPaciente().getNombre() : "";
        String veterinario = nombreEmpleado(cita.getVeterinario());
        String servicio = cita.getServicio() != null ? cita.getServicio().getNombre() : "";
        String titulo = List.of(paciente, servicio).stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .reduce((a, b) -> a + " - " + b)
                .orElse("Cita");

        Map<String, Object> extendedProps = new LinkedHashMap<>();
        extendedProps.put("realizado", realizado);
        extendedProps.put("tooltip", String.format("%s%nCliente: %s%nVeterinario: %s%nEstado: %s",
                titulo,
                cliente,
                veterinario,
                realizado ? "Realizada" : "Pendiente"));

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", cita.getIdCita().toString());
        event.put("title", titulo);
        event.put("start", inicio.toString());
        event.put("end", inicio.plusMinutes(DEFAULT_DURATION_MINUTES).toString());
        event.put("extendedProps", extendedProps);
        return event;
    }

    private String nombreCliente(Cliente cliente) {
        return cliente != null && cliente.getPersona() != null ? cliente.getPersona().getNombre() : "";
    }

    private String nombreEmpleado(Empleado empleado) {
        return empleado != null && empleado.getPersona() != null ? empleado.getPersona().getNombre() : "";
    }

    private LocalDateTime parseCalendarDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
        }

        String normalized = value.length() == 10 ? value + "T09:00:00" : value;
        int offsetIndex = Math.max(normalized.lastIndexOf('+'), normalized.lastIndexOf('-'));
        if (offsetIndex > normalized.indexOf('T')) {
            normalized = normalized.substring(0, offsetIndex);
        }

        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return LocalDateTime.parse(normalized).withSecond(0).withNano(0);
    }

    private String toJson(List<Map<String, Object>> events) {
        try {
            return OBJECT_MAPPER.writeValueAsString(events);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo preparar el calendario de citas", exception);
        }
    }
}
