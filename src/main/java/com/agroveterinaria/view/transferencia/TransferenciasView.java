package com.agroveterinaria.view.transferencia;

import com.agroveterinaria.entity.DetalleTransferencia;
import com.agroveterinaria.entity.Transferencia;
import com.agroveterinaria.enums.EstadoTransferencia;
import com.agroveterinaria.service.TransferenciaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RolesAllowed({"ADMINISTRADOR", "ASISTENTE", "AUDITOR"})
@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class TransferenciasView extends VerticalLayout {

    private final TransferenciaService transferenciaService;
    private Grid<Transferencia> gridHistorial;
    private ListDataProvider<Transferencia> dataProvider;
    private final Runnable accionNuevaTransferencia;

    public TransferenciasView(TransferenciaService transferenciaService, Runnable accionNuevaTransferencia) {
        this.transferenciaService = transferenciaService;
        this.accionNuevaTransferencia = accionNuevaTransferencia;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Historial y Seguimiento de Transferencias");
        titulo.getStyle().set("margin-top", "0");

        HorizontalLayout toolbar = construirToolbar();
        construirGrid();

        add(titulo, toolbar, gridHistorial);
        expand(gridHistorial);

        actualizarGrid();
    }

    private HorizontalLayout construirToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.addClassName("almacen-toolbar");

        Button btnNuevaTransferencia = new Button("Nueva Transferencia", new Icon(VaadinIcon.PLUS));
        btnNuevaTransferencia.addClassName("btn-nuevo");
        btnNuevaTransferencia.addClickListener(e -> accionNuevaTransferencia.run());

        ComboBox<String> cbFiltroEstado = new ComboBox<>("Filtrar por Estado");
        cbFiltroEstado.setItems("TODOS", "PENDIENTE", "EN_TRANSITO", "COMPLETADA", "CANCELADA");
        cbFiltroEstado.setValue("TODOS");
        cbFiltroEstado.addValueChangeListener(e -> {
            if (dataProvider != null) {
                if ("TODOS".equals(e.getValue()) || e.getValue() == null) {
                    dataProvider.clearFilters();
                } else {
                    dataProvider.setFilter(t -> t.getEstado() != null && t.getEstado().equals(e.getValue()));
                }
            }
        });

        toolbar.add(btnNuevaTransferencia, cbFiltroEstado);
        return toolbar;
    }

    private void construirGrid() {
        gridHistorial = new Grid<>(Transferencia.class, false);
        gridHistorial.setSizeFull();
        gridHistorial.addClassName("almacen-grid");
        gridHistorial.addThemeNames("row-stripes");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        gridHistorial.addColumn(t -> "TRF-" + t.getIdTransferencia())
                .setHeader("ID").setFlexGrow(0).setWidth("120px");

        gridHistorial.addColumn(t -> t.getFechaHoraSalidaProgramada() != null ? t.getFechaHoraSalidaProgramada().format(fmt) : "-")
                .setComparator((t1, t2) -> {
                    if (t1.getFechaHoraSalidaProgramada() == null && t2.getFechaHoraSalidaProgramada() == null) return 0;
                    if (t1.getFechaHoraSalidaProgramada() == null) return 1;
                    if (t2.getFechaHoraSalidaProgramada() == null) return -1;
                    return t1.getFechaHoraSalidaProgramada().compareTo(t2.getFechaHoraSalidaProgramada());
                })
                .setHeader("Fecha Emisión").setFlexGrow(1);

        gridHistorial.addColumn(t -> t.getAlmacenOrigen() != null ? t.getAlmacenOrigen().getNombre() : "-")
                .setHeader("Almacén Origen").setFlexGrow(2);

        gridHistorial.addColumn(t -> t.getAlmacenDestino() != null ? t.getAlmacenDestino().getNombre() : "-")
                .setHeader("Almacén Destino").setFlexGrow(2);

        gridHistorial.addComponentColumn(t -> {
            EstadoTransferencia estado = t.getEstado();
            Span badge = new Span(estado.getEtiqueta());
            badge.getElement().getThemeList().add("badge");

            if (estado == EstadoTransferencia.COMPLETADA) badge.getElement().getThemeList().add("success");
            else if (estado == EstadoTransferencia.EN_TRANSITO) badge.getElement().getThemeList().add("contrast");
            else badge.getElement().getThemeList().add("warning");

            return badge;
        }).setHeader("Estado").setFlexGrow(1);

        gridHistorial.addComponentColumn(t -> {
            Button btnDetalles = new Button("Ver Detalles", new Icon(VaadinIcon.EYE));
            btnDetalles.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnDetalles.addClickListener(e -> abrirModalDetalles(t));
            return btnDetalles;
        }).setHeader("Acciones").setWidth("140px").setFlexGrow(0);
    }

    private void actualizarGrid() {
        List<Transferencia> lista = transferenciaService.listarTodosConAlmacenes();
        lista.sort((t1, t2) -> {
            if (t1.getFechaHoraSalidaProgramada() == null && t2.getFechaHoraSalidaProgramada() == null) return 0;
            if (t1.getFechaHoraSalidaProgramada() == null) return 1;
            if (t2.getFechaHoraSalidaProgramada() == null) return -1;
            return t2.getFechaHoraSalidaProgramada().compareTo(t1.getFechaHoraSalidaProgramada());
        });

        dataProvider = new ListDataProvider<>(lista);
        gridHistorial.setDataProvider(dataProvider);
    }

    private void abrirModalDetalles(Transferencia transferencia) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");

        H3 titulo = new H3("Detalles de Transferencia TRF-" + transferencia.getIdTransferencia());
        titulo.getStyle().set("margin-top", "0");

        Grid<DetalleTransferencia> gridDetalles = new Grid<>(DetalleTransferencia.class, false);
        gridDetalles.addThemeNames("row-stripes");
        gridDetalles.setHeight("300px");

        gridDetalles.addColumn(d -> d.getLote().getProducto().getNombre()).setHeader("Producto").setFlexGrow(2);
        gridDetalles.addColumn(d -> d.getLote().getNumeroLote() != null ? d.getLote().getNumeroLote() : "S/N").setHeader("Lote").setFlexGrow(1);
        gridDetalles.addColumn(d -> String.format("%,.2f", d.getCantidad())).setHeader("Cantidad Enviada").setTextAlign(ColumnTextAlign.END).setFlexGrow(1);

        Transferencia tCompleta = transferenciaService.obtenerTransferenciaConDetalles(transferencia.getIdTransferencia());
        gridDetalles.setItems(tCompleta.getDetalles());

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout footer = new HorizontalLayout(btnCerrar);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.END);

        dialog.add(new VerticalLayout(titulo, gridDetalles, footer));
        dialog.open();
    }
}