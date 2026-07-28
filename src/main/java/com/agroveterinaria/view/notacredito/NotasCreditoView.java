package com.agroveterinaria.view.notacredito;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.NotaDeCredito;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.service.ClienteService;
import com.agroveterinaria.service.NotaDeCreditoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class NotasCreditoView extends VerticalLayout {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "DO"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final NotaDeCreditoService notaService;
    private final ClienteService clienteService;
    private final Grid<NotaDeCredito> grid = new Grid<>(NotaDeCredito.class, false);
    private final GridPaginator<NotaDeCredito> paginator = new GridPaginator<>(grid, 10, "notas de crédito");
    private final TextField buscar = new TextField();
    private final Span clientesConSaldo = new Span();
    private final Span montoEmitido = new Span();
    private final Span saldoDisponible = new Span();

    public NotasCreditoView(NotaDeCreditoService notaService, ClienteService clienteService) {
        this.notaService = notaService;
        this.clienteService = clienteService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Notas de Crédito");
        titulo.getStyle().set("margin", "0");

        Button agregar = new Button("Agregar nota", new Icon(VaadinIcon.PLUS));
        agregar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        agregar.addClickListener(event -> abrirDialogo(null));

        HorizontalLayout encabezado = new HorizontalLayout(titulo, agregar);
        encabezado.setWidthFull();
        encabezado.setAlignItems(FlexComponent.Alignment.CENTER);
        encabezado.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        buscar.setPlaceholder("Buscar por cliente, cédula, motivo o número...");
        buscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscar.setClearButtonVisible(true);
        buscar.setWidthFull();
        buscar.setValueChangeMode(ValueChangeMode.LAZY);
        buscar.addValueChangeListener(event -> actualizarGrid());

        configurarGrid();
        add(encabezado, crearMetricas(), buscar, paginator, grid);
        expand(grid);
        actualizarGrid();
    }

    private HorizontalLayout crearMetricas() {
        HorizontalLayout layout = new HorizontalLayout(
                crearMetrica("Clientes con saldo", clientesConSaldo),
                crearMetrica("Crédito emitido", montoEmitido),
                crearMetrica("Saldo disponible", saldoDisponible)
        );
        layout.setWidthFull();
        layout.setSpacing(true);
        return layout;
    }

    private VerticalLayout crearMetrica(String etiqueta, Span valor) {
        valor.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "700");
        Span label = new Span(etiqueta);
        label.getStyle().set("color", "var(--lumo-secondary-text-color)");
        VerticalLayout card = new VerticalLayout(valor, label);
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-base-color)");
        return card;
    }

    private void configurarGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addColumn(nota -> "#" + nota.getIdNotaCredito())
                .setHeader("Nota").setWidth("90px").setFlexGrow(0);
        grid.addColumn(this::nombreCliente)
                .setHeader("Cliente").setFlexGrow(2).setSortable(true);
        grid.addColumn(nota -> cedulaCliente(nota.getCliente()))
                .setHeader("Cédula").setWidth("150px").setFlexGrow(0);
        grid.addColumn(nota -> nota.getFechaEmision() != null
                        ? nota.getFechaEmision().format(DATE_FORMAT)
                        : "-")
                .setHeader("Emisión").setWidth("170px").setFlexGrow(0);
        grid.addColumn(nota -> dinero(nota.getMonto()))
                .setHeader("Monto emitido").setWidth("150px").setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(nota -> dinero(nota.getSaldoDisponible()))
                .setHeader("Monto que cubre").setWidth("160px").setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(nota -> nota.getMotivo() != null ? nota.getMotivo() : "-")
                .setHeader("Motivo").setFlexGrow(2);

        grid.addComponentColumn(nota -> {
            Button editar = new Button(new Icon(VaadinIcon.EDIT));
            editar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editar.setTooltipText("Editar nota");
            editar.addClickListener(event -> abrirDialogo(nota));

            Button eliminar = new Button(new Icon(VaadinIcon.TRASH));
            eliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            eliminar.setTooltipText("Eliminar nota");
            eliminar.addClickListener(event -> confirmarEliminacion(nota));

            return new HorizontalLayout(editar, eliminar);
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);
    }

    private void abrirDialogo(NotaDeCredito nota) {
        boolean editando = nota != null;
        boolean esDevolucion = editando && notaService.provieneDeDevolucion(nota);

        Dialog dialog = new Dialog();
        dialog.setWidth("540px");
        H3 titulo = new H3(editando ? "Editar nota de crédito" : "Nueva nota de crédito");

        ComboBox<Cliente> cliente = new ComboBox<>("Cliente");
        cliente.setItems(clienteService.findAll());
        cliente.setItemLabelGenerator(this::nombreCliente);
        cliente.setWidthFull();

        BigDecimalField monto = new BigDecimalField("Monto emitido");
        monto.setPrefixComponent(new Span("RD$"));
        monto.setWidthFull();

        DateTimePicker fecha = new DateTimePicker("Fecha de emisión");
        fecha.setWidthFull();

        TextArea motivo = new TextArea("Motivo");
        motivo.setMaxLength(255);
        motivo.setWidthFull();

        if (editando) {
            cliente.setValue(nota.getCliente());
            monto.setValue(nota.getMonto());
            fecha.setValue(nota.getFechaEmision());
            motivo.setValue(nota.getMotivo() != null ? nota.getMotivo() : "");
            if (esDevolucion) {
                cliente.setEnabled(false);
                monto.setEnabled(false);
                monto.setHelperText("El monto está respaldado por una devolución.");
            } else if (nota.getMontoUtilizado().compareTo(BigDecimal.ZERO) > 0) {
                cliente.setEnabled(false);
                monto.setHelperText("Ya se utilizaron " + dinero(nota.getMontoUtilizado()) + ".");
            }
        } else {
            fecha.setValue(LocalDateTime.now());
        }

        Button cancelar = new Button("Cancelar", event -> dialog.close());
        Button guardar = new Button(editando ? "Guardar cambios" : "Agregar nota", event -> {
            try {
                if (cliente.isEmpty() || monto.isEmpty() || fecha.isEmpty()) {
                    throw new IllegalArgumentException("Cliente, monto y fecha son obligatorios.");
                }
                if (editando) {
                    notaService.editar(
                            nota.getIdNotaCredito(),
                            cliente.getValue().getIdCliente(),
                            monto.getValue(),
                            fecha.getValue(),
                            motivo.getValue()
                    );
                } else {
                    notaService.crear(
                            cliente.getValue().getIdCliente(),
                            monto.getValue(),
                            fecha.getValue(),
                            motivo.getValue()
                    );
                }
                dialog.close();
                actualizarGrid();
                mostrarExito(editando ? "Nota actualizada correctamente." : "Nota agregada correctamente.");
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout acciones = new HorizontalLayout(cancelar, guardar);
        acciones.setWidthFull();
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        dialog.add(titulo, cliente, monto, fecha, motivo, acciones);
        dialog.open();
    }

    private void confirmarEliminacion(NotaDeCredito nota) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Eliminar nota de crédito");
        dialog.add(new Span(
                "¿Deseas eliminar la nota #" + nota.getIdNotaCredito()
                        + " de " + nombreCliente(nota) + "?"
        ));

        Button cancelar = new Button("Cancelar", event -> dialog.close());
        Button eliminar = new Button("Eliminar", event -> {
            try {
                notaService.eliminar(nota.getIdNotaCredito());
                dialog.close();
                actualizarGrid();
                mostrarExito("Nota eliminada correctamente.");
            } catch (Exception ex) {
                dialog.close();
                mostrarError(ex.getMessage());
            }
        });
        eliminar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancelar, eliminar);
        dialog.open();
    }

    private void actualizarGrid() {
        List<NotaDeCredito> todas = notaService.listarTodas();
        String filtro = buscar.getValue() != null
                ? buscar.getValue().trim().toLowerCase(Locale.ROOT)
                : "";
        List<NotaDeCredito> visibles = todas.stream()
                .filter(nota -> filtro.isBlank()
                        || String.valueOf(nota.getIdNotaCredito()).contains(filtro)
                        || contiene(nombreCliente(nota), filtro)
                        || contiene(cedulaCliente(nota.getCliente()), filtro)
                        || contiene(nota.getMotivo(), filtro))
                .toList();
        paginator.setItems(visibles);

        long clientes = todas.stream()
                .filter(nota -> seguro(nota.getSaldoDisponible()).compareTo(BigDecimal.ZERO) > 0)
                .map(nota -> nota.getCliente().getIdCliente())
                .distinct()
                .count();
        BigDecimal emitido = todas.stream()
                .map(nota -> seguro(nota.getMonto()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disponible = todas.stream()
                .map(nota -> seguro(nota.getSaldoDisponible()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        clientesConSaldo.setText(String.valueOf(clientes));
        montoEmitido.setText(dinero(emitido));
        saldoDisponible.setText(dinero(disponible));
    }

    private boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private String nombreCliente(NotaDeCredito nota) {
        return nota != null ? nombreCliente(nota.getCliente()) : "";
    }

    private String nombreCliente(Cliente cliente) {
        Persona persona = cliente != null ? cliente.getPersona() : null;
        return persona != null && persona.getNombre() != null ? persona.getNombre() : "Cliente sin nombre";
    }

    private String cedulaCliente(Cliente cliente) {
        Persona persona = cliente != null ? cliente.getPersona() : null;
        return persona != null && persona.getCedula() != null ? persona.getCedula() : "-";
    }

    private BigDecimal seguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String dinero(BigDecimal valor) {
        return MONEY_FORMAT.format(seguro(valor));
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4500, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
