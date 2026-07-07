package com.agroveterinaria.view.nomina;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.PeriodoFiscal;
import com.agroveterinaria.service.PeriodoFiscalService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import java.time.LocalDate;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class PeriodoFiscalView extends VerticalLayout {

    private final PeriodoFiscalService periodoFiscalService;
    private final Grid<PeriodoFiscal> gridPeriodos;
    private final GridPaginator<PeriodoFiscal> paginator;

    public PeriodoFiscalView(PeriodoFiscalService periodoFiscalService){
        this.periodoFiscalService = periodoFiscalService;
        this.gridPeriodos = new Grid<>(PeriodoFiscal.class, false);
        this.paginator = new GridPaginator<>(gridPeriodos, 10, "periodos");

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        Button btnNuevoPeriodo = new Button("Nuevo Periodo Fiscal", new Icon(VaadinIcon.PLUS));
        btnNuevoPeriodo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevoPeriodo.addClickListener(e -> dialogNuevoPeriodo());


        configurarGrid();

        add(btnNuevoPeriodo, paginator, gridPeriodos);
        actualizarGrid();

    }

    private void configurarGrid() {
        gridPeriodos.addColumn(PeriodoFiscal::getAnio).setHeader("Año").setSortable(true);
        gridPeriodos.addColumn(PeriodoFiscal::getFechaInicio).setHeader("Fecha Inicio");
        gridPeriodos.addColumn(PeriodoFiscal::getFechaCierre).setHeader("Fecha Cierre");

        gridPeriodos.addClassName("periodo-grid");
        gridPeriodos.setWidthFull();
        gridPeriodos.setHeight("390px");

        gridPeriodos.addComponentColumn(periodo -> {
            Span circulo = new Span();
            circulo.getStyle().set("width", "10px");
            circulo.getStyle().set("height", "10px");
            circulo.getStyle().set("border-radius", "50%");
            circulo.getStyle().set("display", "inline-block");
            circulo.getStyle().set("background-color", periodo.isCerrado() ? "#d32f2f" : "#2e7d32");

            Span texto = new Span(periodo.isCerrado() ? "Cerrado" : "Abierto");
            texto.getStyle().set("color", periodo.isCerrado() ? "#d32f2f" : "#2e7d32");
            texto.getStyle().set("font-weight", "500");

            HorizontalLayout layout = new HorizontalLayout(circulo, texto);
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            return layout;
        }).setHeader("Estado");

        gridPeriodos.addComponentColumn(periodo -> {
            Button btnCerrar = new Button("Cerrar Año");
            btnCerrar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            btnCerrar.setEnabled(!periodo.isCerrado());
            btnCerrar.addClickListener(e -> confirmarCierrePeriodo(periodo));
            return btnCerrar;
        }).setHeader("Acciones");
    }

    private void actualizarGrid() {
        paginator.setItems(periodoFiscalService.findAll());
    }

    private void dialogNuevoPeriodo() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Registrar Nuevo Período Fiscal");

        IntegerField anioField = new IntegerField("Año");
        anioField.setReadOnly(true);

        int ultimoAnio = periodoFiscalService.findAll().stream()
                .mapToInt(PeriodoFiscal::getAnio)
                .max()
                .orElse(LocalDate.now().getYear() - 1);

        anioField.setValue(ultimoAnio + 1);

        DatePicker inicioField = new DatePicker("Fecha de Inicio");
        inicioField.setValue(LocalDate.of(ultimoAnio + 1, 1, 1));

        DatePicker cierreField = new DatePicker("Fecha de Cierre");
        cierreField.setValue(LocalDate.of(ultimoAnio + 1, 12, 31));

        FormLayout formLayout = new FormLayout(anioField, inicioField, cierreField);

        Button btnGuardar = new Button("Guardar", e -> {
            if (anioField.isEmpty() || inicioField.isEmpty() || cierreField.isEmpty()) {
                mostrarError("Todos los campos son obligatorios");
                return;
            }

            if (periodoFiscalService.existePeriodoAbierto()) {
                Notification.show("Error: No puedes abrir un nuevo año fiscal si hay otro abierto.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            PeriodoFiscal nuevoPeriodo = new PeriodoFiscal();
            nuevoPeriodo.setAnio(anioField.getValue());
            nuevoPeriodo.setFechaInicio(inicioField.getValue());
            nuevoPeriodo.setFechaCierre(cierreField.getValue());
            nuevoPeriodo.setCerrado(false);

            periodoFiscalService.save(nuevoPeriodo);
            mostrarExito("Período fiscal abierto exitosamente");

            dialog.close();
            actualizarGrid();
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());

        dialog.add(formLayout);
        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private void confirmarCierrePeriodo(PeriodoFiscal periodo) {
        if (periodoFiscalService.existenPeriodosAnterioresAbiertos(periodo.getAnio())) {
            mostrarError("No puede cerrar el año " + periodo.getAnio() + " porque existen años anteriores que aún están abiertos.");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Confirmar Cierre de Período Discal");

        dialog.add("¿Está seguro que desea cerrar el período fiscal " + periodo.getAnio() + "? ");
        dialog.add("Esta acción es irreversible.");

        Button btnConfirmar = new Button("Cerrar Definitivamente", e -> {
            periodo.setCerrado(true);
            periodoFiscalService.save(periodo);

            mostrarExito("Período fiscal cerrado exitosamente");

            actualizarGrid();
            dialog.close();
        });
        btnConfirmar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(btnConfirmar, btnCancelar);
        dialog.open();
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
