package com.agroveterinaria.view.almacen;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.AjusteInventario;
import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.enums.TipoAjuste;
import com.agroveterinaria.security.SecurityService;
import com.agroveterinaria.service.*;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.agroveterinaria.component.CantidadFraccionadaField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@CssImport(value = "./sorter-styles.css", themeFor = "vaadin-grid-sorter")
@Route("almacen/ajustes-inventario")
@PageTitle("Ajustes de Inventario y Mermas")
@RolesAllowed({"ADMINISTRADOR", "AUDITOR"})
public class AjustesInventarioView extends VerticalLayout {

    private final AjusteInventarioService ajusteService;
    private final AlmacenService almacenService;
    private final ProductoService productoService;
    private final LoteService loteService;
    private final EmpleadoService empleadoService;
    private final SecurityService securityService;
    private final InventarioService inventarioService;

    private Grid<AjusteInventario> gridAuditoria;
    private GridPaginator<AjusteInventario> paginator;

    public AjustesInventarioView(AjusteInventarioService ajusteService, AlmacenService almacenService,
                                 ProductoService productoService, LoteService loteService,
                                 EmpleadoService empleadoService, SecurityService securityService,
                                 InventarioService inventarioService) {
        this.ajusteService = ajusteService;
        this.almacenService = almacenService;
        this.productoService = productoService;
        this.loteService = loteService;
        this.empleadoService = empleadoService;
        this.securityService = securityService;
        this.inventarioService = inventarioService;

        setSizeFull();
        setSpacing(true);

        Button btnNuevo = new Button("Registrar Nuevo Ajuste", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.addClickListener(e -> abrirModalNuevoAjuste());

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.addClassName("almacen-toolbar");

        construirGrid();
        actualizarGrid();

        add(toolbar, paginator, gridAuditoria);
    }

    private void construirGrid() {
        gridAuditoria = new Grid<>(AjusteInventario.class, false);
        gridAuditoria.setWidthFull();
        gridAuditoria.setHeight("390px");
        gridAuditoria.addThemeNames("row-stripes");
        gridAuditoria.addClassName("ajustes-grid");
        paginator = new GridPaginator<>(gridAuditoria, 10, "ajustes");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        gridAuditoria.addColumn(a -> a.getFechaHora().format(fmt)).setHeader("Fecha").setFlexGrow(1);
        gridAuditoria.addColumn(a -> a.getAlmacen().getNombre()).setHeader("Almacén").setFlexGrow(1);
        gridAuditoria.addColumn(a -> a.getLote().getProducto().getNombre()).setHeader("Producto").setFlexGrow(2);
        gridAuditoria.addColumn(a -> a.getLote().getNumeroLote()).setHeader("Lote").setFlexGrow(1);

        gridAuditoria.addComponentColumn(a -> {
            Span badge = new Span(a.getTipoAjuste().name());
            badge.getElement().getThemeList().add("badge " + (a.getTipoAjuste() == TipoAjuste.ENTRADA ? "success" : "error"));
            return badge;
        }).setHeader("Tipo").setFlexGrow(0).setWidth("120px");

        gridAuditoria.addColumn(a -> FormatoInventarioUtil.formatearCantidad(
                a.getCantidad(),
                a.getLote().getProducto().getContenidoPorEmpaque(),
                Boolean.TRUE.equals(a.getLote().getProducto().getPermiteFraccionamiento()),
                false
        )).setHeader("Cantidad").setFlexGrow(0).setWidth("160px");

        gridAuditoria.addColumn(AjusteInventario::getJustificacion).setHeader("Motivo").setFlexGrow(3);
        gridAuditoria.addColumn(a -> a.getEmpleado().getPersona().getNombre()).setHeader("Auditor").setFlexGrow(1);
    }

    private void actualizarGrid() {
        paginator.setItems(ajusteService.listarHistorial());
    }

    private void abrirModalNuevoAjuste() {
        Dialog dialog = new Dialog();
        dialog.setWidth("650px");

        H3 titulo = new H3("Declarar Diferencia Físca");

        RadioButtonGroup<TipoAjuste> rbgTipo = new RadioButtonGroup<>("Tipo de Movimiento");
        rbgTipo.setItems(TipoAjuste.values());
        rbgTipo.setItemLabelGenerator(TipoAjuste::getEtiqueta);
        rbgTipo.setValue(TipoAjuste.SALIDA);

        ComboBox<Almacen> cbAlmacen = new ComboBox<>("Almacén Afectado");
        cbAlmacen.setItems(almacenService.listarTodos().stream().filter(a -> a.getStatus() == StatusEntidad.ACTIVO).toList());
        cbAlmacen.setItemLabelGenerator(Almacen::getNombre);
        cbAlmacen.setWidthFull();

        ComboBox<Producto> cbProducto = new ComboBox<>("Producto");
        cbProducto.setItems(productoService.listarTodosActivos());
        cbProducto.setItemLabelGenerator(Producto::getNombre);
        cbProducto.setWidthFull();

        ComboBox<Lote> cbLote = new ComboBox<>("Lote Específico");
        cbLote.setItemLabelGenerator(lote ->
                lote.getNumeroLote() != null ? lote.getNumeroLote() : "Sin número (ID: " + lote.getIdLote() + ")"
        );
        cbLote.setWidthFull();
        cbLote.setEnabled(false);

        CantidadFraccionadaField txtCantidad = new CantidadFraccionadaField();

        Runnable actualizarLotes = () -> {
            Producto prod = cbProducto.getValue();
            Almacen alm = cbAlmacen.getValue();
            TipoAjuste tipo = rbgTipo.getValue();

            if (prod != null) {
                txtCantidad.configurarProducto(
                        prod.getContenidoPorEmpaque(),
                        Boolean.TRUE.equals(prod.getPermiteFraccionamiento()),
                        false
                );
            } else {
                txtCantidad.clear();
            }

            if (prod != null && alm != null) {
                cbLote.setEnabled(true);
                if (tipo == TipoAjuste.SALIDA) {
                    cbLote.setItems(inventarioService.obtenerLotesConStock(alm, prod));
                } else {
                    cbLote.setItems(loteService.buscarPorProducto(prod));
                }
            } else {
                cbLote.setEnabled(false);
                cbLote.clear();
            }
        };

        rbgTipo.addValueChangeListener(e -> actualizarLotes.run());
        cbAlmacen.addValueChangeListener(e -> actualizarLotes.run());
        cbProducto.addValueChangeListener(e -> actualizarLotes.run());

        TextArea txtJustificacion = new TextArea("Justificación Detallada (Obligatoria)");
        txtJustificacion.setWidthFull();
        txtJustificacion.setPlaceholder("Ej. Saco perforado por roedores en la bodega trasera...");

        Button btnGuardar = new Button("Ejecutar Ajuste", new Icon(VaadinIcon.CHECK));
        btnGuardar.addClassName("btn-nuevo");
        btnGuardar.addClickListener(e -> {
            if (cbAlmacen.getValue() == null || cbLote.getValue() == null || txtCantidad.getValue() == null || txtJustificacion.isEmpty()) {
                Notification.show("Todos los campos son obligatorios").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                Empleado auditor = securityService.obtenerEmpleadoAutenticado();

                ajusteService.registrarAjusteManual(
                        cbAlmacen.getValue(), cbLote.getValue(), rbgTipo.getValue(),
                        txtCantidad.getValue(), txtJustificacion.getValue(), auditor
                );

                Notification.show("Ajuste procesado y auditado con éxito.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                actualizarGrid();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnCancelar, btnGuardar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout layout = new VerticalLayout(titulo, rbgTipo, cbAlmacen, cbProducto, cbLote, txtCantidad, txtJustificacion, botones);
        dialog.add(layout);
        dialog.open();
    }
}
