package com.agroveterinaria.view.transferencia;

import com.agroveterinaria.dto.detalle_transferencia.DetalleTransferenciaDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.service.*;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.agroveterinaria.component.CantidadFraccionadaField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Registrar Transferencia")
@RolesAllowed("ADMINISTRADOR")
@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class RegistroTransferenciaView extends VerticalLayout {

    private final AlmacenService almacenService;
    private final TransferenciaService transferenciaService;
    private final InventarioService inventarioService;
    private final Runnable accionVolver;

    private ComboBox<Almacen> cbAlmacenOrigen;
    private ComboBox<Almacen> cbAlmacenDestino;
    private RadioButtonGroup<String> rbgTipoTraslado;
    private Grid<Inventario> gridInventario;
    private Grid<DetalleTransferenciaDTO> gridDetalles;

    private final List<DetalleTransferenciaDTO> carrito = new ArrayList<>();

    public RegistroTransferenciaView(AlmacenService almacenService,
                                     TransferenciaService transferenciaService,
                                     InventarioService inventarioService,
                                     Runnable accionVolver) {
        this.almacenService = almacenService;
        this.transferenciaService = transferenciaService;
        this.inventarioService = inventarioService;
        this.accionVolver = accionVolver;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button btnVolver = new Button("Volver a Transferencias", new Icon(VaadinIcon.ARROW_LEFT));
        btnVolver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnVolver.addClickListener(e -> accionVolver.run());

        H2 titulo = new H2("Registro de Transferencia entre Almacenes");
        titulo.getStyle().set("margin-top", "0");

        VerticalLayout panelIzquierdo = construirPanelIzquierdo();
        VerticalLayout panelDerecho = construirPanelDerecho();

        SplitLayout splitLayout = new SplitLayout(panelIzquierdo, panelDerecho);
        splitLayout.setSplitterPosition(50);
        splitLayout.setSizeFull();

        add(btnVolver, titulo, splitLayout);
    }

    private VerticalLayout construirPanelIzquierdo() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setSizeFull();

        HorizontalLayout layoutAlmacenes = new HorizontalLayout();
        layoutAlmacenes.setWidthFull();

        cbAlmacenOrigen = new ComboBox<>("Almacén Origen");
        cbAlmacenOrigen.setItems(almacenService.listarTodos());
        cbAlmacenOrigen.setItemLabelGenerator(Almacen::getNombre);
        cbAlmacenOrigen.setWidthFull();
        cbAlmacenOrigen.addValueChangeListener(e -> {
            actualizarInventarioOrigen();
            Almacen seleccionado = e.getValue();
            if (seleccionado != null) {
                cbAlmacenDestino.setItems(almacenService.listarTodos().stream()
                        .filter(a -> !a.getIdAlmacen().equals(seleccionado.getIdAlmacen()))
                        .toList());
            } else {
                cbAlmacenDestino.setItems(almacenService.listarTodos());
            }
        });

        cbAlmacenDestino = new ComboBox<>("Almacén Destino");
        cbAlmacenDestino.setItems(almacenService.listarTodos());
        cbAlmacenDestino.setItemLabelGenerator(Almacen::getNombre);
        cbAlmacenDestino.setWidthFull();

        layoutAlmacenes.add(cbAlmacenOrigen, cbAlmacenDestino);

        rbgTipoTraslado = new RadioButtonGroup<>();
        rbgTipoTraslado.setLabel("Modalidad del Traslado:");
        rbgTipoTraslado.setItems("Inmediato / Interno (Express)", "Inter-Sucursal (Requiere Despacho y Transporte)");
        rbgTipoTraslado.setValue("Inmediato / Interno (Express)");
        rbgTipoTraslado.getStyle().set("margin-top", "5px");

        H3 lblInventario = new H3("Inventario Disponible");
        lblInventario.getStyle().set("margin", "10px 0 0 0");

        TextField txtBuscar = new TextField();
        txtBuscar.setPlaceholder("Buscar producto...");
        txtBuscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtBuscar.setValueChangeMode(ValueChangeMode.LAZY);
        txtBuscar.setWidthFull();
        txtBuscar.addValueChangeListener(e -> buscarInventario(e.getValue()));

        gridInventario = new Grid<>(Inventario.class, false);
        gridInventario.addClassName("transferencia-grid");
        gridInventario.addThemeNames("row-stripes");
        gridInventario.addColumn(i -> i.getLote().getProducto().getNombre()).setHeader("Producto").setFlexGrow(2).setComparator(inventario -> inventario.getLote().getProducto().getNombre());
        gridInventario.addColumn(i -> i.getLote().getNumeroLote()).setHeader("Lote").setFlexGrow(1).setComparator(inventario -> inventario.getLote().getNumeroLote());

        gridInventario.addColumn(i -> FormatoInventarioUtil.formatearCantidad(
                i.getCantidadActual(),
                i.getLote().getProducto().getContenidoPorEmpaque(),
                Boolean.TRUE.equals(i.getLote().getProducto().getPermiteFraccionamiento()),
                false,
                FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(i.getLote().getProducto()),
                FormatoInventarioUtil.getNombreUnidadFraccionSafe(i.getLote().getProducto())
        )).setHeader("Stock").setTextAlign(ColumnTextAlign.END).setFlexGrow(1).setComparator(Inventario::getCantidadActual);

        gridInventario.addComponentColumn(this::crearBotonAgregar).setHeader("Acción").setTextAlign(ColumnTextAlign.CENTER);
        gridInventario.setSizeFull();

        layout.add(layoutAlmacenes, rbgTipoTraslado, lblInventario, txtBuscar, gridInventario);
        return layout;
    }

    private VerticalLayout construirPanelDerecho() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setSizeFull();

        H3 lblDetalles = new H3("Productos a Transferir");
        lblDetalles.getStyle().set("margin", "0");

        gridDetalles = new Grid<>(DetalleTransferenciaDTO.class, false);
        gridDetalles.addClassName("transferencia-grid");
        gridDetalles.addThemeNames("row-stripes");
        gridDetalles.addColumn(dto -> dto.getLote().getProducto().getNombre()).setHeader("Producto").setFlexGrow(2);
        gridDetalles.addColumn(dto -> dto.getLote().getNumeroLote()).setHeader("Lote").setFlexGrow(1);

        gridDetalles.addComponentColumn(this::crearCampoCantidad).setHeader("Cantidad").setFlexGrow(0).setWidth("280px");

        gridDetalles.addComponentColumn(this::crearBotonQuitar).setHeader("Acción").setTextAlign(ColumnTextAlign.CENTER).setWidth("90px").setFlexGrow(0);
        gridDetalles.setSizeFull();

        Button btnProcesar = new Button("Procesar Transferencia", new Icon(VaadinIcon.CHECK));
        btnProcesar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnProcesar.setWidthFull();
        btnProcesar.addClickListener(e -> procesarTransferencia());

        layout.add(lblDetalles, gridDetalles, btnProcesar);
        return layout;
    }

    private Button crearBotonAgregar(Inventario inventario) {
        Button btn = new Button(new Icon(VaadinIcon.PLUS));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
        btn.addClickListener(e -> {
            boolean existe = carrito.stream().anyMatch(dto -> dto.getLote().getIdLote().equals(inventario.getLote().getIdLote()));
            if (existe) {
                Notification.show("El producto/lote ya está en la transferencia").addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (inventario.getCantidadActual().compareTo(BigDecimal.ZERO) <= 0) {
                Notification.show("No hay stock disponible").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            carrito.add(new DetalleTransferenciaDTO(inventario.getLote(), inventario.getCantidadActual()));
            gridDetalles.setItems(carrito);
        });
        return btn;
    }

    private CantidadFraccionadaField crearCampoCantidad(DetalleTransferenciaDTO dto) {
        Producto prod = dto.getLote().getProducto();
        CantidadFraccionadaField field = new CantidadFraccionadaField();

        field.configurarProducto(
                prod.getContenidoPorEmpaque(),
                Boolean.TRUE.equals(prod.getPermiteFraccionamiento()),
                false,
                FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(prod),
                FormatoInventarioUtil.getNombreUnidadFraccionSafe(prod)
        );

        field.setValue(dto.getCantidad());

        field.addValueChangeListener(e -> {
            BigDecimal val = e.getValue();
            if (val == null || val.compareTo(BigDecimal.ZERO) <= 0) {
                field.setValue(BigDecimal.ONE);
                dto.setCantidad(BigDecimal.ONE);
                Notification.show("La cantidad debe ser mayor a 0").addThemeVariants(NotificationVariant.LUMO_WARNING);
                gridDetalles.getDataProvider().refreshItem(dto);
            } else if (val.compareTo(dto.getExistenciaMaxima()) > 0) {
                field.setValue(dto.getExistenciaMaxima());
                dto.setCantidad(dto.getExistenciaMaxima());

                String maxFormateado = FormatoInventarioUtil.formatearCantidad(
                        dto.getExistenciaMaxima(), prod.getContenidoPorEmpaque(),
                        Boolean.TRUE.equals(prod.getPermiteFraccionamiento()), false,
                        FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(prod),
                        FormatoInventarioUtil.getNombreUnidadFraccionSafe(prod)
                );
                Notification.show("La cantidad no puede superar la existencia máxima (" + maxFormateado + ")")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);

                gridDetalles.getDataProvider().refreshItem(dto);
            } else {
                dto.setCantidad(val);
            }
        });
        return field;
    }

    private Button crearBotonQuitar(DetalleTransferenciaDTO dto) {
        Button btn = new Button(new Icon(VaadinIcon.TRASH));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        btn.addClickListener(e -> {
            carrito.remove(dto);
            gridDetalles.setItems(carrito);
        });
        return btn;
    }

    private void actualizarInventarioOrigen() {
        Almacen origen = cbAlmacenOrigen.getValue();
        if (origen != null) {
            gridInventario.setItems(inventarioService.listarPorAlmacen(origen));
            carrito.clear();
            gridDetalles.setItems(carrito);
        }
    }

    private void buscarInventario(String term) {
        Almacen origen = cbAlmacenOrigen.getValue();
        if (origen == null) return;
        List<Inventario> invs = inventarioService.listarPorAlmacen(origen);
        if (term != null && !term.trim().isEmpty()) {
            invs = invs.stream()
                    .filter(i -> i.getLote().getProducto().getNombre().toLowerCase().contains(term.toLowerCase()))
                    .toList();
        }
        gridInventario.setItems(invs);
    }

    private void procesarTransferencia() {
        if (cbAlmacenOrigen.getValue() == null || cbAlmacenDestino.getValue() == null) {
            Notification.show("Debe seleccionar almacén de origen y destino").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (cbAlmacenOrigen.getValue().getIdAlmacen().equals(cbAlmacenDestino.getValue().getIdAlmacen())) {
            Notification.show("El almacén de origen y destino deben ser distintos").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (carrito.isEmpty()) {
            Notification.show("Debe agregar al menos un producto").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Transferencia transferencia = new Transferencia();
        transferencia.setAlmacenOrigen(cbAlmacenOrigen.getValue());
        transferencia.setAlmacenDestino(cbAlmacenDestino.getValue());
        transferencia.setFechaHoraSalidaProgramada(LocalDateTime.now());

        for (DetalleTransferenciaDTO dto : carrito) {
            DetalleTransferencia dt = new DetalleTransferencia();
            dt.setLote(dto.getLote());
            dt.setCantidad(dto.getCantidad());
            transferencia.addDetalle(dt);
        }

        try {
            boolean esInmediato = "Inmediato / Interno (Express)".equals(rbgTipoTraslado.getValue());

            transferenciaService.registrarTransferencia(transferencia, null, esInmediato);

            String msg = esInmediato ? "Transferencia procesada e inventario actualizado al instante."
                    : "Transferencia registrada. Pendiente de despacho logístico.";
            Notification.show(msg).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            cbAlmacenOrigen.clear();
            cbAlmacenDestino.clear();
            carrito.clear();
            gridDetalles.setItems(carrito);
            gridInventario.setItems(new ArrayList<>());

            accionVolver.run();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}