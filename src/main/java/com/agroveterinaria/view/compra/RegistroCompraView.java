package com.agroveterinaria.view.compra;

import com.agroveterinaria.dto.detalle_compra.DetalleCompraDTO;
import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.entity.DetalleCompra;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.CompraService;
import com.agroveterinaria.service.InventarioService;
import com.agroveterinaria.service.ProductoService;
import com.agroveterinaria.service.ProveedorService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
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
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@PageTitle("Registrar Compra")
@RolesAllowed("ADMINISTRADOR")
public class RegistroCompraView extends VerticalLayout {

    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final CompraService compraService;
    private final InventarioService inventarioService;

    private boolean cargandoBorrador = false;
    private Long idBorradorActual = null;

    private ComboBox<Proveedor> cbProveedor;
    private Grid<Producto> gridProductos;
    private Grid<DetalleCompraDTO> gridDetalles;
    private Span lblTotalGlobal;

    private final List<DetalleCompraDTO> carrito = new ArrayList<>();

    private Runnable accionVolver;

    public RegistroCompraView(ProveedorService proveedorService, ProductoService productoService,
                              CompraService compraService, InventarioService inventarioService) {
        this.proveedorService = proveedorService;
        this.productoService = productoService;
        this.compraService = compraService;
        this.inventarioService = inventarioService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Registro de Compra a Proveedor");
        titulo.getStyle().set("margin-top", "0");

        Button btnVolver = new Button("Volver a Compras", new Icon(VaadinIcon.ARROW_LEFT));
        btnVolver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnVolver.addClickListener(e -> {
            if (accionVolver != null) accionVolver.run();
        });

        VerticalLayout panelIzquierdo = construirPanelIzquierdo();
        VerticalLayout panelDerecho = construirPanelDerecho();

        SplitLayout splitLayout = new SplitLayout(panelIzquierdo, panelDerecho);
        splitLayout.setSplitterPosition(55);
        splitLayout.setSizeFull();

        add(btnVolver, titulo, splitLayout);
    }

    public void configurarVista(Long idBorrador, Runnable accionVolver) {
        this.idBorradorActual = idBorrador;
        this.accionVolver = accionVolver;

        if (idBorrador != null) {
            cargarBorrador(idBorrador);
        } else {
            this.cargandoBorrador = true;
            this.carrito.clear();
            actualizarTotal();
            if (this.gridDetalles != null) this.gridDetalles.getDataProvider().refreshAll();
            this.cargandoBorrador = false;
        }
    }

    private void cargarBorrador(Long id) {
        cargandoBorrador = true;

        compraService.buscarPorId(id).ifPresent(borrador -> {
            cbProveedor.setValue(borrador.getProveedor());
            carrito.clear();
            for (DetalleCompra dc : borrador.getDetalles()) {
                DetalleCompraDTO dto = new DetalleCompraDTO(dc.getProducto());
                dto.setCantidad(dc.getCantidad());
                dto.setCostoActual(dc.getPrecioUnitarioCompra());
                carrito.add(dto);
            }
            gridDetalles.getDataProvider().refreshAll();
            actualizarTotal();
        });

        cargandoBorrador = false;
    }

    private VerticalLayout construirPanelIzquierdo() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setSizeFull();
        layout.getStyle().set("padding-bottom", "15px");

        H3 lblProveedor = new H3("Proveedor");
        lblProveedor.getStyle().set("margin-bottom", "0");

        HorizontalLayout layoutProveedor = new HorizontalLayout();
        layoutProveedor.setWidthFull();
        layoutProveedor.setAlignItems(Alignment.CENTER);

        cbProveedor = new ComboBox<>();
        cbProveedor.setPlaceholder("Seleccione un proveedor...");
        cbProveedor.setWidthFull();
        cbProveedor.setItems(proveedorService.listarTodos().stream()
                .filter(p -> p.getStatus() == StatusEntidad.ACTIVO).toList());
        cbProveedor.setItemLabelGenerator(Proveedor::getNombre);
        cbProveedor.addValueChangeListener(e -> {
            gridProductos.getDataProvider().refreshAll();
            ejecutarAutoGuardadoSilencioso();
        });

        Button btnNuevoProveedor = new Button("Nuevo proveedor", new Icon(VaadinIcon.PLUS));
        btnNuevoProveedor.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnNuevoProveedor.addClickListener(e -> dialogNuevoProveedor());

