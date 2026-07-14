package com.agroveterinaria.view.logistica;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.dto.despacho.DespachoResumenDTO;
import com.agroveterinaria.entity.Despacho;
import com.agroveterinaria.entity.Transporte;
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
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@PageTitle("Gestión de Despachos")
@RolesAllowed({"ADMINISTRADOR", "CONDUCTOR", "ASISTENTE"})
@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class GestionDespachosView extends VerticalLayout {

    private final DespachoService despachoService;
    private final VehiculoService vehiculoService;
    private final EmpleadoService empleadoService;
    private final LoteService loteService;
    private final TransporteService transporteService;
    private Grid<DespachoResumenDTO> gridDespachos;
    private GridPaginator<DespachoResumenDTO> paginator;
    private List<DespachoResumenDTO> despachos = List.of();

    private Span lblTotalDespachosVal;
    private Span lblVentasVal;
    private Span lblTransferenciasVal;

    public GestionDespachosView(DespachoService despachoService,
                                VehiculoService vehiculoService,
                                EmpleadoService empleadoService,
                                LoteService loteService,
                                TransporteService transporteService) {
        this.despachoService = despachoService;
        this.vehiculoService = vehiculoService;
        this.empleadoService = empleadoService;
        this.loteService = loteService;
        this.transporteService = transporteService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button btnNuevo = new Button("Nuevo Despacho", new Icon(VaadinIcon.PLUS));
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> {
            NuevoDespachoDialog dialog = new NuevoDespachoDialog(
                    despachoService,
                    vehiculoService,
                    empleadoService,
                    loteService,
                    this::cargarDatos
            );
            dialog.open();
        });

        HorizontalLayout header = new HorizontalLayout(btnNuevo);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        HorizontalLayout kpis = construirTarjetasKPI();

        HorizontalLayout filtros = construirFiltros();
        construirGrid();

        add(header, kpis, filtros, paginator, gridDespachos);

        cargarDatos();
    }

    private HorizontalLayout construirTarjetasKPI() {
        HorizontalLayout kpiLayout = new HorizontalLayout();
        kpiLayout.setWidthFull();

        lblTotalDespachosVal = new Span("0");
        lblVentasVal = new Span("0");
        lblTransferenciasVal = new Span("0");

        kpiLayout.add(crearTarjeta("Total Despachos Listados", lblTotalDespachosVal, VaadinIcon.PACKAGE));
        kpiLayout.add(crearTarjeta("Ventas", lblVentasVal, VaadinIcon.CART));
        kpiLayout.add(crearTarjeta("Transferencias", lblTransferenciasVal, VaadinIcon.EXCHANGE));

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
        tarjeta.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("background-color", "var(--lumo-base-color)");

        return tarjeta;
    }

    private HorizontalLayout construirFiltros() {
        Button btnTodos = new Button("Todos", e -> filtrarPorTipo(""));
        btnTodos.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnVentas = new Button("Ventas", e -> filtrarPorTipo("Venta"));
        Button btnTransferencias = new Button("Transferencias", e -> filtrarPorTipo("Transferencia"));

        return new HorizontalLayout(btnTodos, btnVentas, btnTransferencias);
    }

    private void construirGrid() {
        gridDespachos = new Grid<>(DespachoResumenDTO.class, false);
        gridDespachos.setWidthFull();
        gridDespachos.setHeight("390px");
        gridDespachos.addClassName("almacen-grid");
        gridDespachos.addThemeNames("row-stripes");
        paginator = new GridPaginator<>(gridDespachos, 10, "despachos");

        gridDespachos.addColumn(dto -> dto.getCodigo() != null ? dto.getCodigo() : "-")
                .setHeader("ID Despacho").setFlexGrow(0).setWidth("130px");

        gridDespachos.addComponentColumn(dto -> {
            String tipo = dto.getTipo() != null ? dto.getTipo() : "Desc.";
            Span badge = new Span(tipo);

            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add(tipo.equals("Venta") ? "success" : "contrast");

            return badge;
        }).setHeader("Tipo").setFlexGrow(0).setWidth("130px");

        gridDespachos.addColumn(dto -> dto.getDestinatario() != null ? dto.getDestinatario() : "-")
                .setHeader("Destinatario").setFlexGrow(2);

        gridDespachos.addColumn(dto -> dto.getDireccionEntrega() != null ? dto.getDireccionEntrega() : "-")
                .setHeader("Destino").setFlexGrow(2);

        gridDespachos.addColumn(dto -> dto.getFechaProgramadaFormateada() != null ? dto.getFechaProgramadaFormateada() : "-")
                .setComparator((d1, d2) -> {
                    if (d1.getFechaProgramadaRaw() == null && d2.getFechaProgramadaRaw() == null) return 0;
                    if (d1.getFechaProgramadaRaw() == null) return -1;
                    if (d2.getFechaProgramadaRaw() == null) return 1;
                    return d1.getFechaProgramadaRaw().compareTo(d2.getFechaProgramadaRaw());
                })
                .setHeader("Fecha Prog.").setFlexGrow(1);

        gridDespachos.addColumn(dto -> dto.getEstado() != null ? dto.getEstado() : "-")
                .setHeader("Estado").setFlexGrow(1);

        gridDespachos.addComponentColumn(dto -> {
            HorizontalLayout acciones = new HorizontalLayout();
            acciones.setSpacing(false);

            Button btnVer = new Button(new Icon(VaadinIcon.EYE));
            btnVer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnVer.addClickListener(e -> {
                Notification.show("Abrir modal para el despacho " + dto.getCodigo());
            });
            acciones.add(btnVer);

            boolean esVenta = "Venta".equals(dto.getTipo());
            boolean noLiquidado = dto.getEstado() != null && !dto.getEstado().equalsIgnoreCase("COMPLETADO");

            if (esVenta && noLiquidado) {
                Button btnLiquidar = new Button(new Icon(VaadinIcon.INVOICE));
                btnLiquidar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                btnLiquidar.setTooltipText("Liquidar Retorno del Chofer");

                btnLiquidar.addClickListener(e -> {
                    Despacho despacho = despachoService.buscarPorIdConTransporte(dto.getIdDespacho()).orElse(null);
                    if (despacho != null) {
                        Transporte transporte = despacho.getTransporte();
                        LiquidarRutaDialog dialog = new LiquidarRutaDialog(transporte, despachoService, this::cargarDatos);
                        dialog.open();
                    }
                });

                acciones.add(btnLiquidar);
            }

            return acciones;
        }).setHeader("Acciones").setWidth("140px").setFlexGrow(0);
    }

    private void cargarDatos() {
        List<DespachoResumenDTO> lista = despachoService.obtenerColaDespachos();

        long countVentas = lista.stream().filter(d -> "Venta".equals(d.getTipo())).count();
        long countTransferencias = lista.stream().filter(d -> "Transferencia".equals(d.getTipo())).count();

        lblTotalDespachosVal.setText(String.valueOf(lista.size()));
        lblVentasVal.setText(String.valueOf(countVentas));
        lblTransferenciasVal.setText(String.valueOf(countTransferencias));

        despachos = List.copyOf(lista);
        paginator.setItems(despachos);
    }

    private void filtrarPorTipo(String tipo) {
        paginator.setItems(despachos.stream()
                .filter(dto -> tipo.isEmpty() || dto.getTipo().equals(tipo))
                .toList());
    }
}
