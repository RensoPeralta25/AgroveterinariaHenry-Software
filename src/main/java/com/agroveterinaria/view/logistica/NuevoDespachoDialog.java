package com.agroveterinaria.view.logistica;

import com.agroveterinaria.dto.despacho.LineaDespachoDTO;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Transferencia;
import com.agroveterinaria.entity.Vehiculo;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.DespachoService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.VehiculoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NuevoDespachoDialog extends Dialog {

    private final DespachoService despachoService;
    private final VehiculoService vehiculoService;
    private final EmpleadoService empleadoService;
    private final Runnable alGuardarExitosamente;

    private ComboBox<Transferencia> cbTransferencia;
    private ComboBox<Vehiculo> cbVehiculo;
    private ComboBox<Empleado> cbConductor;
    private Grid<LineaDespachoDTO> gridLineas;

    private List<LineaDespachoDTO> lineasActuales = new ArrayList<>();

    public NuevoDespachoDialog(DespachoService despachoService, VehiculoService vehiculoService, EmpleadoService empleadoService, Runnable alGuardarExitosamente) {
        this.empleadoService = empleadoService;
        this.despachoService = despachoService;
        this.vehiculoService = vehiculoService;
        this.alGuardarExitosamente = alGuardarExitosamente;

        setWidth("800px");
        setCloseOnOutsideClick(false);

        H3 titulo = new H3("Preparar Carga al Camión (Picking)");
        titulo.getStyle().set("margin-top", "0");

        construirFormulario();
        construirGrid();

        Button btnGuardar = new Button("Confirmar y Despachar", e -> procesarDespacho());
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnCancelar, btnGuardar);
        botones.getStyle().set("margin-top", "20px");
        botones.setWidthFull();
        botones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        add(titulo, cbTransferencia, new HorizontalLayout(cbVehiculo, cbConductor), gridLineas, botones);
    }

    private void construirFormulario() {
        cbTransferencia = new ComboBox<>("Documento Pendiente");
        cbTransferencia.setWidthFull();
        cbTransferencia.setItems(despachoService.obtenerTransferenciasPendientes());
        cbTransferencia.setItemLabelGenerator(t -> "TRF-" + t.getIdTransferencia() + " -> " + t.getAlmacenDestino().getNombre());

        cbTransferencia.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                lineasActuales = despachoService.obtenerLineasPendientesTransferencia(e.getValue());
                gridLineas.setItems(lineasActuales);
            } else {
                lineasActuales.clear();
                gridLineas.setItems(lineasActuales);
            }
        });

        cbVehiculo = new ComboBox<>("Vehículo Asignado");
        cbVehiculo.setWidthFull();
        cbVehiculo.setItems(vehiculoService.listarTodos());
        cbVehiculo.setItemLabelGenerator(v -> v.getPlaca() + " - " + v.getModelo());

        cbConductor = new ComboBox<>("Conductor Asignado");
        cbConductor.setWidthFull();
        cbConductor.setItems(empleadoService.findByCargo(RolEmpleado.CONDUCTOR));
        cbConductor.setItemLabelGenerator(c -> c.getPersona().getNombre());
    }

    private void construirGrid() {
        gridLineas = new Grid<>(LineaDespachoDTO.class, false);
        gridLineas.addThemeNames("row-stripes", "compact");
        gridLineas.setHeight("250px");

        gridLineas.addColumn(LineaDespachoDTO::getNombreProducto).setHeader("Producto").setFlexGrow(2);
        gridLineas.addColumn(LineaDespachoDTO::getNumeroLote).setHeader("Lote").setFlexGrow(1);
        gridLineas.addColumn(LineaDespachoDTO::getCantidadPendiente).setHeader("Pendiente").setFlexGrow(0).setTextAlign(ColumnTextAlign.END);

        gridLineas.addComponentColumn(dto -> {
            BigDecimalField field = new BigDecimalField();
            field.setWidth("100px");
            field.setValue(dto.getCantidadADespacharActual());
            field.setValueChangeMode(ValueChangeMode.ON_BLUR);
            field.addValueChangeListener(e -> {
                if (e.getValue() == null || e.getValue().compareTo(BigDecimal.ZERO) < 0) {
                    field.setValue(BigDecimal.ZERO);
                    dto.setCantidadADespacharActual(BigDecimal.ZERO);
                } else if (e.getValue().compareTo(dto.getCantidadPendiente()) > 0) {
                    field.setValue(dto.getCantidadPendiente());
                    dto.setCantidadADespacharActual(dto.getCantidadPendiente());
                    Notification.show("No puede despachar más de lo pendiente").addThemeVariants(NotificationVariant.LUMO_WARNING);
                } else {
                    dto.setCantidadADespacharActual(e.getValue());
                }
            });
            return field;
        }).setHeader("A Cargar Hoy").setFlexGrow(0).setTextAlign(ColumnTextAlign.END);
    }

    private void procesarDespacho() {
        if (cbTransferencia.isEmpty() || cbVehiculo.isEmpty() || cbConductor.isEmpty()) {
            Notification.show("Complete todos los campos de cabecera").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        boolean hayMercancia = lineasActuales.stream().anyMatch(l -> l.getCantidadADespacharActual().compareTo(BigDecimal.ZERO) > 0);
        if (!hayMercancia) {
            Notification.show("Debe cargar al menos un artículo al camión").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            despachoService.procesarDespachoTransferencia(cbTransferencia.getValue(), cbVehiculo.getValue(), cbConductor.getValue(), lineasActuales);
            Notification.show("Camión despachado. Inventario actualizado.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            alGuardarExitosamente.run();
            close();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}