        layoutProveedor.add(cbProveedor, btnNuevoProveedor);
        layoutProveedor.expand(cbProveedor);

        HorizontalLayout layoutBuscador = new HorizontalLayout();
        layoutBuscador.setWidthFull();
        layoutBuscador.setAlignItems(Alignment.CENTER);
        layoutBuscador.getStyle().set("margin-top", "15px");

        H3 lblProductos = new H3("Productos");
        lblProductos.getStyle().set("margin", "0");

        TextField txtBuscarProducto = new TextField();
        txtBuscarProducto.setPlaceholder("Buscar por nombre o código...");
        txtBuscarProducto.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtBuscarProducto.setValueChangeMode(ValueChangeMode.LAZY);
        txtBuscarProducto.setWidthFull();

        layoutBuscador.add(lblProductos, txtBuscarProducto);
        layoutBuscador.expand(txtBuscarProducto);

        gridProductos = new Grid<>(Producto.class, false);
        gridProductos.addClassName("compra-grid");
        gridProductos.addThemeNames("row-stripes");
        gridProductos.setSizeFull();

        gridProductos.addColumn(Producto::getNombre).setHeader("Nombre").setFlexGrow(2);

        gridProductos.addColumn(producto -> {
            BigDecimal stock = inventarioService.obtenerStockTotal(producto);
            return stock != null ? String.format("%,.2f", stock) : "0.00";
        }).setHeader("Stock Actual").setTextAlign(ColumnTextAlign.END).setFlexGrow(1);

        gridProductos.addColumn(producto -> {
            if (cbProveedor.getValue() == null) return "-";
            BigDecimal costoAnterior = compraService.obtenerUltimoCosto(producto, cbProveedor.getValue());
            return costoAnterior != null ? String.format("RD$ %,.2f", costoAnterior) : "-";
        }).setHeader("Costo Anterior").setTextAlign(ColumnTextAlign.END).setFlexGrow(1);

