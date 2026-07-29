package com.agroveterinaria.view.nomina;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.ConfiguracionNomina;
import com.agroveterinaria.service.ConfiguracionNominaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class ConfiguracionNominaView extends VerticalLayout {
    private final ConfiguracionNominaService configuracionNominaService;

    public ConfiguracionNominaView(ConfiguracionNominaService configuracionNominaService) {
        this.configuracionNominaService = configuracionNominaService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Grid<ConfiguracionNomina> grid = new Grid<>(ConfiguracionNomina.class, false);
        GridPaginator<ConfiguracionNomina> paginator = new GridPaginator<>(grid, 10, "configuraciones");
        grid.addClassName("configuracion-grid-grid");
        grid.addThemeNames("row-stripes");
        grid.addClassName("configuracion-grid");
        grid.setWidthFull();
        grid.setHeight("390px");

        grid.addColumn(ConfiguracionNomina::getClave).setHeader("Clave").setFlexGrow(1);
        grid.addColumn(ConfiguracionNomina::getDescripcion).setHeader("Descripción").setFlexGrow(2);
        grid.addColumn(this::formatearValorConfiguracion).setHeader("Valor actual").setWidth("150px").setFlexGrow(0);

        grid.addComponentColumn(config -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> dialogEditar(config, paginator));
            return btnEditar;
        }).setHeader("Acciones").setWidth("100px").setFlexGrow(0);

        paginator.setItems(configuracionNominaService.findAll());
        add(paginator, grid);
    }

    private void dialogEditar(ConfiguracionNomina config, GridPaginator<ConfiguracionNomina> paginator) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Editar — " + config.getDescripcion());
        dialog.setWidth("400px");

        BigDecimalField valorField = new BigDecimalField("Valor");
        valorField.setValue(config.getValor());
        valorField.setWidthFull();
        valorField.setPrefixComponent(new Span("#"));
        valorField.setClearButtonVisible(true);

        VerticalLayout contenido = new VerticalLayout(valorField);
        contenido.setPadding(false);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button btnGuardar = new Button("Guardar", new Icon(VaadinIcon.CHECK));
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnGuardar.addClickListener(e -> {
            if (valorField.isEmpty()) {
                mostrarError("El valor no puede estar vacío.");
                return;
            }

            if (valorField.getValue().compareTo(BigDecimal.ZERO) < 0) {
                mostrarError("El valor de configuración no puede ser negativo.");
                return;
            }
            config.setValor(valorField.getValue());


            try {
                configuracionNominaService.actualizar(config);
                paginator.setItems(configuracionNominaService.findAll());
                dialog.close();
                mostrarExito("Configuración actualizada correctamente.");
            } catch (Exception ex) {
                mostrarError("Error al guardar: " + ex.getMessage());
            }
        });

        dialog.add(contenido);
        dialog.getFooter().add(btnCancelar, btnGuardar);
        dialog.open();
    }

    private String formatearMonto(BigDecimal monto) {
        if (monto == null) return "0.00";
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "DO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(monto);
    }

    private String formatearValorConfiguracion(ConfiguracionNomina config) {
        if (config.getValor() == null) return "0.00";

        String clave = config.getClave().toUpperCase();

        if (clave.contains("PORCENTAJE")) {
            BigDecimal valorPorcentual = config.getValor().multiply(new BigDecimal("100"));
            return valorPorcentual.setScale(2, java.math.RoundingMode.HALF_UP) + "%";

        } else {
            return "RD$ " + formatearMonto(config.getValor());
        }
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
