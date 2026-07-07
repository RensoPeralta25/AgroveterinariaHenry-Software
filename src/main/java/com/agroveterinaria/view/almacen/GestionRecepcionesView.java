package com.agroveterinaria.view.almacen;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.dto.recepcion.RecepcionResumenDTO;
import com.agroveterinaria.service.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@PageTitle("Gestión de Recepciones")
@RolesAllowed({"ADMINISTRADOR", "ASISTENTE"})
@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class GestionRecepcionesView extends VerticalLayout {

    private final AlmacenService almacenService;
    private final LoteService loteService;
    private final VehiculoService vehiculoService;
    private final EmpleadoService empleadoService;
    private final RutaService rutaService;
    private final RecepcionService recepcionService;

    private Grid<RecepcionResumenDTO> gridRecepciones;
    private GridPaginator<RecepcionResumenDTO> paginator;
    private List<RecepcionResumenDTO> recepciones = List.of();

    private Span lblTotalRecepcionesVal;
    private Span lblComprasVal;
    private Span lblTransferenciasVal;

    public GestionRecepcionesView(AlmacenService almacenService, LoteService loteService,
                                  VehiculoService vehiculoService, EmpleadoService empleadoService,
                                  RutaService rutaService, RecepcionService recepcionService) {
        this.almacenService = almacenService;
        this.loteService = loteService;
        this.vehiculoService = vehiculoService;
        this.empleadoService = empleadoService;
        this.rutaService = rutaService;
        this.recepcionService = recepcionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button btnNuevo = new Button("Nueva Recepción", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.addClickListener(e -> {
            NuevaRecepcionDialog dialog = new NuevaRecepcionDialog(
                    almacenService, loteService, vehiculoService,
                    empleadoService, rutaService, recepcionService,
                    this::cargarDatos
            );
            dialog.open();
        });

        HorizontalLayout header = new HorizontalLayout(btnNuevo);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.addClassName("almacen-toolbar");

        HorizontalLayout kpis = construirTarjetasKPI();
        HorizontalLayout filtros = construirFiltros();
        construirGrid();

        add(header, kpis, filtros, paginator, gridRecepciones);

        cargarDatos();
    }

    private HorizontalLayout construirTarjetasKPI() {
        HorizontalLayout kpiLayout = new HorizontalLayout();
        kpiLayout.setWidthFull();

        lblTotalRecepcionesVal = new Span("0");
        lblComprasVal = new Span("0");
        lblTransferenciasVal = new Span("0");

        kpiLayout.add(crearTarjeta("Total Recepciones", lblTotalRecepcionesVal, VaadinIcon.INBOX));
        kpiLayout.add(crearTarjeta("Por Compras", lblComprasVal, VaadinIcon.SHOP));
        kpiLayout.add(crearTarjeta("Por Transferencias", lblTransferenciasVal, VaadinIcon.EXCHANGE));

        return kpiLayout;
    }

    private VerticalLayout crearTarjeta(String titulo, Span lblValor, VaadinIcon icono) {
        Span lblTitulo = new Span(titulo);
        lblTitulo.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "14px");
        lblValor.getStyle().set("font-size", "24px").set("font-weight", "bold").set("color", "var(--lumo-primary-color)");

        Icon icon = new Icon(icono);
        icon.setSize("20px");
        icon.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout top = new HorizontalLayout(icon, lblTitulo);
        top.setAlignItems(Alignment.CENTER);

        VerticalLayout tarjeta = new VerticalLayout(top, lblValor);
        tarjeta.setPadding(true);
        tarjeta.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px").set("background-color", "var(--lumo-base-color)");

        return tarjeta;
    }

    private HorizontalLayout construirFiltros() {
        Button btnTodos = new Button("Todas", e -> filtrarPorTipo(""));
        btnTodos.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button btnCompras = new Button("Compras", e -> filtrarPorTipo("Compra"));
        Button btnTransferencias = new Button("Transferencias", e -> filtrarPorTipo("Transferencia"));

        return new HorizontalLayout(btnTodos, btnCompras, btnTransferencias);
    }

    private void construirGrid() {
        gridRecepciones = new Grid<>(RecepcionResumenDTO.class, false);
        gridRecepciones.setWidthFull();
        gridRecepciones.setHeight("390px");
        gridRecepciones.addClassName("almacen-grid");
        gridRecepciones.addThemeNames("row-stripes");
        paginator = new GridPaginator<>(gridRecepciones, 10, "recepciones");

        gridRecepciones.addColumn(dto -> dto.getCodigo() != null ? dto.getCodigo() : "-")
                .setHeader("Documento").setWidth("120px").setFlexGrow(0);

        gridRecepciones.addComponentColumn(dto -> {
            String tipo = dto.getTipo() != null ? dto.getTipo() : "Desc.";
            Span badge = new Span(tipo);
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add(tipo.equals("Compra") ? "success" : "contrast");
            return badge;
        }).setHeader("Tipo").setFlexGrow(0).setWidth("130px");

        gridRecepciones.addColumn(dto -> dto.getOrigen() != null ? dto.getOrigen() : "-")
                .setHeader("Origen").setFlexGrow(2);

        gridRecepciones.addColumn(dto -> dto.getFechaFormateada() != null ? dto.getFechaFormateada() : "-")
                .setComparator((d1, d2) -> {
                    if (d1.getFechaRaw() == null && d2.getFechaRaw() == null) return 0;
                    if (d1.getFechaRaw() == null) return -1;
                    if (d2.getFechaRaw() == null) return 1;
                    return d1.getFechaRaw().compareTo(d2.getFechaRaw());
                })
                .setHeader("Fecha Emisión").setFlexGrow(1);

        gridRecepciones.addColumn(dto -> dto.getEstado() != null ? dto.getEstado() : "-")
                .setHeader("Estado").setFlexGrow(1);

        gridRecepciones.addComponentColumn(dto -> {
            Button btnVer = new Button(new Icon(VaadinIcon.EYE));
            btnVer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnVer.addClickListener(e -> Notification.show("Aquí podrías abrir un modal de solo lectura del histórico."));
            return btnVer;
        }).setHeader("Acciones").setWidth("100px").setFlexGrow(0);
    }

    private void cargarDatos() {
        List<RecepcionResumenDTO> pendientes = recepcionService.obtenerColaRecepciones();
        List<RecepcionResumenDTO> historial = recepcionService.obtenerHistorialRecepciones();

        List<RecepcionResumenDTO> todasLasRecepciones = new java.util.ArrayList<>();
        todasLasRecepciones.addAll(pendientes);
        todasLasRecepciones.addAll(historial);

        todasLasRecepciones.sort((d1, d2) -> {
            if (d1.getFechaRaw() == null || d2.getFechaRaw() == null) return 0;
            return d2.getFechaRaw().compareTo(d1.getFechaRaw());
        });

        long countCompras = todasLasRecepciones.stream().filter(d -> "Compra".equals(d.getTipo())).count();
        long countTransferencias = todasLasRecepciones.stream().filter(d -> "Transferencia".equals(d.getTipo())).count();

        lblTotalRecepcionesVal.setText(String.valueOf(todasLasRecepciones.size()));
        lblComprasVal.setText(String.valueOf(countCompras));
        lblTransferenciasVal.setText(String.valueOf(countTransferencias));

        recepciones = List.copyOf(todasLasRecepciones);
        paginator.setItems(recepciones);
    }

    private void filtrarPorTipo(String tipo) {
        paginator.setItems(recepciones.stream()
                .filter(dto -> tipo.isEmpty() || (dto.getTipo() != null && dto.getTipo().equals(tipo)))
                .toList());
    }
}
