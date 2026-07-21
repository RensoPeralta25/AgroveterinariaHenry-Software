package com.agroveterinaria.view.logistica;

import com.agroveterinaria.dto.despacho.DespachoResumenDTO;
import com.agroveterinaria.dto.despacho.LineaDespachoDTO;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Vehiculo;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.DespachoService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.LoteService;
import com.agroveterinaria.service.VehiculoService;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.agroveterinaria.component.CantidadFraccionadaField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NuevoDespachoDialog extends Dialog {

    private final DespachoService despachoService;
    private final VehiculoService vehiculoService;
    private final EmpleadoService empleadoService;
    private final LoteService loteService;
    private final Runnable alGuardarExitosamente;

    private ComboBox<DespachoResumenDTO> cbDocumentoPendiente;
    private ComboBox<Vehiculo> cbVehiculo;
    private ComboBox<Empleado> cbConductor;
    private Grid<LineaDespachoDTO> gridLineas;

    private List<LineaDespachoDTO> lineasActuales = new ArrayList<>();

    public NuevoDespachoDialog(DespachoService despachoService, VehiculoService vehiculoService,
                               EmpleadoService empleadoService, LoteService loteService,
                               Runnable alGuardarExitosamente) {
        this.empleadoService = empleadoService;
        this.despachoService = despachoService;
        this.vehiculoService = vehiculoService;
        this.loteService = loteService;
        this.alGuardarExitosamente = alGuardarExitosamente;

        setWidth("980px");
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

        add(titulo, cbDocumentoPendiente, new HorizontalLayout(cbVehiculo, cbConductor), gridLineas, botones);
    }

    private void construirFormulario() {
        cbDocumentoPendiente = new ComboBox<>("Documento Pendiente (Venta o Transferencia)");
        cbDocumentoPendiente.setWidthFull();
        cbDocumentoPendiente.setItems(despachoService.obtenerDocumentosPendientesDespacho());
        cbDocumentoPendiente.setItemLabelGenerator(doc -> doc.getCodigo() + " -> Destino: " + doc.getDestinatario());

        cbDocumentoPendiente.addValueChangeListener(e -> {
            DespachoResumenDTO doc = e.getValue();
            if (doc != null) {
                if ("Venta".equals(doc.getTipo())) {
                    lineasActuales = despachoService.obtenerLineasPendientesVenta(doc.getVentaOriginal().getIdVenta());
                } else {
                    lineasActuales = despachoService.obtenerLineasPendientesTransferencia(doc.getTransferenciaOriginal());
                }
                gridLineas.setItems(lineasActuales);
            } else {
                lineasActuales.clear();
                gridLineas.setItems(lineasActuales);
            }
        });

        cbVehiculo = new ComboBox<>("Vehículo Asignado");
        cbVehiculo.setWidthFull();
        cbVehiculo.setItems(vehiculoService.listarDisponibles());
        cbVehiculo.setItemLabelGenerator(v -> v.getPlaca() + " - " + v.getModelo());

        cbConductor = new ComboBox<>("Conductor Asignado");
        cbConductor.setWidthFull();
        cbConductor.setItems(empleadoService.findByCargo(RolEmpleado.CONDUCTOR));
        cbConductor.setItemLabelGenerator(c -> c.getPersona().getNombre());
    }

    private void construirGrid() {
        gridLineas = new Grid<>(LineaDespachoDTO.class, false);
        gridLineas.addThemeNames("row-stripes");
        gridLineas.setHeight("300px");

        gridLineas.addColumn(LineaDespachoDTO::getNombreProducto).setHeader("Producto").setFlexGrow(2);

        gridLineas.addComponentColumn(dto -> {
            if (dto.getNumeroLote() != null && !dto.getNumeroLote().equals("Asignado en picking")) {
                return new Span(dto.getNumeroLote());
            }

            ComboBox<Lote> cbLotesDisponibles = new ComboBox<>();
            cbLotesDisponibles.setWidthFull();
            cbLotesDisponibles.setPlaceholder("Seleccionar lote...");

            Producto producto = obtenerProducto(dto);

            cbLotesDisponibles.setItems(loteService.buscarPorProducto(producto));
            cbLotesDisponibles.setItemLabelGenerator(Lote::getNumeroLote);

            if (dto.getLoteSeleccionadoFisicamente() != null) {
                cbLotesDisponibles.setValue(dto.getLoteSeleccionadoFisicamente());
            }

            cbLotesDisponibles.addValueChangeListener(ev -> dto.setLoteSeleccionadoFisicamente(ev.getValue()));
            return cbLotesDisponibles;
        }).setHeader("Lote").setFlexGrow(1).setWidth("160px");

        gridLineas.addColumn(dto -> {
            Producto prod = obtenerProducto(dto);
            return FormatoInventarioUtil.formatearCantidad(
                    dto.getCantidadPendiente(),
                    prod.getContenidoPorEmpaque(),
                    Boolean.TRUE.equals(prod.getPermiteFraccionamiento()),
                    false
            );
        }).setHeader("Pendiente").setFlexGrow(0).setWidth("130px").setTextAlign(ColumnTextAlign.END);


        gridLineas.addComponentColumn(dto -> {
            Producto prod = obtenerProducto(dto);

            CantidadFraccionadaField field = new CantidadFraccionadaField();
            field.configurarProducto(
                    prod.getContenidoPorEmpaque(),
                    Boolean.TRUE.equals(prod.getPermiteFraccionamiento()),
                    false
            );

            field.setValue(dto.getCantidadADespacharActual());

            field.addValueChangeListener(e -> {
                BigDecimal val = e.getValue();
                if (val == null || val.compareTo(BigDecimal.ZERO) < 0) {
                    field.setValue(BigDecimal.ZERO);
                    dto.setCantidadADespacharActual(BigDecimal.ZERO);
                } else if (val.compareTo(dto.getCantidadPendiente()) > 0) {
                    field.setValue(dto.getCantidadPendiente());
                    dto.setCantidadADespacharActual(dto.getCantidadPendiente());
                    Notification.show("No puede despachar más mercancía de la pendiente documentada").addThemeVariants(NotificationVariant.LUMO_WARNING);
                } else {
                    dto.setCantidadADespacharActual(val);
                }
            });
            return field;
        }).setHeader("A Cargar Hoy").setFlexGrow(0).setWidth("320px");
    }

    private Producto obtenerProducto(LineaDespachoDTO dto) {
        if (dto.getDetalleVenta() != null) {
            return dto.getDetalleVenta().getProducto();
        } else {
            return dto.getDetalleTransferencia().getLote().getProducto();
        }
    }

    private void procesarDespacho() {
        if (cbDocumentoPendiente.isEmpty() || cbVehiculo.isEmpty() || cbConductor.isEmpty()) {
            Notification.show("Complete todos los campos de cabecera").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        boolean hayMercancia = lineasActuales.stream().anyMatch(l -> l.getCantidadADespacharActual().compareTo(BigDecimal.ZERO) > 0);
        if (!hayMercancia) {
            Notification.show("Debe cargar al menos un artículo al camión").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        boolean faltanLotesBackorder = lineasActuales.stream().anyMatch(l ->
                l.getCantidadADespacharActual().compareTo(BigDecimal.ZERO) > 0 &&
                        "Asignado en picking".equals(l.getNumeroLote()) &&
                        l.getLoteSeleccionadoFisicamente() == null
        );

        if (faltanLotesBackorder) {
            Notification.show("Debe especificar el lote para todos los artículos en backorder a despachar")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            DespachoResumenDTO doc = cbDocumentoPendiente.getValue();

            if ("Venta".equals(doc.getTipo())) {
                despachoService.procesarDespachoVenta(doc.getVentaOriginal().getIdVenta(), cbVehiculo.getValue(), cbConductor.getValue(), lineasActuales);
            } else {
                despachoService.procesarDespachoTransferencia(doc.getTransferenciaOriginal(), cbVehiculo.getValue(), cbConductor.getValue(), lineasActuales);
            }

            Notification.show("Camión despachado. Inventario actualizado.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            alGuardarExitosamente.run();
            close();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}