package com.agroveterinaria.view.logistica;

import com.agroveterinaria.dto.recepcion.GastoOperativoUI;
import com.agroveterinaria.entity.Transporte;
import com.agroveterinaria.service.DespachoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiquidarRutaDialog extends Dialog {

    private final DespachoService despachoService;
    private final Transporte transporte;
    private final Runnable alLiquidarExitosamente;

    private final List<GastoOperativoUI> listaGastos = new ArrayList<>();
    private final Grid<GastoOperativoUI> gridGastos = new Grid<>(GastoOperativoUI.class, false);

    public LiquidarRutaDialog(Transporte transporte, DespachoService despachoService, Runnable alLiquidarExitosamente) {
        this.transporte = transporte;
        this.despachoService = despachoService;
        this.alLiquidarExitosamente = alLiquidarExitosamente;

        setWidth("600px");
        setCloseOnOutsideClick(false);

        H3 titulo = new H3("Liquidar Ruta del Camión #" + transporte.getIdTransporte());
        Paragraph subtitulo = new Paragraph("Conductor: " + transporte.getConductor().getPersona().getNombre() +
                " | Vehículo: " + transporte.getVehiculo().getPlaca());
        subtitulo.getStyle().set("color", "gray").set("margin-top", "0");

        construirTablaGastos();

        Button btnLiquidar = new Button("Confirmar Retorno y Gastos", new Icon(VaadinIcon.CHECK_CIRCLE));
        btnLiquidar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnLiquidar.addClickListener(e -> procesarLiquidacion());

        Button btnCancelar = new Button("Cancelar", e -> close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnCancelar, btnLiquidar);
        botones.setWidthFull();
        botones.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "20px");

        VerticalLayout layout = new VerticalLayout(titulo, subtitulo, gridGastos, botones);
        layout.setPadding(false);
        add(layout);
    }

    private void construirTablaGastos() {
        gridGastos.setHeight("250px");
        gridGastos.addThemeNames("row-stripes");

        listaGastos.add(new GastoOperativoUI());
        gridGastos.setItems(listaGastos);

        gridGastos.addComponentColumn(gasto -> {
            TextField txtNota = new TextField();
            txtNota.setWidthFull();
            txtNota.setPlaceholder("Ej: Peaje Autopista, Dietas...");
            txtNota.setValue(gasto.getNotas() != null ? gasto.getNotas() : "");
            txtNota.addValueChangeListener(ev -> gasto.setNotas(ev.getValue()));
            return txtNota;
        }).setHeader("Concepto").setFlexGrow(2);

        gridGastos.addComponentColumn(gasto -> {
            BigDecimalField txtMonto = new BigDecimalField();
            txtMonto.setWidthFull();
            txtMonto.setPrefixComponent(new Span("RD$"));
            txtMonto.setValue(gasto.getMonto());
            txtMonto.addValueChangeListener(ev -> gasto.setMonto(ev.getValue()));
            return txtMonto;
        }).setHeader("Monto").setWidth("150px").setFlexGrow(0);

        gridGastos.addComponentColumn(gasto -> {
            Button btnQuitar = new Button(new Icon(VaadinIcon.TRASH));
            btnQuitar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnQuitar.addClickListener(ev -> {
                listaGastos.remove(gasto);
                gridGastos.getDataProvider().refreshAll();
            });
            return btnQuitar;
        }).setWidth("80px").setFlexGrow(0);

        Button btnAddGasto = new Button("Agregar gasto", new Icon(VaadinIcon.PLUS));
        btnAddGasto.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnAddGasto.addClickListener(e -> {
            listaGastos.add(new GastoOperativoUI());
            gridGastos.getDataProvider().refreshAll();
        });

        gridGastos.appendFooterRow().getCell(gridGastos.getColumns().get(0)).setComponent(btnAddGasto);
    }

    private void procesarLiquidacion() {
        boolean gastosValidos = listaGastos.stream().allMatch(g ->
                g.getMonto() == null || g.getMonto().compareTo(BigDecimal.ZERO) >= 0);

        if (!gastosValidos) {
            Notification.show("Asegúrese de colocar montos válidos.").addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        try {
            despachoService.liquidarViaje(transporte.getIdTransporte(), listaGastos);
            Notification.show("Transporte liquidado y gastos registrados.", 4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            alLiquidarExitosamente.run();
            close();
        } catch (Exception ex) {
            Notification.show("Error al liquidar: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}