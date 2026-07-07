package com.agroveterinaria.view.finanzas;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.GastoOperativo;
import com.agroveterinaria.enums.TipoGasto;
import com.agroveterinaria.service.GastoOperativoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.router.PageTitle;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

@PageTitle("Registro de Gastos Operativos")
@RolesAllowed({"ADMINISTRADOR", "GERENTE_FINANZAS"})
@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class GastosView extends VerticalLayout {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "DO"));
    private final GastoOperativoService gastoService;
    private final Grid<GastoOperativo> grid = new Grid<>(GastoOperativo.class, false);
    private final GridPaginator<GastoOperativo> paginator = new GridPaginator<>(grid, 10, "gastos");

    public GastosView(GastoOperativoService gastoService) {
        this.gastoService = gastoService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Gastos y Egresos Generales");
        titulo.getStyle().set("margin-top", "0");

        Button btnNuevo = new Button("Registrar Gasto", new Icon(VaadinIcon.PLUS));
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> abrirDialogo(new GastoOperativo()));

        HorizontalLayout toolbar = new HorizontalLayout(titulo, btnNuevo);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        configurarGrid();

        add(toolbar, paginator, grid);
        actualizarGrid();
    }

    private void configurarGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.setHeight("390px");
        grid.addClassName("gastos-grid");

        grid.addColumn(GastoOperativo::getFecha).setHeader("Fecha").setWidth("120px").setFlexGrow(0).setSortable(true);

        grid.addComponentColumn(g -> {
            Span badge = new Span(g.getTipoGasto().getEtiqueta());
            badge.getElement().getThemeList().add("badge");
            if (g.getTipoGasto() == TipoGasto.FIJO) {
                badge.getElement().getThemeList().add("primary");
            } else {
                badge.getElement().getThemeList().add("contrast");
            }
            return badge;
        }).setHeader("Tipo").setWidth("120px").setFlexGrow(0);

        grid.addColumn(GastoOperativo::getNotas).setHeader("Concepto / Descripción").setFlexGrow(2);
        grid.addColumn(GastoOperativo::getComprobanteFiscal).setHeader("Comprobante").setFlexGrow(1);

        grid.addColumn(g -> MONEY_FORMAT.format(g.getMonto())).setHeader("Monto")
                .setWidth("150px").setFlexGrow(0).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        grid.addComponentColumn(g -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.EDIT));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> abrirDialogo(g));

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnEliminar.addClickListener(e -> confirmarEliminacion(g));

            return new HorizontalLayout(btnEditar, btnEliminar);
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);
    }

    private void abrirDialogo(GastoOperativo gasto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        H3 titulo = new H3(gasto.getIdGasto() == null ? "Nuevo Egreso" : "Editar Egreso");
        titulo.getStyle().set("margin-top", "0");

        DatePicker dpFecha = new DatePicker("Fecha del Gasto");
        dpFecha.setWidthFull();

        ComboBox<TipoGasto> cbTipo = new ComboBox<>("Clasificación");
        cbTipo.setItems(TipoGasto.values());
        cbTipo.setItemLabelGenerator(TipoGasto::getEtiqueta);
        cbTipo.setWidthFull();

        BigDecimalField txtMonto = new BigDecimalField("Monto Total (RD$)");
        txtMonto.setWidthFull();

        TextField txtComprobante = new TextField("NCF / Número de Factura");
        txtComprobante.setPlaceholder("Opcional");
        txtComprobante.setWidthFull();

        TextArea txtNotas = new TextArea("Concepto / Descripción detallada");
        txtNotas.setPlaceholder("Ej. Mantenimiento camión ficha 04, pago de luz, etc.");
        txtNotas.setWidthFull();
        txtNotas.setMaxLength(255);

        if (gasto.getIdGasto() != null) {
            dpFecha.setValue(gasto.getFecha());
            cbTipo.setValue(gasto.getTipoGasto());
            txtMonto.setValue(gasto.getMonto());
            txtComprobante.setValue(gasto.getComprobanteFiscal() != null ? gasto.getComprobanteFiscal() : "");
            txtNotas.setValue(gasto.getNotas() != null ? gasto.getNotas() : "");
        } else {
            dpFecha.setValue(LocalDate.now());
            cbTipo.setValue(TipoGasto.VARIABLE);
        }

        HorizontalLayout fila1 = new HorizontalLayout(dpFecha, cbTipo);
        fila1.setWidthFull();

        VerticalLayout form = new VerticalLayout(fila1, txtMonto, txtComprobante, txtNotas);
        form.setPadding(false);

        Button btnGuardar = new Button("Guardar Gasto", e -> {
            if (dpFecha.isEmpty() || cbTipo.isEmpty() || txtMonto.isEmpty() || txtNotas.isEmpty()) {
                Notification.show("Complete los campos obligatorios (Fecha, Tipo, Monto y Concepto)").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                gasto.setFecha(dpFecha.getValue());
                gasto.setTipoGasto(cbTipo.getValue());
                gasto.setMonto(txtMonto.getValue());
                gasto.setComprobanteFiscal(txtComprobante.getValue());
                gasto.setNotas(txtNotas.getValue());

                gastoService.guardar(gasto);
                actualizarGrid();
                dialog.close();
                Notification.show("Gasto registrado correctamente").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error al guardar: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout acciones = new HorizontalLayout(btnCancelar, btnGuardar);
        acciones.setWidthFull();
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.getStyle().set("margin-top", "20px");

        dialog.add(titulo, form, acciones);
        dialog.open();
    }

    private void confirmarEliminacion(GastoOperativo gasto) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Eliminar Registro");
        confirmDialog.add(new Span("¿Está seguro de que desea eliminar este gasto por RD$ " + MONEY_FORMAT.format(gasto.getMonto()) + "?"));

        Button btnConfirmar = new Button("Eliminar", e -> {
            try {
                gastoService.eliminar(gasto.getIdGasto());
                actualizarGrid();
                confirmDialog.close();
                Notification.show("Gasto eliminado correctamente").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalStateException ex) {
                confirmDialog.close();
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                confirmDialog.close();
                Notification.show("Error inesperado al intentar borrar el registro").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnConfirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        confirmDialog.getFooter().add(new Button("Cancelar", e -> confirmDialog.close()), btnConfirmar);
        confirmDialog.open();
    }

    private void actualizarGrid() {
        paginator.setItems(gastoService.listarTodos());
    }
}
