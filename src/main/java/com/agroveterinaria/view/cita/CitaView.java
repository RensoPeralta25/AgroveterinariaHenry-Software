package com.agroveterinaria.view.cita;

import com.agroveterinaria.entity.Cita;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Mascota;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.CitaService;
import com.agroveterinaria.service.ClienteService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.ProductoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Gestión de Citas | Agroveterinaria")
public class CitaView extends VerticalLayout {

    private final CitaService citaService;
    private final ClienteService clienteService;
    private final EmpleadoService empleadoService;
    private final ProductoService productoService;
    private final Grid<Cita> grid = new Grid<>(Cita.class, false);
    private final TextField buscar = new TextField();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CitaView(
            CitaService citaService,
            ClienteService clienteService,
            EmpleadoService empleadoService,
            ProductoService productoService
    ) {
        this.citaService = citaService;
        this.clienteService = clienteService;
        this.empleadoService = empleadoService;
        this.productoService = productoService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("cita-view");

        configurarGrid();

        Button btnNueva = new Button("Nueva cita", new Icon(VaadinIcon.PLUS));
        btnNueva.addClassName("btn-nuevo");
        btnNueva.addClickListener(event -> abrirDialogoCita(null));

        buscar.setPlaceholder("Buscar cita...");
        buscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscar.setClearButtonVisible(true);
        buscar.setValueChangeMode(ValueChangeMode.LAZY);
        buscar.addValueChangeListener(event -> refrescarGrid());

        HorizontalLayout toolbar = new HorizontalLayout(btnNueva, buscar);
        toolbar.addClassName("usuario-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.expand(buscar);

        add(toolbar, grid);
        expand(grid);

        refrescarGrid();
    }

    private void configurarGrid() {
        grid.addClassName("usuario-grid");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addColumn(cita -> formatDateTime(cita.getFechaHora()))
                .setHeader("Fecha y hora")
                .setAutoWidth(true)
                .setSortable(true);
        grid.addColumn(cita -> nombreCliente(cita.getCliente()))
                .setHeader("Cliente")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(cita -> cita.getPaciente() != null ? cita.getPaciente().getNombre() : "")
                .setHeader("Mascota")
                .setAutoWidth(true);
        grid.addColumn(cita -> nombreEmpleado(cita.getVeterinario()))
                .setHeader("Veterinario")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(cita -> cita.getServicio() != null ? cita.getServicio().getNombre() : "")
                .setHeader("Servicio")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addComponentColumn(this::crearEstado)
                .setHeader("Estado")
                .setAutoWidth(true);
        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones")
                .setWidth("150px")
                .setFlexGrow(0);
    }

    private Component crearEstado(Cita cita) {
        Span badge = new Span(Boolean.TRUE.equals(cita.getRealizado()) ? "Realizada" : "Pendiente");
        badge.getElement().getThemeList().add("badge " + (Boolean.TRUE.equals(cita.getRealizado()) ? "success" : "contrast"));
        return badge;
    }

    private Component crearAcciones(Cita cita) {
        Button editar = new Button(new Icon(VaadinIcon.PENCIL));
        editar.addClassName("btn-accion-editar");
        editar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editar.setAriaLabel("Editar cita");
        editar.addClickListener(event -> abrirDialogoCita(cita));

        Button cambiarEstado = new Button(new Icon(Boolean.TRUE.equals(cita.getRealizado()) ? VaadinIcon.CLOCK : VaadinIcon.CHECK));
        cambiarEstado.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cambiarEstado.setAriaLabel(Boolean.TRUE.equals(cita.getRealizado()) ? "Marcar pendiente" : "Marcar realizada");
        cambiarEstado.addClickListener(event -> {
            try {
                if (Boolean.TRUE.equals(cita.getRealizado())) {
                    citaService.marcarComoPendiente(cita.getIdCita());
                    mostrarExito("Cita marcada como pendiente");
                } else {
                    citaService.marcarComoRealizada(cita.getIdCita());
                    mostrarExito("Cita marcada como realizada");
                }
                refrescarGrid();
            } catch (IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
            }
        });

        Button eliminar = new Button(new Icon(VaadinIcon.TRASH));
        eliminar.addClassName("btn-accion-eliminar");
        eliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        eliminar.setAriaLabel("Eliminar cita");
        eliminar.addClickListener(event -> confirmarEliminacion(cita));

        HorizontalLayout acciones = new HorizontalLayout(editar, cambiarEstado, eliminar);
        acciones.setPadding(false);
        acciones.setSpacing(false);
        return acciones;
    }

    private void abrirDialogoCita(Cita cita) {
        boolean editando = cita != null && cita.getIdCita() != null;
        Cita citaForm = editando ? cita : new Cita();

        Dialog dialog = new Dialog();
        dialog.setWidth("760px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3(editando ? "Editar cita" : "Nueva cita");
        titulo.addClassName("cliente-dialog-title");

        ComboBox<Cliente> cliente = new ComboBox<>("Cliente");
        cliente.setItems(clienteService.findAll());
        cliente.setItemLabelGenerator(this::nombreCliente);
        cliente.setValue(citaForm.getCliente());
        cliente.setRequiredIndicatorVisible(true);

        ComboBox<Mascota> mascota = new ComboBox<>("Mascota");
        mascota.setItemLabelGenerator(Mascota::getNombre);
        mascota.setRequiredIndicatorVisible(true);

        ComboBox<Empleado> veterinario = new ComboBox<>("Veterinario");
        veterinario.setItems(empleadoService.findAll().stream()
                .filter(empleado -> empleado.getCargos() != null && empleado.getCargos().contains(RolEmpleado.VETERINARIO))
                .toList());
        veterinario.setItemLabelGenerator(this::nombreEmpleado);
        veterinario.setValue(citaForm.getVeterinario());
        veterinario.setRequiredIndicatorVisible(true);

        ComboBox<Producto> servicio = new ComboBox<>("Servicio");
        servicio.setItems(productoService.listarTodos().stream()
                .filter(producto -> producto.getCategoria() == CategoriaProducto.SERVICIO)
                .toList());
        servicio.setItemLabelGenerator(Producto::getNombre);
        servicio.setValue(citaForm.getServicio());
        servicio.setRequiredIndicatorVisible(true);

        DateTimePicker fechaHora = new DateTimePicker("Fecha y hora");
        fechaHora.setValue(citaForm.getFechaHora() != null ? citaForm.getFechaHora() : LocalDateTime.now().plusHours(1));
        fechaHora.setRequiredIndicatorVisible(true);

        Checkbox realizada = new Checkbox("Cita realizada");
        realizada.setValue(Boolean.TRUE.equals(citaForm.getRealizado()));

        Runnable cargarMascotas = () -> {
            Cliente seleccionado = cliente.getValue();
            List<Mascota> mascotas = seleccionado != null
                    ? citaService.findMascotasByCliente(seleccionado.getIdCliente())
                    : List.of();
            mascota.setItems(mascotas);

            if (citaForm.getPaciente() != null && mascotas.stream()
                    .anyMatch(item -> item.getIdMascota().equals(citaForm.getPaciente().getIdMascota()))) {
                mascota.setValue(citaForm.getPaciente());
            } else {
                mascota.clear();
            }
        };

        cliente.addValueChangeListener(event -> cargarMascotas.run());
        cargarMascotas.run();

        HorizontalLayout filaCliente = new HorizontalLayout(cliente, mascota);
        filaCliente.setWidthFull();
        filaCliente.setFlexGrow(1, cliente, mascota);

        HorizontalLayout filaAsignacion = new HorizontalLayout(veterinario, servicio);
        filaAsignacion.setWidthFull();
        filaAsignacion.setFlexGrow(1, veterinario, servicio);

        HorizontalLayout filaFecha = new HorizontalLayout(fechaHora, realizada);
        filaFecha.setWidthFull();
        filaFecha.setAlignItems(FlexComponent.Alignment.CENTER);
        filaFecha.setFlexGrow(1, fechaHora);

        Button guardar = new Button(editando ? "Guardar cambios" : "Crear cita", new Icon(VaadinIcon.CHECK));
        guardar.addClassName("btn-nuevo");
        guardar.addClickListener(event -> {
            if (cliente.isEmpty() || mascota.isEmpty() || veterinario.isEmpty() || servicio.isEmpty() || fechaHora.isEmpty()) {
                mostrarError("Completa cliente, mascota, veterinario, servicio y fecha/hora.");
                return;
            }

            citaForm.setCliente(cliente.getValue());
            citaForm.setPaciente(mascota.getValue());
            citaForm.setVeterinario(veterinario.getValue());
            citaForm.setServicio(servicio.getValue());
            citaForm.setFechaHora(fechaHora.getValue());
            citaForm.setRealizado(realizada.getValue());

            try {
                citaService.save(citaForm);
                dialog.close();
                refrescarGrid();
                mostrarExito(editando ? "Cita actualizada" : "Cita creada");
            } catch (IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
            } catch (RuntimeException ex) {
                mostrarError("No se pudo guardar la cita. Revisa los datos seleccionados.");
            }
        });

        Button cancelar = new Button("Cancelar", event -> dialog.close());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout acciones = new HorizontalLayout(cancelar, guardar);
        acciones.addClassName("cliente-dialog-actions");
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.setWidthFull();

        VerticalLayout contenido = new VerticalLayout(titulo, filaCliente, filaAsignacion, filaFecha, acciones);
        contenido.setPadding(false);
        contenido.setSpacing(true);
        dialog.add(contenido);
        dialog.open();
    }

    private void confirmarEliminacion(Cita cita) {
        Dialog dialog = new Dialog();
        dialog.setWidth("420px");

        H3 titulo = new H3("Eliminar cita");
        titulo.addClassName("cliente-dialog-title");

        Span mensaje = new Span("¿Seguro que deseas eliminar esta cita?");

        Button cancelar = new Button("Cancelar", event -> dialog.close());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button eliminar = new Button("Eliminar", new Icon(VaadinIcon.TRASH), event -> {
            try {
                citaService.delete(cita);
                dialog.close();
                refrescarGrid();
                mostrarExito("Cita eliminada");
            } catch (RuntimeException ex) {
                mostrarError("No se pudo eliminar la cita.");
            }
        });
        eliminar.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout acciones = new HorizontalLayout(cancelar, eliminar);
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.setWidthFull();

        VerticalLayout contenido = new VerticalLayout(titulo, mensaje, acciones);
        contenido.setPadding(false);
        dialog.add(contenido);
        dialog.open();
    }

    private void refrescarGrid() {
        String termino = buscar.getValue() != null ? buscar.getValue().trim().toLowerCase() : "";

        grid.setItems(citaService.findAll().stream()
                .filter(cita -> termino.isBlank()
                        || nombreCliente(cita.getCliente()).toLowerCase().contains(termino)
                        || nombreEmpleado(cita.getVeterinario()).toLowerCase().contains(termino)
                        || (cita.getPaciente() != null && cita.getPaciente().getNombre() != null
                        && cita.getPaciente().getNombre().toLowerCase().contains(termino))
                        || (cita.getServicio() != null && cita.getServicio().getNombre() != null
                        && cita.getServicio().getNombre().toLowerCase().contains(termino)))
                .toList());
    }

    private String nombreCliente(Cliente cliente) {
        return cliente != null && cliente.getPersona() != null ? cliente.getPersona().getNombre() : "";
    }

    private String nombreEmpleado(Empleado empleado) {
        return empleado != null && empleado.getPersona() != null ? empleado.getPersona().getNombre() : "";
    }

    private String formatDateTime(LocalDateTime fechaHora) {
        return fechaHora != null ? DATE_TIME_FORMATTER.format(fechaHora) : "";
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 3500, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 2500, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