        txtBuscarProducto.addValueChangeListener(e -> {
            String filtro = e.getValue().toLowerCase().trim();
            gridProductos.setItems(productoService.listarTodos().stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(filtro) && p.getStatus() == StatusEntidad.ACTIVO)
                    .toList());
        });
        gridProductos.setItems(productoService.listarTodos().stream()
                .filter(p -> p.getStatus() == StatusEntidad.ACTIVO).toList());

        Button btnAgregar = new Button("Agregar producto a la compra", new Icon(VaadinIcon.ARROW_RIGHT));
        btnAgregar.setWidthFull();
        btnAgregar.getStyle().set("margin-top", "10px").set("height", "50px").set("font-size", "16px");
        btnAgregar.addClickListener(e -> {
            Producto seleccionado = gridProductos.asSingleSelect().getValue();
            if (seleccionado == null) {
                mostrarError("Debes seleccionar un producto de la tabla.");
                return;
            }
            agregarAlCarrito(seleccionado);
        });

        layout.add(lblProveedor, layoutProveedor, layoutBuscador, gridProductos, btnAgregar);

        layout.expand(gridProductos);

        return layout;
    }

    private VerticalLayout construirPanelDerecho() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setSizeFull();
        layout.getStyle()
                .set("background-color", "#f8f9fa")
                .set("padding", "15px")
                .set("border-radius", "8px")
                .set("box-sizing", "border-box");

        H3 titulo = new H3("Detalles de compra");
        titulo.getStyle().set("margin-top", "0");

        gridDetalles = new Grid<>(DetalleCompraDTO.class, false);
        gridDetalles.addClassName("compra-grid");
        gridDetalles.setSizeFull();

        gridDetalles.addColumn(item -> item.getProducto().getNombre())
                .setHeader("Producto").setFlexGrow(2);

        gridDetalles.addComponentColumn(item -> {
            BigDecimalField txtCantidad = new BigDecimalField();
            txtCantidad.setWidthFull();
            txtCantidad.addThemeName("small");
            txtCantidad.setValue(item.getCantidad());
            txtCantidad.addThemeName("align-right");
            txtCantidad.setValueChangeMode(ValueChangeMode.ON_BLUR);
            txtCantidad.addValueChangeListener(e -> {
                if (e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    item.setCantidad(e.getValue());
                    gridDetalles.getDataProvider().refreshItem(item);
                    actualizarTotal();
                } else {
                    txtCantidad.setValue(BigDecimal.ONE);
                }
                ejecutarAutoGuardadoSilencioso();
            });
            return txtCantidad;
        }).setHeader("Cantidad").setWidth("110px").setFlexGrow(0);

        gridDetalles.addComponentColumn(item -> {
            BigDecimalField txtCosto = new BigDecimalField();
            txtCosto.setWidthFull();
            txtCosto.addThemeName("small");
            txtCosto.setPrefixComponent(new Span("RD$"));
            txtCosto.setValue(item.getCostoActual());
            txtCosto.addThemeName("align-right");
            txtCosto.setValueChangeMode(ValueChangeMode.ON_BLUR);
            txtCosto.addValueChangeListener(e -> {
                if (e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) >= 0) {
                    item.setCostoActual(e.getValue());
                    gridDetalles.getDataProvider().refreshItem(item);
                    actualizarTotal();
                } else {
                    txtCosto.setValue(BigDecimal.ZERO);
                }
                ejecutarAutoGuardadoSilencioso();
            });
            return txtCosto;
        }).setHeader("Costo actual").setWidth("140px").setFlexGrow(0);

        gridDetalles.addColumn(item -> String.format("RD$ %,.2f", item.getSubtotal()))
                .setHeader("Subtotal").setTextAlign(ColumnTextAlign.END).setFlexGrow(0);

        gridDetalles.addComponentColumn(item -> {
            Button btnQuitar = new Button(new Icon(VaadinIcon.CLOSE));
            btnQuitar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnQuitar.addClickListener(e -> {
                carrito.remove(item);
                gridDetalles.getDataProvider().refreshAll();
                actualizarTotal();
                ejecutarAutoGuardadoSilencioso();
            });
            return btnQuitar;
        }).setWidth("60px").setFlexGrow(0);

        gridDetalles.setItems(carrito);

        HorizontalLayout layoutTotal = new HorizontalLayout();
        layoutTotal.setWidthFull();
        layoutTotal.setJustifyContentMode(JustifyContentMode.END);
        layoutTotal.setAlignItems(Alignment.CENTER);

        Span lblTextoTotal = new Span("Total: ");
        lblTextoTotal.getStyle().set("font-size", "18px").set("font-weight", "bold");

        lblTotalGlobal = new Span("RD$ 0.00");
        lblTotalGlobal.getStyle().set("font-size", "22px").set("font-weight", "bold").set("color", "#002899");

        layoutTotal.add(lblTextoTotal, lblTotalGlobal);

        Button btnProcesar = new Button("Procesar compra", new Icon(VaadinIcon.CHECK_CIRCLE));
        btnProcesar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnProcesar.setWidthFull();
        btnProcesar.getStyle().set("margin-top", "10px").set("height", "50px").set("font-size", "16px");
        btnProcesar.addClickListener(e -> procesarCompra());

        layout.add(titulo, gridDetalles, layoutTotal, btnProcesar);

        layout.expand(gridDetalles);

        return layout;
    }


    private void agregarAlCarrito(Producto producto) {
        for (DetalleCompraDTO item : carrito) {
            if (item.getProducto().getIdProducto().equals(producto.getIdProducto())) {
                item.setCantidad(item.getCantidad().add(BigDecimal.ONE));
                gridDetalles.getDataProvider().refreshItem(item);
                actualizarTotal();
                ejecutarAutoGuardadoSilencioso();
                return;
            }
        }

        DetalleCompraDTO nuevoItem = new DetalleCompraDTO(producto);

        if (cbProveedor.getValue() != null) {
            BigDecimal ultimoCosto = compraService.obtenerUltimoCosto(producto, cbProveedor.getValue());
            if (ultimoCosto != null) {
                nuevoItem.setCostoActual(ultimoCosto);
            }
        }

        carrito.add(nuevoItem);
        gridDetalles.getDataProvider().refreshAll();
        actualizarTotal();
        ejecutarAutoGuardadoSilencioso();
    }

    private void actualizarTotal() {
        BigDecimal total = carrito.stream()
                .map(DetalleCompraDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalGlobal.setText(String.format("RD$ %,.2f", total));
    }

    private void procesarCompra() {
        if (cbProveedor.getValue() == null) {
            mostrarError("Debes seleccionar un proveedor antes de procesar.");
            return;
        }
        if (carrito.isEmpty()) {
            mostrarError("El carrito está vacío. Agrega al menos un producto.");
            return;
        }

        mostrarDialogoConfirmacion();
    }

    private void mostrarDialogoConfirmacion() {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3("Confirmar Compra");
        titulo.getStyle().set("margin", "0 0 10px 0");

        Span mensaje = new Span(String.format("¿Estás seguro de registrar esta compra a %s por un total de %s?",
                cbProveedor.getValue().getNombre(), lblTotalGlobal.getText()));

        Button btnConfirmar = new Button("Sí, registrar compra", e -> {
            try {
                if (idBorradorActual != null) {
                    compraService.confirmarBorradorComoPendiente(idBorradorActual);
                } else {
                    compraService.registrarCompra(cbProveedor.getValue(), carrito);
                }
                Notification notif = Notification.show("Compra registrada y enviada a recepción", 4000, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                carrito.clear();
                idBorradorActual = null;
                gridDetalles.getDataProvider().refreshAll();
                cbProveedor.clear();
                actualizarTotal();
                accionVolver.run();
            } catch (Exception ex) {
                mostrarError("Ocurrió un error al procesar la compra: " + ex.getMessage());
                dialog.close();
            }
        });
        btnConfirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout layoutBotones = new HorizontalLayout(btnConfirmar, btnCancelar);
        layoutBotones.getStyle().set("margin-top", "20px");

        VerticalLayout layoutContenido = new VerticalLayout(titulo, mensaje, layoutBotones);
        layoutContenido.setPadding(true);
        layoutContenido.setSpacing(false);
        layoutContenido.setAlignItems(Alignment.CENTER);

        dialog.add(layoutContenido);
        dialog.open();
    }


    private void dialogNuevoProveedor() {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3("Registrar Nuevo Proveedor");
        titulo.getStyle().set("margin", "0 0 16px 0");

        TextField txtRnc = new TextField("RNC / Cédula");
        txtRnc.setWidthFull();
        txtRnc.setPlaceholder("Ej: 031-0000000-0");
        txtRnc.setAllowedCharPattern("[0-9\\-]");
        txtRnc.setValueChangeMode(ValueChangeMode.EAGER);
        txtRnc.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null) {
                String raw = e.getValue().replaceAll("[^0-9]", "");
                if (raw.length() > 11) raw = raw.substring(0, 11);

                StringBuilder formatted = new StringBuilder();
                if (raw.length() > 9) {
                    for (int i = 0; i < raw.length(); i++) {
                        if (i == 3 || i == 10) formatted.append("-");
                        formatted.append(raw.charAt(i));
                    }
                } else {
                    for (int i = 0; i < raw.length(); i++) {
                        if (i == 1 || i == 3 || i == 8) formatted.append("-");
                        formatted.append(raw.charAt(i));
                    }
                }

                if (!e.getValue().equals(formatted.toString())) {
                    txtRnc.setValue(formatted.toString());
                }
            }
        });

        TextField txtNombre = new TextField("Nombre del proveedor");
        txtNombre.setWidthFull();
        txtNombre.setPlaceholder("Ej: Distribuidora Agrovet");

        TextField txtDireccion = new TextField("Dirección");
        txtDireccion.setWidthFull();

        HorizontalLayout filaTelefonos = new HorizontalLayout();
        filaTelefonos.setWidthFull();

        TextField txtTelefono = new TextField("Teléfono");
        txtTelefono.setWidthFull();
        txtTelefono.setPlaceholder("000-000-0000");
        txtTelefono.setAllowedCharPattern("[0-9\\-]");
        txtTelefono.setValueChangeMode(ValueChangeMode.EAGER);
        txtTelefono.addValueChangeListener(e -> autoFormatearTelefono(e.getValue(), txtTelefono, e.isFromClient()));

        TextField txtContacto = new TextField("Número de contacto (Opcional)");
        txtContacto.setWidthFull();
        txtContacto.setPlaceholder("000-000-0000");
        txtContacto.setAllowedCharPattern("[0-9\\-]");
        txtContacto.setValueChangeMode(ValueChangeMode.EAGER);
        txtContacto.addValueChangeListener(e -> autoFormatearTelefono(e.getValue(), txtContacto, e.isFromClient()));

        filaTelefonos.add(txtTelefono, txtContacto);

        ComboBox<StatusEntidad> cbStatus = new ComboBox<>("Estado");
        cbStatus.setWidthFull();
        cbStatus.setItems(StatusEntidad.values());
        cbStatus.setItemLabelGenerator(StatusEntidad::getEtiqueta);
        cbStatus.setValue(StatusEntidad.ACTIVO);
        cbStatus.setEnabled(false);

        Button btnGuardar = new Button("Agregar", e -> {
            if (txtNombre.isEmpty() || txtRnc.isEmpty() || txtTelefono.isEmpty()) {
                mostrarError("Los campos Nombre, RNC y Teléfono son obligatorios.");
                return;
            }

            String rncLimpio = txtRnc.getValue().replaceAll("[^0-9]", "");
            if (rncLimpio.length() != 9 && rncLimpio.length() != 11) {
                mostrarError("El RNC debe tener 9 dígitos o la Cédula 11 dígitos.");
                return;
            }

            String telLimpio = txtTelefono.getValue().replaceAll("[^0-9]", "");
            if (telLimpio.length() != 10) {
                mostrarError("El Teléfono debe tener exactamente 10 dígitos.");
                return;
            }

            String contactoFinal;
            if (txtContacto.isEmpty()) {
                contactoFinal = txtTelefono.getValue();
            } else {
                String contLimpio = txtContacto.getValue().replaceAll("[^0-9]", "");
                if (contLimpio.length() != 10) {
                    mostrarError("El Número de contacto debe tener exactamente 10 dígitos.");
                    return;
                }
                contactoFinal = txtContacto.getValue();
            }

            String nombreFinal = txtNombre.getValue().trim();
            if (!nombreFinal.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                mostrarError("El nombre no debe contener números ni puntos (ej. quite S.A. o S.R.L.)");
                return;
            }

            try {
                Proveedor nuevoProveedor = new Proveedor();
                nuevoProveedor.setRnc(rncLimpio);
                nuevoProveedor.setNombre(nombreFinal);
                nuevoProveedor.setDireccion(txtDireccion.getValue() != null ? txtDireccion.getValue().trim() : "Sin dirección");

                nuevoProveedor.setTelefono(txtTelefono.getValue().trim());
                nuevoProveedor.setNumPersonaContacto(contactoFinal.trim());
                nuevoProveedor.setStatus(cbStatus.getValue());

                Proveedor proveedorGuardado = proveedorService.guardar(nuevoProveedor);

                cbProveedor.setItems(proveedorService.listarTodos().stream()
                        .filter(p -> p.getStatus() == StatusEntidad.ACTIVO).toList());
                cbProveedor.setValue(proveedorGuardado);

                dialog.close();
                Notification notif = Notification.show("Proveedor registrado exitosamente", 3500, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                mostrarError("Error al guardar: " + ex.getMessage());
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "16px");

        VerticalLayout contenido = new VerticalLayout(titulo, txtRnc, txtNombre, txtDireccion, filaTelefonos, cbStatus, botones);
        contenido.setPadding(true);
        contenido.setSpacing(true);

        dialog.add(contenido);
        dialog.open();
    }

    private void autoFormatearTelefono(String valorActual, TextField campoTexto, boolean isFromClient) {
        if (isFromClient && valorActual != null) {
            String raw = valorActual.replaceAll("[^0-9]", "");
            if (raw.length() > 10) raw = raw.substring(0, 10);

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < raw.length(); i++) {
                if (i == 3 || i == 6) formatted.append("-");
                formatted.append(raw.charAt(i));
            }

            if (!valorActual.equals(formatted.toString())) {
                campoTexto.setValue(formatted.toString());
            }
        }
    }

    private void mostrarError(String mensaje) {
        Notification notif = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void ejecutarAutoGuardadoSilencioso() {
        if (cargandoBorrador) {
            return;
        }
        if (cbProveedor.getValue() != null && (!carrito.isEmpty() || idBorradorActual != null)) {
            Compra borradorGuardado = compraService.guardarBorradorSilencioso(idBorradorActual, cbProveedor.getValue(), carrito);
            idBorradorActual = borradorGuardado.getIdCompra();
        }
    }

}