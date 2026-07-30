package com.agroveterinaria.view.devolucion;

import com.agroveterinaria.entity.DetalleDevVenta;
import com.agroveterinaria.entity.DevolucionVenta;
import com.agroveterinaria.enums.EstadoDevolucion;
import com.agroveterinaria.service.DevolucionVentaService;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.spring.security.AuthenticationContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class DevolucionesView extends VerticalLayout {

    private final DevolucionVentaService devolucionService;
    private final AuthenticationContext authContext;
    private final Runnable accionNuevaDevolucion;

    private Grid<DevolucionVenta> gridHistorial;
    private ListDataProvider<DevolucionVenta> dataProvider;

    private DatePicker filterFechaInicio;
    private DatePicker filterFechaFin;

    public DevolucionesView(DevolucionVentaService devolucionService, AuthenticationContext authContext,
                            Runnable accionNuevaDevolucion) {
        this.devolucionService = devolucionService;
        this.authContext = authContext;
        this.accionNuevaDevolucion = accionNuevaDevolucion;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Historial de Devoluciones y Notas de Crédito");
        titulo.getStyle().set("margin-top", "0");

        HorizontalLayout toolbar = construirToolbar();
        construirGridPrincipal();

        add(titulo, toolbar, gridHistorial);
        expand(gridHistorial);

        actualizarTabla();
    }

    private HorizontalLayout construirToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.END);
        toolbar.addClassName("almacen-toolbar");

        Button btnNueva = new Button("Registrar Devolución", new Icon(VaadinIcon.PLUS), e -> accionNuevaDevolucion.run());
        btnNueva.addClassName("btn-nuevo");

        filterFechaInicio = new DatePicker("Desde:");
        filterFechaFin = new DatePicker("Hasta:");

        filterFechaInicio.addValueChangeListener(e -> aplicarFiltrosFecha());
        filterFechaFin.addValueChangeListener(e -> aplicarFiltrosFecha());

        HorizontalLayout filtros = new HorizontalLayout(filterFechaInicio, filterFechaFin);
        filtros.setSpacing(true);

        toolbar.add(btnNueva, filtros);
        return toolbar;
    }

    private void construirGridPrincipal() {
        gridHistorial = new Grid<>(DevolucionVenta.class, false);
        gridHistorial.setSizeFull();
        gridHistorial.addThemeNames("row-stripes");
        gridHistorial.addClassName("devolucion-grid");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        gridHistorial.addColumn(d -> "DEV-" + d.getIdDevolucionVenta()).setHeader("ID").setWidth("100px").setFlexGrow(0);
        gridHistorial.addColumn(d -> d.getFechaHora().format(fmt)).setHeader("Fecha/Hora").setFlexGrow(1);
        gridHistorial.addColumn(d -> d.getCliente().getPersona().getNombre()).setHeader("Cliente").setFlexGrow(2);
        gridHistorial.addColumn(d -> d.getEmpleado().getPersona().getNombre()).setHeader("Registrado Por").setFlexGrow(1);
        gridHistorial.addColumn(DevolucionVenta::getRazonDevolucion).setHeader("Motivo").setFlexGrow(2);

        gridHistorial.addColumn(d -> String.format("RD$ %,.2f", d.getMontoTotal()))
                .setHeader("Monto Total").setTextAlign(ColumnTextAlign.END).setFlexGrow(1);

        gridHistorial.addComponentColumn(d -> {
            Span badge = new Span(d.getEstado().getEtiqueta());
            badge.getElement().getThemeList().add("badge " + (d.getEstado() == EstadoDevolucion.COMPLETADA ? "success" : "error"));
            return badge;
        }).setHeader("Estado").setWidth("130px").setFlexGrow(0);

        gridHistorial.addComponentColumn(d -> {
            Button btnDetalle = new Button(new Icon(VaadinIcon.EYE), event -> abrirDetallesModal(d));
            btnDetalle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return btnDetalle;
        }).setHeader("Ver").setWidth("80px").setFlexGrow(0);
    }

    private void actualizarTabla() {
        List<DevolucionVenta> lista = devolucionService.listarTodas();
        dataProvider = new ListDataProvider<>(lista);
        gridHistorial.setDataProvider(dataProvider);
    }

    private void aplicarFiltrosFecha() {
        if (dataProvider == null) return;

        dataProvider.setFilter(dev -> {
            LocalDate fDev = dev.getFechaHora().toLocalDate();
            if (filterFechaInicio.getValue() != null && fDev.isBefore(filterFechaInicio.getValue())) {
                return false;
            }
            if (filterFechaFin.getValue() != null && fDev.isAfter(filterFechaFin.getValue())) {
                return false;
            }
            return true;
        });
    }

    private void abrirDetallesModal(DevolucionVenta dev) {
        Dialog dialog = new Dialog();
        dialog.setWidth("850px");

        H3 titulo = new H3("Detalle Devolución DEV-" + dev.getIdDevolucionVenta());
        titulo.getStyle().set("margin-top", "0");

        String txtNc = dev.getNotaDeCredito() != null ?
                "Asociada (ID NC: #" + dev.getNotaDeCredito().getIdNotaCredito() + ")" : "Ninguna / Reembolso Directo";
        Span lblNc = new Span("Nota de Crédito: " + txtNc);
        lblNc.getStyle().set("font-weight", "600").set("color", "var(--lumo-primary-color)");

        Grid<DetalleDevVenta> gridItems = new Grid<>();
        gridItems.addThemeNames("row-stripes");
        gridItems.setHeight("250px");

        gridItems.addColumn(d -> d.getDetalleVenta().getProducto().getNombre()).setHeader("Producto").setFlexGrow(2);
        gridItems.addColumn(d -> d.getLote().getNumeroLote()).setHeader("Lote").setFlexGrow(1);
        gridItems.addColumn(d -> d.getAlmacenEntrada().getNombre()).setHeader("Almacén Entrada").setFlexGrow(1);

        gridItems.addColumn(d -> FormatoInventarioUtil.formatearCantidad(
                d.getCantidadDevuelta(),
                d.getDetalleVenta().getProducto().getContenidoPorEmpaque(),
                Boolean.TRUE.equals(d.getDetalleVenta().getProducto().getPermiteFraccionamiento()),
                false,
                FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(d.getDetalleVenta().getProducto()),
                FormatoInventarioUtil.getNombreUnidadFraccionSafe(d.getDetalleVenta().getProducto())
        )).setHeader("Cant. Devuelta").setTextAlign(ColumnTextAlign.END).setFlexGrow(1);

        gridItems.setItems(devolucionService.obtenerDetallesDeDevolucion(dev.getIdDevolucionVenta()));

        HorizontalLayout footerButtons = new HorizontalLayout();
        footerButtons.setWidthFull();
        footerButtons.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        footerButtons.add(btnCerrar);

        if (authContext.hasRole("ADMINISTRADOR") && dev.getEstado() == EstadoDevolucion.COMPLETADA) {
            Button btnAnular = new Button("Anular Operación", new Icon(VaadinIcon.BAN), event -> {
                try {
                    devolucionService.anularDevolucionManual(dev.getIdDevolucionVenta());
                    Notification.show("Devolución anulada. Stock y finanzas revertidos.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    actualizarTabla();
                    dialog.close();
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            btnAnular.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            footerButtons.add(btnAnular);
        }

        VerticalLayout layout = new VerticalLayout(titulo, lblNc, gridItems, footerButtons);
        dialog.add(layout);
        dialog.open();
    }
}