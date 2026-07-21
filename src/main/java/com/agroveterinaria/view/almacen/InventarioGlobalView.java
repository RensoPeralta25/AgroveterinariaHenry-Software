package com.agroveterinaria.view.almacen;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.dto.inventario.InventarioGlobalDTO;
import com.agroveterinaria.entity.Inventario;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.service.InventarioService;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@Route("almacen/inventario-global")
@PageTitle("Inventario Global")
@RolesAllowed({"ADMINISTRADOR", "ASISTENTE", "AUDITOR"})
public class InventarioGlobalView extends VerticalLayout {

    private final InventarioService inventarioService;
    private Grid<InventarioGlobalDTO> gridGlobal;
    private GridPaginator<InventarioGlobalDTO> paginator;
    private List<InventarioGlobalDTO> datosOriginales;

    public InventarioGlobalView(InventarioService inventarioService) {
        this.inventarioService = inventarioService;

        setSizeFull();
        setSpacing(true);

        TextField txtBuscar = new TextField();
        txtBuscar.setPlaceholder("Buscar por nombre o categoría...");
        txtBuscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtBuscar.setValueChangeMode(ValueChangeMode.LAZY);
        txtBuscar.setWidth("350px");
        txtBuscar.addValueChangeListener(e -> filtrarGrid(e.getValue()));

        HorizontalLayout toolbar = new HorizontalLayout(txtBuscar);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.addClassName("almacen-toolbar");

        construirGridGlobal();
        actualizarDatos();

        add(toolbar, paginator, gridGlobal);
    }

    private void construirGridGlobal() {
        gridGlobal = new Grid<>(InventarioGlobalDTO.class, false);
        gridGlobal.setWidthFull();
        gridGlobal.setHeight("390px");
        gridGlobal.addThemeNames("row-stripes");
        paginator = new GridPaginator<>(gridGlobal, 10, "productos");

        gridGlobal.addClassName("almacen-grid");

        gridGlobal.addColumn(dto -> dto.getProducto().getNombre())
                .setHeader("Producto")
                .setFlexGrow(3).setSortable(true);

        gridGlobal.addColumn(dto -> dto.getProducto().getCategoria().getEtiqueta())
                .setHeader("Categoría")
                .setFlexGrow(1).setSortable(true);

        gridGlobal.addColumn(dto -> {
            Producto p = dto.getProducto();

            String empaque = p.getUnidadEmpaque() != null ? p.getUnidadEmpaque().name() : "N/A";

            if (Boolean.TRUE.equals(p.getPermiteFraccionamiento()) && p.getUnidadFraccion() != null) {
                String fraccion = p.getUnidadFraccion().name();
                return empaque + " - " + fraccion;
            }

            return empaque;
        }).setHeader("Presentación").setFlexGrow(1).setSortable(true);

        gridGlobal.addColumn(dto -> {
                    Producto p = dto.getProducto();
                    return FormatoInventarioUtil.formatearCantidad(
                            dto.getTotalGlobal(),
                            p.getContenidoPorEmpaque(),
                            Boolean.TRUE.equals(p.getPermiteFraccionamiento()),
                            false
                    );
                })
                .setHeader("Existencia Total")
                .setFlexGrow(1)
                .setTextAlign(ColumnTextAlign.END)
                .setSortable(true);

        gridGlobal.addComponentColumn(dto -> {
                    Button btnDesglose = new Button("Ver Desglose", new Icon(VaadinIcon.EYE));
                    btnDesglose.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    btnDesglose.addClickListener(e -> abrirModalDesglose(dto.getProducto()));

                    HorizontalLayout layoutAcciones = new HorizontalLayout(btnDesglose);
                    layoutAcciones.setJustifyContentMode(JustifyContentMode.END);
                    layoutAcciones.setWidthFull();

                    return layoutAcciones;
                })
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private void actualizarDatos() {
        datosOriginales = inventarioService.obtenerInventarioGlobal();
        paginator.setItems(datosOriginales);
    }

    private void filtrarGrid(String filtro) {
        if (filtro == null || filtro.isEmpty()) {
            paginator.setItems(datosOriginales);
            return;
        }
        String search = filtro.toLowerCase().trim();
        List<InventarioGlobalDTO> filtrados = datosOriginales.stream()
                .filter(dto -> dto.getProducto().getNombre().toLowerCase().contains(search) ||
                        dto.getProducto().getCategoria().getEtiqueta().toLowerCase().contains(search))
                .toList();
        paginator.setItems(filtrados);
    }

    private void abrirModalDesglose(Producto producto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");

        H3 titulo = new H3("Desglose: " + producto.getNombre());
        titulo.getStyle().set("margin-top", "0");

        Grid<Inventario> gridDesglose = new Grid<>(Inventario.class, false);
        gridDesglose.addThemeNames("row-stripes");
        gridDesglose.setHeight("400px");

        gridDesglose.setPartNameGenerator(inv -> {
            if (inv.getLote() != null && inv.getLote().getFechaVencimiento() != null) {
                long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), inv.getLote().getFechaVencimiento());
                if (diasRestantes < 0) {
                    return "lote-expirado";
                } else if (diasRestantes <= 30) {
                    return "lote-peligro";
                }
            }
            return null;
        });

        gridDesglose.addColumn(inv -> inv.getAlmacen().getNombre()).setHeader("Almacén").setFlexGrow(2);
        gridDesglose.addColumn(inv -> inv.getLote().getNumeroLote() != null ? inv.getLote().getNumeroLote() : "S/N")
                .setHeader("Lote").setFlexGrow(1);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        gridDesglose.addColumn(inv -> inv.getLote().getFechaVencimiento() != null ? inv.getLote().getFechaVencimiento().format(fmt) : "No Caduca")
                .setHeader("Vencimiento").setFlexGrow(1);

        gridDesglose.addColumn(inv -> {
            if (inv.getLote().getFechaVencimiento() != null) {
                long dias = ChronoUnit.DAYS.between(LocalDate.now(), inv.getLote().getFechaVencimiento());
                if (dias < 0) return "¡Expirado!";
                return dias + " días";
            }
            return "-";
        }).setHeader("Estado").setFlexGrow(1);

        gridDesglose.addColumn(inv -> {
                    return FormatoInventarioUtil.formatearCantidad(
                            inv.getCantidadActual(),
                            producto.getContenidoPorEmpaque(),
                            Boolean.TRUE.equals(producto.getPermiteFraccionamiento()),
                            false
                    );
                })
                .setHeader("Cantidad").setFlexGrow(1).setTextAlign(ColumnTextAlign.END);

        List<Inventario> detalle = inventarioService.obtenerDesglosePorProducto(producto);
        gridDesglose.setItems(detalle);

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        btnCerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footer = new HorizontalLayout(btnCerrar);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout layout = new VerticalLayout(titulo, gridDesglose, footer);
        layout.setPadding(false);
        dialog.add(layout);
        dialog.open();
    }
}
