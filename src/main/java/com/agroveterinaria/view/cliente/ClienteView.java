package com.agroveterinaria.view.cliente;

import com.agroveterinaria.dto.cliente.ClienteDetalleDTO;
import com.agroveterinaria.dto.cliente.ClienteResumenDTO;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.TipoCliente;
import com.agroveterinaria.service.ClienteService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@PageTitle("Gestión de Clientes | Agroveterinaria")
public class ClienteView extends VerticalLayout {

    private final ClienteService clienteService;
    private final Grid<ClienteResumenDTO> gridClientes = new Grid<>(ClienteResumenDTO.class, false);
    private final TextField buscarCliente = new TextField();
    private final ComboBox<String> filtroTipoCliente = new ComboBox<>();
    private final VerticalLayout panelDetalle = new VerticalLayout();
    private final Div contenidoTab = new Div();
    private final Tab resumenTab = new Tab("Resumen");
    private final Tab mascotasTab = new Tab("Mascotas");
    private final Tab citasTab = new Tab("Citas");
    private final Tab ventasTab = new Tab("Ventas");
    private final Tab cobrosTab = new Tab("Cobros");
    private final Tab notasTab = new Tab("Notas crédito");

    private ClienteDetalleDTO detalleActual;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "DO"));
    private static final String CEDULA_REGEX = "^\\d{3}-\\d{7}-\\d{1}$";
    private static final String TELEFONO_REGEX = "^\\d{3}-\\d{3}-\\d{4}$";

    public ClienteView(ClienteService clienteService) {
        this.clienteService = clienteService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("cliente-view");

        configurarGrid();
        configurarFiltros();
        configurarDetalleVacio();

        VerticalLayout listado = crearPanelListado();
        panelDetalle.addClassName("cliente-detail-panel");
        panelDetalle.setPadding(false);
        panelDetalle.setSpacing(false);

        HorizontalLayout workspace = new HorizontalLayout(listado, panelDetalle);
        workspace.addClassName("cliente-workspace");
        workspace.setSizeFull();
        workspace.setPadding(false);
        workspace.setSpacing(false);
        workspace.expand(listado);

        add(workspace);
        expand(workspace);

        refrescarGrid();
    }

    private VerticalLayout crearPanelListado() {
        Button btnNuevo = new Button("Nuevo cliente", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.addClickListener(event -> abrirDialogoCliente(null));

        HorizontalLayout toolbar = new HorizontalLayout(buscarCliente, filtroTipoCliente, btnNuevo);
        toolbar.addClassName("cliente-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.expand(buscarCliente);

        VerticalLayout listado = new VerticalLayout(toolbar, gridClientes);
        listado.addClassName("cliente-list-panel");
        listado.setSizeFull();
        listado.setPadding(false);
        listado.setSpacing(false);
        listado.expand(gridClientes);
        return listado;
    }

    private void configurarFiltros() {
        buscarCliente.setPlaceholder("Buscar cliente...");
        buscarCliente.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarCliente.setClearButtonVisible(true);
        buscarCliente.setValueChangeMode(ValueChangeMode.LAZY);
        buscarCliente.addValueChangeListener(event -> refrescarGrid());

        filtroTipoCliente.setPlaceholder("Tipo de cliente");
        filtroTipoCliente.setClearButtonVisible(true);
        filtroTipoCliente.setItems(clienteService.findTiposCliente().stream()
                .map(TipoCliente::getNombreTipoCliente)
                .toList());
        filtroTipoCliente.addValueChangeListener(event -> refrescarGrid());
    }

    private void configurarGrid() {
        gridClientes.addClassName("cliente-grid");
        gridClientes.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        gridClientes.setSizeFull();

        gridClientes.addComponentColumn(this::crearCeldaCliente)
                .setHeader("Cliente")
                .setAutoWidth(true)
                .setFlexGrow(1);
        gridClientes.addColumn(ClienteResumenDTO::tipoCliente)
                .setHeader("Tipo")
                .setAutoWidth(true);
        gridClientes.addColumn(ClienteResumenDTO::telefono)
                .setHeader("Teléfono")
                .setAutoWidth(true);
        gridClientes.addColumn(ClienteResumenDTO::cantidadMascotas)
                .setHeader("Mascotas")
                .setAutoWidth(true);
        gridClientes.addColumn(cliente -> formatDateTime(cliente.proximaCita()))
                .setHeader("Próxima cita")
                .setAutoWidth(true);
        gridClientes.addColumn(cliente -> cliente.cantidadVentas() + " / " + formatMoney(cliente.totalVendido()))
                .setHeader("Ventas")
                .setAutoWidth(true);
        gridClientes.addColumn(cliente -> formatMoney(cliente.balancePendiente()))
                .setHeader("Balance")
                .setAutoWidth(true);
        gridClientes.addColumn(cliente -> formatMoney(cliente.totalNotasCredito()))
                .setHeader("Notas")
                .setAutoWidth(true);
        gridClientes.addComponentColumn(this::crearAccionesCliente)
                .setHeader("Acciones")
                .setWidth("120px")
                .setFlexGrow(0);

        gridClientes.asSingleSelect().addValueChangeListener(event -> {
            ClienteResumenDTO cliente = event.getValue();
            if (cliente != null) {
                mostrarDetalle(cliente.idCliente());
            }
        });
    }

    private Component crearCeldaCliente(ClienteResumenDTO cliente) {
        Div iniciales = new Div();
        iniciales.addClassName("cliente-avatar");
        iniciales.setText(iniciales(cliente.nombre()));

        Span nombre = new Span(cliente.nombre());
        nombre.addClassName("cliente-cell-title");

        Span cedula = new Span(cliente.cedula());
        cedula.addClassName("cliente-cell-subtitle");

        VerticalLayout textos = new VerticalLayout(nombre, cedula);
        textos.setPadding(false);
        textos.setSpacing(false);

        HorizontalLayout layout = new HorizontalLayout(iniciales, textos);
        layout.addClassName("cliente-cell");
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        return layout;
    }

    private Component crearAccionesCliente(ClienteResumenDTO cliente) {
        Button editar = new Button(new Icon(VaadinIcon.PENCIL));
        editar.addClassName("btn-accion-editar");
        editar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editar.setAriaLabel("Editar cliente");
        editar.addClickListener(event -> clienteService.findById(cliente.idCliente())
                .ifPresent(this::abrirDialogoCliente));

        Button eliminar = new Button(new Icon(VaadinIcon.TRASH));
        eliminar.addClassName("btn-accion-eliminar");
        eliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        eliminar.setAriaLabel("Eliminar cliente");
        eliminar.addClickListener(event -> confirmarEliminacion(cliente));

        HorizontalLayout acciones = new HorizontalLayout(editar, eliminar);
        acciones.setPadding(false);
        acciones.setSpacing(false);
        return acciones;
    }

    private void refrescarGrid() {
        String termino = buscarCliente.getValue() != null ? buscarCliente.getValue().trim().toLowerCase() : "";
        String tipo = filtroTipoCliente.getValue();

        List<ClienteResumenDTO> clientes = clienteService.findResumen().stream()
                .filter(cliente -> termino.isBlank()
                        || contains(cliente.nombre(), termino)
                        || contains(cliente.cedula(), termino)
                        || contains(cliente.telefono(), termino))
                .filter(cliente -> tipo == null || tipo.equals(cliente.tipoCliente()))
                .toList();

        gridClientes.setItems(clientes);

        if (!clientes.isEmpty() && gridClientes.asSingleSelect().getValue() == null) {
            gridClientes.select(clientes.get(0));
        } else if (clientes.isEmpty()) {
            configurarDetalleVacio();
        }
    }

    private void mostrarDetalle(Long idCliente) {
        detalleActual = clienteService.findDetalle(idCliente);

        panelDetalle.removeAll();
        panelDetalle.add(crearEncabezadoDetalle(), crearMetricasDetalle(), crearTabsDetalle());
        renderizarTab(resumenTab);
    }

    private Component crearEncabezadoDetalle() {
        Div avatar = new Div();
        avatar.addClassName("cliente-detail-avatar");
        avatar.setText(iniciales(detalleActual.nombre()));

        Span nombre = new Span(detalleActual.nombre());
        nombre.addClassName("cliente-detail-name");

        Span datos = new Span(detalleActual.cedula() + " · " + detalleActual.telefono());
        datos.addClassName("cliente-detail-subtitle");

        VerticalLayout textos = new VerticalLayout(nombre, datos);
        textos.setPadding(false);
        textos.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(avatar, textos);
        header.addClassName("cliente-detail-header");
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private Component crearMetricasDetalle() {
        HorizontalLayout metricas = new HorizontalLayout(
                crearMetrica("Mascotas", String.valueOf(detalleActual.cantidadMascotas())),
                crearMetrica("Citas pendientes", String.valueOf(detalleActual.citasPendientes())),
                crearMetrica("Vendido", formatMoney(detalleActual.totalVendido())),
                crearMetrica("Notas crédito", formatMoney(detalleActual.totalNotasCredito()))
        );
        metricas.addClassName("cliente-metrics");
        metricas.setWidthFull();
        return metricas;
    }

    private Component crearTabsDetalle() {
        Tabs tabs = new Tabs(resumenTab, mascotasTab, citasTab, ventasTab, cobrosTab, notasTab);
        tabs.addClassName("cliente-tabs");
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> renderizarTab(event.getSelectedTab()));

        contenidoTab.addClassName("cliente-tab-content");

        VerticalLayout contenedor = new VerticalLayout(tabs, contenidoTab);
        contenedor.setPadding(false);
        contenedor.setSpacing(false);
        contenedor.setWidthFull();
        return contenedor;
    }

    private Component crearMetrica(String etiqueta, String valor) {
        Span valorSpan = new Span(valor);
        valorSpan.addClassName("cliente-metric-value");

        Span etiquetaSpan = new Span(etiqueta);
        etiquetaSpan.addClassName("cliente-metric-label");

        VerticalLayout metrica = new VerticalLayout(valorSpan, etiquetaSpan);
        metrica.addClassName("cliente-metric");
        metrica.setPadding(false);
        metrica.setSpacing(false);
        return metrica;
    }

    private void renderizarTab(Tab tab) {
        contenidoTab.removeAll();

        if (tab == resumenTab) {
            contenidoTab.add(crearResumen());
        } else if (tab == mascotasTab) {
            contenidoTab.add(crearGridMascotas());
        } else if (tab == citasTab) {
            contenidoTab.add(crearGridCitas());
        } else if (tab == ventasTab) {
            contenidoTab.add(crearGridVentas());
        } else if (tab == cobrosTab) {
            contenidoTab.add(crearGridCobros());
        } else if (tab == notasTab) {
            contenidoTab.add(crearGridNotas());
        }
    }

    private Component crearResumen() {
        VerticalLayout resumen = new VerticalLayout(
                crearFilaResumen("Tipo de cliente", detalleActual.tipoCliente()),
                crearFilaResumen("Dirección", detalleActual.direccion()),
                crearFilaResumen("Ubicación", formatCoordinate(detalleActual.latitud()) + ", " + formatCoordinate(detalleActual.longitud())),
                crearFilaResumen("Balance pendiente", formatMoney(detalleActual.balancePendiente()))
        );
        resumen.addClassName("cliente-summary-list");
        resumen.setPadding(false);
        resumen.setSpacing(false);
        return resumen;
    }

    private Component crearFilaResumen(String etiqueta, String valor) {
        Span label = new Span(etiqueta);
        label.addClassName("cliente-summary-label");

        Span value = new Span(valor == null || valor.isBlank() ? "Sin registrar" : valor);
        value.addClassName("cliente-summary-value");

        HorizontalLayout fila = new HorizontalLayout(label, value);
        fila.addClassName("cliente-summary-row");
        fila.setWidthFull();
        fila.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return fila;
    }

    private Component crearGridMascotas() {
        Grid<ClienteDetalleDTO.MascotaResumenDTO> grid = crearGridDetalle();
        grid.addColumn(ClienteDetalleDTO.MascotaResumenDTO::nombre).setHeader("Nombre");
        grid.addColumn(ClienteDetalleDTO.MascotaResumenDTO::tipoAnimal).setHeader("Tipo");
        grid.addColumn(ClienteDetalleDTO.MascotaResumenDTO::raza).setHeader("Raza");
        grid.addColumn(ClienteDetalleDTO.MascotaResumenDTO::sexo).setHeader("Sexo");
        grid.addColumn(mascota -> formatDate(mascota.fechaNacimiento())).setHeader("Nacimiento");
        grid.setItems(detalleActual.mascotas());
        return envolverGridDetalle(grid, detalleActual.mascotas().isEmpty(), "Este cliente no tiene mascotas registradas.");
    }

    private Component crearGridCitas() {
        Grid<ClienteDetalleDTO.CitaResumenDTO> grid = crearGridDetalle();
        grid.addColumn(cita -> formatDateTime(cita.fechaHora())).setHeader("Fecha");
        grid.addColumn(ClienteDetalleDTO.CitaResumenDTO::mascota).setHeader("Mascota");
        grid.addColumn(ClienteDetalleDTO.CitaResumenDTO::veterinario).setHeader("Veterinario");
        grid.addColumn(ClienteDetalleDTO.CitaResumenDTO::servicio).setHeader("Servicio");
        grid.addColumn(cita -> Boolean.TRUE.equals(cita.realizado()) ? "Realizada" : "Pendiente").setHeader("Estado");
        grid.setItems(detalleActual.citas());
        return envolverGridDetalle(grid, detalleActual.citas().isEmpty(), "Este cliente no tiene citas registradas.");
    }

    private Component crearGridVentas() {
        Grid<ClienteDetalleDTO.VentaResumenDTO> grid = crearGridDetalle();
        grid.addColumn(venta -> formatDateTime(venta.fechaHoraVenta())).setHeader("Fecha");
        grid.addColumn(ClienteDetalleDTO.VentaResumenDTO::vendedor).setHeader("Vendedor");
        grid.addColumn(ClienteDetalleDTO.VentaResumenDTO::estado).setHeader("Estado");
        grid.addColumn(venta -> formatMoney(venta.montoTotal())).setHeader("Total");
        grid.setItems(detalleActual.ventas());
        return envolverGridDetalle(grid, detalleActual.ventas().isEmpty(), "Este cliente no tiene ventas registradas.");
    }

    private Component crearGridCobros() {
        Grid<ClienteDetalleDTO.CobroResumenDTO> grid = crearGridDetalle();
        grid.addColumn(ClienteDetalleDTO.CobroResumenDTO::idCobro).setHeader("ID");
        grid.addColumn(cobro -> formatMoney(cobro.montoTotal())).setHeader("Monto");
        grid.addColumn(ClienteDetalleDTO.CobroResumenDTO::metodoPago).setHeader("Método");
        grid.setItems(detalleActual.cobros());
        return envolverGridDetalle(grid, detalleActual.cobros().isEmpty(), "Este cliente no tiene cobros registrados.");
    }

    private Component crearGridNotas() {
        Grid<ClienteDetalleDTO.NotaCreditoResumenDTO> grid = crearGridDetalle();
        grid.addColumn(ClienteDetalleDTO.NotaCreditoResumenDTO::idNotaCredito).setHeader("ID");
        grid.addColumn(nota -> formatMoney(nota.monto())).setHeader("Monto");
        grid.setItems(detalleActual.notasCredito());
        return envolverGridDetalle(grid, detalleActual.notasCredito().isEmpty(), "Este cliente no tiene notas de crédito.");
    }

    private <T> Grid<T> crearGridDetalle() {
        Grid<T> grid = new Grid<>();
        grid.addClassName("cliente-detail-grid");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("260px");
        return grid;
    }

    private Component envolverGridDetalle(Component grid, boolean vacio, String mensaje) {
        if (!vacio) {
            return grid;
        }

        Span empty = new Span(mensaje);
        empty.addClassName("cliente-empty-state");
        VerticalLayout wrapper = new VerticalLayout(empty);
        wrapper.addClassName("cliente-empty-wrapper");
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        return wrapper;
    }

    private void abrirDialogoCliente(Cliente cliente) {
        boolean editando = cliente != null && cliente.getIdCliente() != null;
        Cliente clienteForm = editando ? cliente : nuevoCliente();
        Persona persona = clienteForm.getPersona() != null ? clienteForm.getPersona() : new Persona();
        clienteForm.setPersona(persona);

        Dialog dialog = new Dialog();
        dialog.setWidth("720px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3(editando ? "Editar cliente" : "Nuevo cliente");
        titulo.addClassName("cliente-dialog-title");

        TextField cedula = new TextField("Cédula");
        cedula.setValue(valueOrEmpty(persona.getCedula()));
        cedula.setReadOnly(editando);
        cedula.setRequiredIndicatorVisible(true);
        cedula.setPlaceholder("000-0000000-0");
        cedula.setHelperText("Formato: 000-0000000-0");

        TextField nombre = new TextField("Nombre");
        nombre.setValue(valueOrEmpty(persona.getNombre()));
        nombre.setRequiredIndicatorVisible(true);

        TextField telefono = new TextField("Teléfono");
        telefono.setValue(valueOrEmpty(persona.getTelefono()));
        telefono.setRequiredIndicatorVisible(true);
        telefono.setPlaceholder("000-000-0000");
        telefono.setHelperText("Formato: 000-000-0000");

        TextField direccion = new TextField("Dirección");
        direccion.setValue(valueOrEmpty(persona.getDireccion()));
        direccion.setRequiredIndicatorVisible(true);

        ComboBox<TipoCliente> tipoCliente = new ComboBox<>("Tipo de cliente");
        tipoCliente.setItems(clienteService.findTiposCliente());
        tipoCliente.setItemLabelGenerator(TipoCliente::getNombreTipoCliente);
        tipoCliente.setValue(clienteForm.getTipoCliente());
        tipoCliente.setRequiredIndicatorVisible(true);

        NumberField latitud = new NumberField("Latitud");
        latitud.setValue(clienteForm.getLatitud());

        NumberField longitud = new NumberField("Longitud");
        longitud.setValue(clienteForm.getLongitud());

        HorizontalLayout filaIdentidad = new HorizontalLayout(cedula, nombre);
        filaIdentidad.setWidthFull();
        filaIdentidad.setFlexGrow(1, cedula, nombre);

        HorizontalLayout filaContacto = new HorizontalLayout(telefono, direccion);
        filaContacto.setWidthFull();
        filaContacto.setFlexGrow(1, telefono, direccion);

        HorizontalLayout filaComercial = new HorizontalLayout(tipoCliente, latitud, longitud);
        filaComercial.setWidthFull();
        filaComercial.setFlexGrow(1, tipoCliente, latitud, longitud);

        Button guardar = new Button(editando ? "Guardar cambios" : "Crear cliente", new Icon(VaadinIcon.CHECK));
        guardar.addClassName("btn-nuevo");
        guardar.addClickListener(event -> {
            if (cedula.isEmpty() || nombre.isEmpty() || telefono.isEmpty() || direccion.isEmpty() || tipoCliente.isEmpty()) {
                mostrarError("Completa los campos obligatorios del cliente.");
                return;
            }

            if (!cedula.getValue().trim().matches(CEDULA_REGEX)) {
                cedula.setInvalid(true);
                cedula.setErrorMessage("Usa el formato 000-0000000-0");
                mostrarError("La cédula debe tener el formato 000-0000000-0.");
                return;
            }

            if (!telefono.getValue().trim().matches(TELEFONO_REGEX)) {
                telefono.setInvalid(true);
                telefono.setErrorMessage("Usa el formato 000-000-0000");
                mostrarError("El teléfono debe tener el formato 000-000-0000.");
                return;
            }

            cedula.setInvalid(false);
            telefono.setInvalid(false);

            persona.setCedula(cedula.getValue().trim());
            persona.setNombre(nombre.getValue().trim());
            persona.setTelefono(telefono.getValue().trim());
            persona.setDireccion(direccion.getValue().trim());
            clienteForm.setTipoCliente(tipoCliente.getValue());
            clienteForm.setLatitud(latitud.getValue());
            clienteForm.setLongitud(longitud.getValue());

            try {
                Cliente guardado = editando ? clienteService.update(clienteForm) : clienteService.save(clienteForm);
                dialog.close();
                refrescarGrid();
                seleccionarCliente(guardado.getIdCliente());
                mostrarExito(editando ? "Cliente actualizado" : "Cliente creado");
            } catch (IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
            } catch (RuntimeException ex) {
                mostrarError("No se pudo guardar el cliente. Revisa los datos del formulario.");
            }
        });

        Button cancelar = new Button("Cancelar", event -> dialog.close());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout acciones = new HorizontalLayout(cancelar, guardar);
        acciones.addClassName("cliente-dialog-actions");
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.setWidthFull();

        VerticalLayout contenido = new VerticalLayout(titulo, filaIdentidad, filaContacto, filaComercial, acciones);
        contenido.setPadding(false);
        contenido.setSpacing(true);
        dialog.add(contenido);
        dialog.open();
    }

    private void confirmarEliminacion(ClienteResumenDTO cliente) {
        Dialog dialog = new Dialog();
        dialog.setWidth("420px");

        H3 titulo = new H3("Eliminar cliente");
        titulo.addClassName("cliente-dialog-title");

        Span mensaje = new Span("¿Seguro que deseas eliminar a " + cliente.nombre() + "?");

        Button cancelar = new Button("Cancelar", event -> dialog.close());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button eliminar = new Button("Eliminar", new Icon(VaadinIcon.TRASH), event -> {
            try {
                clienteService.deleteById(cliente.idCliente());
                dialog.close();
                gridClientes.deselectAll();
                configurarDetalleVacio();
                refrescarGrid();
                mostrarExito("Cliente eliminado");
            } catch (RuntimeException ex) {
                mostrarError("No se pudo eliminar el cliente. Puede tener registros relacionados.");
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

    private void seleccionarCliente(Long idCliente) {
        clienteService.findResumen().stream()
                .filter(cliente -> cliente.idCliente().equals(idCliente))
                .findFirst()
                .ifPresent(gridClientes::select);
    }

    private Cliente nuevoCliente() {
        Cliente cliente = new Cliente();
        cliente.setPersona(new Persona());
        return cliente;
    }

    private void configurarDetalleVacio() {
        detalleActual = null;
        panelDetalle.removeAll();

        Span icono = new Span();
        icono.add(new Icon(VaadinIcon.USER));
        icono.addClassName("cliente-empty-icon");

        Span titulo = new Span("Selecciona un cliente");
        titulo.addClassName("cliente-empty-title");

        Span descripcion = new Span("Aquí verás mascotas, citas, ventas, cobros y notas de crédito.");
        descripcion.addClassName("cliente-empty-description");

        VerticalLayout empty = new VerticalLayout(icono, titulo, descripcion);
        empty.addClassName("cliente-detail-empty");
        empty.setAlignItems(FlexComponent.Alignment.CENTER);
        empty.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        panelDetalle.add(empty);
    }

    private boolean contains(String value, String termino) {
        return value != null && value.toLowerCase().contains(termino);
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "CL";
        }

        String[] partes = nombre.trim().split("\\s+");
        String primera = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (primera + segunda).toUpperCase();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? DATE_TIME_FORMATTER.format(dateTime) : "Sin fecha";
    }

    private String formatDate(LocalDate date) {
        return date != null ? DATE_FORMATTER.format(date) : "Sin fecha";
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value != null ? value : BigDecimal.ZERO);
    }

    private String formatCoordinate(Double value) {
        return value != null ? String.format("%.6f", value) : "Sin registrar";
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
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
