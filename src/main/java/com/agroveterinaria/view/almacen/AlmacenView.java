package com.agroveterinaria.view.almacen;

import com.agroveterinaria.component.CrudGridPaginator;
import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Inventario;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.AlmacenService;
import com.agroveterinaria.service.InventarioService;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@CssImport(value = "./sorter-styles.css", themeFor = "vaadin-grid-sorter")
@PageTitle("Gestión de Almacenes")
@Route("/almacenes")
@RolesAllowed("ADMINISTRADOR")
public class AlmacenView extends VerticalLayout {

    private final AlmacenService almacenService;
    private final InventarioService inventarioService;

    public AlmacenView(AlmacenService almacenService, InventarioService inventarioService) {
        this.almacenService = almacenService;
        this.inventarioService = inventarioService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        GridCrud<Almacen> crudAlmacen = new GridCrud<>(Almacen.class, new WindowBasedCrudLayout());
        crudAlmacen.addClassName("almacen-crud");
        crudAlmacen.getGrid().addClassName("almacen-grid");
        CrudGridPaginator<Almacen> paginator = new CrudGridPaginator<>(10, "almacenes");
        paginator.setRefreshOperation(crudAlmacen::refreshGrid);

        crudAlmacen.getGrid().removeAllColumns();

        crudAlmacen.getGrid().addColumn(Almacen::getIdAlmacen)
                .setHeader("ID")
                .setKey("idAlmacen")
                .setWidth("80px").setFlexGrow(0).setComparator(Almacen::getIdAlmacen);

        crudAlmacen.getGrid().addColumn(Almacen::getNombre)
                .setHeader("Nombre del Almacén")
                .setKey("nombre")
                .setFlexGrow(1).setComparator(Almacen::getNombre);

        crudAlmacen.getGrid().addColumn(Almacen::getDireccion)
                .setHeader("Dirección")
                .setKey("direccion")
                .setFlexGrow(2).setComparator(Almacen::getDireccion);

        crudAlmacen.getGrid().addComponentColumn(almacen -> {
            boolean isActivo = almacen.getStatus() == StatusEntidad.ACTIVO;
            Span badge = new Span(almacen.getStatus() != null ? almacen.getStatus().getEtiqueta() : "");
            badge.getElement().getThemeList().add("badge " + (isActivo ? "success" : "error"));
            return badge;
        }).setHeader("Estado").setKey("status").setWidth("120px").setFlexGrow(0).setComparator(a -> a.getStatus().getEtiqueta());

        crudAlmacen.getGrid().addComponentColumn(almacen -> {

            Button btnStock = new Button(new Icon(VaadinIcon.PACKAGE));
            btnStock.addClassName("btn-accion-stock");
            btnStock.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            btnStock.setTooltipText("Ver inventario");
            btnStock.addClickListener(e -> dialogVerStock(almacen));

            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.setTooltipText("Editar almacén");
            btnEditar.addClickListener(e -> dialogAlmacen(almacen, crudAlmacen, false));

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addClassName("btn-accion-eliminar");
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnEliminar.setTooltipText("Eliminar almacén");
            btnEliminar.addClickListener(e -> {
                crudAlmacen.getGrid().select(almacen);
                crudAlmacen.getDeleteButton().click();
            });

            HorizontalLayout acciones = new HorizontalLayout(btnStock, btnEditar, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("160px").setFlexGrow(0);

        crudAlmacen.getGrid().addThemeNames("row-stripes");

        crudAlmacen.getAddButton().setVisible(false);
        crudAlmacen.getUpdateButton().setVisible(false);
        crudAlmacen.getDeleteButton().setVisible(false);
        crudAlmacen.getFindAllButton().setVisible(false);

        Button btnNuevo = new Button("Nuevo Almacén", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> dialogAlmacen(new Almacen(), crudAlmacen, true));

        TextField buscarAlmacen = new TextField();
        buscarAlmacen.setWidthFull();
        buscarAlmacen.setPlaceholder("Buscar por nombre o dirección...");
        buscarAlmacen.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarAlmacen.setValueChangeMode(ValueChangeMode.LAZY);
        buscarAlmacen.addValueChangeListener(e -> {
            String filtro = e.getValue().toLowerCase().trim();
            paginator.setSource(() ->
                    almacenService.listarTodos().stream()
                            .filter(a -> (a.getNombre() != null && a.getNombre().toLowerCase().contains(filtro)) ||
                                    (a.getDireccion() != null && a.getDireccion().toLowerCase().contains(filtro)))
                            .toList()
            );
            paginator.reset();
        });

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, buscarAlmacen);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.addClassName("almacen-toolbar");
        toolbar.expand(buscarAlmacen);

        paginator.setSource(almacenService::listarTodos);
        crudAlmacen.setFindAllOperation(paginator::pageItems);
        crudAlmacen.setDeleteOperation(almacenService::eliminar);

        crudAlmacen.getCrudFormFactory().setCaption(org.vaadin.crudui.crud.CrudOperation.DELETE, "¿Eliminar Almacén?");

        add(toolbar, paginator, crudAlmacen);
    }

    private void dialogAlmacen(Almacen almacen, GridCrud<Almacen> crudAlmacen, boolean esNuevo) {
        Dialog dialog = new Dialog();
        dialog.setWidth("650px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3(esNuevo ? "Registrar Nuevo Almacén" : "Editar Almacén");
        titulo.getStyle().set("margin", "0 0 16px 0");

        HorizontalLayout primeraFila = new HorizontalLayout();
        primeraFila.setWidthFull();
        primeraFila.setSpacing(true);

        TextField txtNombre = new TextField("Nombre del Almacén");
        txtNombre.setWidthFull();
        txtNombre.setValue(almacen.getNombre() != null ? almacen.getNombre() : "");

        ComboBox<StatusEntidad> cbStatus = new ComboBox<>("Estado");
        cbStatus.setWidth("150px");
        cbStatus.setItems(StatusEntidad.values());
        cbStatus.setItemLabelGenerator(StatusEntidad::getEtiqueta);
        cbStatus.setValue(almacen.getStatus() != null ? almacen.getStatus() : StatusEntidad.ACTIVO);

        primeraFila.add(txtNombre, cbStatus);
        primeraFila.expand(txtNombre);

        TextField txtDireccion = new TextField("Dirección Físico-Descriptiva");
        txtDireccion.setWidthFull();
        txtDireccion.setValue(almacen.getDireccion() != null ? almacen.getDireccion() : "");

        NumberField numLatitud = new NumberField("Latitud");
        numLatitud.setWidthFull();
        numLatitud.setValue(almacen.getLatitud());
        numLatitud.setPlaceholder("Ej: 19.4500");

        NumberField numLongitud = new NumberField("Longitud");
        numLongitud.setWidthFull();
        numLongitud.setValue(almacen.getLongitud());
        numLongitud.setPlaceholder("Ej: -70.6167");

        HorizontalLayout layoutCoordenadas = new HorizontalLayout(numLatitud, numLongitud);
        layoutCoordenadas.setWidthFull();
        layoutCoordenadas.setSpacing(true);

        Button btnToggleMapa = new Button("Seleccionar en mapa", new Icon(VaadinIcon.MAP_MARKER));
        btnToggleMapa.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnToggleMapa.getStyle().set("margin-top", "5px");

        Span mapHint = new Span("Haz clic en el mapa para fijar la ubicación:");
        mapHint.getStyle().set("font-size", "13px").set("color", "gray");
        mapHint.setVisible(false);

        com.vaadin.flow.component.html.Div mapContainer = new com.vaadin.flow.component.html.Div();
        mapContainer.setWidthFull();
        mapContainer.setHeight("250px");
        mapContainer.setVisible(false);
        mapContainer.getStyle()
                .set("border-radius", "8px")
                .set("border", "1px solid #e0e0e0")
                .set("z-index", "1");

        double latInicial = almacen.getLatitud() != null ? almacen.getLatitud() : 19.428239;
        double lngInicial = almacen.getLongitud() != null ? almacen.getLongitud() : -70.629731;

        if (almacen.getLatitud() != null && almacen.getLongitud() != null) {
            mapContainer.setVisible(true);
            mapHint.setVisible(true);
            btnToggleMapa.setText("Ocultar mapa");
            btnToggleMapa.setIcon(new Icon(VaadinIcon.CHEVRON_UP));
        }

        mapContainer.addAttachListener(evt -> {
            String jsCode =
                    "const el = this;" +
                            "const lat = $0;" +
                            "const lng = $1;" +
                            "const latField = $2;" +
                            "const lngField = $3;" +
                            "function initMap() {" +
                            "  if(el._leaflet_map) return;" +
                            "  const map = L.map(el).setView([lat, lng], 14);" +
                            "  el._leaflet_map = map;" +
                            "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                            "    attribution: '© OpenStreetMap'" +
                            "  }).addTo(map);" +
                            "  const marker = L.marker([lat, lng]).addTo(map);" +
                            "  el._leaflet_marker = marker;" +
                            "  setTimeout(() => map.invalidateSize(), 300);" +
                            "  map.on('click', function(e) {" +
                            "    marker.setLatLng(e.latlng);" +
                            "    latField.value = e.latlng.lat.toFixed(6);" +
                            "    lngField.value = e.latlng.lng.toFixed(6);" +
                            "    latField.dispatchEvent(new Event('change'));" +
                            "    lngField.dispatchEvent(new Event('change'));" +
                            "  });" +
                            "}" +
                            "if (!window.L) {" +
                            "  const css = document.createElement('link'); css.rel = 'stylesheet'; css.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'; document.head.appendChild(css);" +
                            "  const script = document.createElement('script'); script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'; script.onload = initMap; document.head.appendChild(script);" +
                            "} else {" +
                            "  initMap();" +
                            "}";

            mapContainer.getElement().executeJs(jsCode, latInicial, lngInicial, numLatitud.getElement(), numLongitud.getElement());
        });

        btnToggleMapa.addClickListener(e -> {
            boolean hacerVisible = !mapContainer.isVisible();
            mapContainer.setVisible(hacerVisible);
            mapHint.setVisible(hacerVisible);

            btnToggleMapa.setText(hacerVisible ? "Ocultar mapa" : "Seleccionar en mapa");
            btnToggleMapa.setIcon(hacerVisible ? new Icon(VaadinIcon.CHEVRON_UP) : new Icon(VaadinIcon.MAP_MARKER));

            if (hacerVisible) {
                mapContainer.getElement().executeJs(
                        "if (this._leaflet_map) { " +
                                "  setTimeout(() => { this._leaflet_map.invalidateSize(); this._leaflet_map.setView([$0, $1]); }, 150); " +
                                "}",
                        numLatitud.getValue() != null ? numLatitud.getValue() : latInicial,
                        numLongitud.getValue() != null ? numLongitud.getValue() : lngInicial
                );
            }
        });

        numLatitud.addValueChangeListener(e -> {
            if (e.isFromClient() && numLatitud.getValue() != null && numLongitud.getValue() != null) {
                mapContainer.getElement().executeJs(
                        "if(this._leaflet_map) { this._leaflet_map.setView([$0, $1]); this._leaflet_marker.setLatLng([$0, $1]); }",
                        numLatitud.getValue(), numLongitud.getValue()
                );
            }
        });

        numLongitud.addValueChangeListener(e -> {
            if (e.isFromClient() && numLatitud.getValue() != null && numLongitud.getValue() != null) {
                mapContainer.getElement().executeJs(
                        "if(this._leaflet_map) { this._leaflet_map.setView([$0, $1]); this._leaflet_marker.setLatLng([$0, $1]); }",
                        numLatitud.getValue(), numLongitud.getValue()
                );
            }
        });


        Anchor linkMaps = new Anchor();
        linkMaps.setText("Abrir externamente en Google Maps");
        linkMaps.setTarget("_blank");
        linkMaps.getStyle().set("font-size", "14px").set("color", "#0066cc").set("font-weight", "500").set("margin-top", "5px");

        Runnable actualizarLink = () -> {
            if (numLatitud.getValue() != null && numLongitud.getValue() != null) {
                linkMaps.setHref("https://www.google.com/maps?q=" + numLatitud.getValue() + "," + numLongitud.getValue());
                linkMaps.setVisible(true);
            } else {
                linkMaps.setVisible(false);
            }
        };

        actualizarLink.run();
        numLatitud.addValueChangeListener(e -> actualizarLink.run());
        numLongitud.addValueChangeListener(e -> actualizarLink.run());

        Button btnGuardar = new Button(esNuevo ? "Crear Almacén" : "Guardar cambios", e -> {
            if (txtNombre.isEmpty() || txtDireccion.isEmpty()) {
                mostrarError("El nombre y la dirección son obligatorios.");
                return;
            }

            try {
                almacen.setNombre(txtNombre.getValue().trim());
                almacen.setDireccion(txtDireccion.getValue().trim());
                almacen.setStatus(cbStatus.getValue());
                almacen.setLatitud(numLatitud.getValue());
                almacen.setLongitud(numLongitud.getValue());

                almacenService.guardar(almacen);

                dialog.close();
                crudAlmacen.refreshGrid();

                Notification notif = Notification.show(
                        esNuevo ? "Almacén registrado exitosamente" : "Almacén actualizado correctamente",
                        3500, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                mostrarError("Error al guardar: Asegúrese de que el nombre no esté duplicado.");
            }
        });
        btnGuardar.addClassName("btn-nuevo");
        btnGuardar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "16px");

        mapHint.getStyle().set("font-size", "13px").set("color", "gray");

        VerticalLayout contenido = new VerticalLayout(titulo, primeraFila, txtDireccion, layoutCoordenadas, btnToggleMapa, mapHint, mapContainer, linkMaps, botones);
        contenido.setPadding(true);
        contenido.setSpacing(true);

        dialog.add(contenido);
        dialog.open();
    }

    private void dialogVerStock(Almacen almacen) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setCloseOnOutsideClick(true);

        H3 titulo = new H3("Inventario: " + almacen.getNombre());
        titulo.getStyle().set("margin", "0 0 16px 0");

        Grid<Inventario> gridInventario = new Grid<>(Inventario.class, false);
        gridInventario.addClassName("almacen-grid");
        gridInventario.addThemeNames("row-stripes");
        gridInventario.setHeight("400px");

        gridInventario.addComponentColumn(inv -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            Image avatar = new Image();
            avatar.setWidth("36px");
            avatar.setHeight("36px");
            avatar.getStyle().set("object-fit", "cover").set("border-radius", "8px");

            Producto producto = inv.getLote().getProducto();
            if (producto != null && producto.getFoto() != null && producto.getFoto().length > 0) {
                String base64 = Base64.getEncoder().encodeToString(producto.getFoto());
                avatar.setSrc("data:image/jpeg;base64," + base64);
            }

            Span nombreSpan = new Span(producto != null ? producto.getNombre() : "Desconocido");
            nombreSpan.getStyle().set("font-weight", "500");

            layout.add(avatar, nombreSpan);
            return layout;
        }).setHeader("Producto").setFlexGrow(1).setAutoWidth(true);

        gridInventario.addColumn(inv -> inv.getLote().getNumeroLote())
                .setHeader("N° Lote").setWidth("100px").setFlexGrow(0);

        gridInventario.addColumn(inv ->
                inv.getLote().getFechaVencimiento() != null ?
                        inv.getLote().getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"
        ).setHeader("Vencimiento").setWidth("140px").setFlexGrow(0);

        gridInventario.addColumn(inv -> {
                    Producto p = inv.getLote().getProducto();
                    return FormatoInventarioUtil.formatearCantidad(
                            inv.getCantidadActual(),
                            p.getContenidoPorEmpaque(),
                            Boolean.TRUE.equals(p.getPermiteFraccionamiento()),
                            false,
                            FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(p),
                            FormatoInventarioUtil.getNombreUnidadFraccionSafe(p)
                    );
                }).setHeader("Cantidad Actual").setWidth("170px").setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);

        List<Inventario> existencias = inventarioService.listarPorAlmacen(almacen);
        gridInventario.setItems(existencias);

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout footer = new HorizontalLayout(btnCerrar);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout contenido = new VerticalLayout(titulo, gridInventario, footer);
        contenido.setPadding(true);
        contenido.setSpacing(true);

        dialog.add(contenido);
        dialog.open();
    }

    private void mostrarError(String mensaje) {
        Notification notif = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
