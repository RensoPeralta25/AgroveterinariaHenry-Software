package com.agroveterinaria.view.Venta;

import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.TipoCliente;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.MetodoPago;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.ClienteService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.ProductoService;
import com.agroveterinaria.service.VentaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class VentaView extends VerticalLayout {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "DO"));

    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final EmpleadoService empleadoService;
    private final ProductoService productoService;

    private final ComboBox<Cliente> clienteExistente = new ComboBox<>("Buscar cliente");
    private final TextField cedulaCliente = new TextField("Cedula");
    private final TextField nombreCliente = new TextField("Nombre");
    private final TextField telefonoCliente = new TextField("Telefono");
    private final TextField direccionCliente = new TextField("Direccion");
    private final ComboBox<TipoCliente> tipoCliente = new ComboBox<>("Tipo de cliente");
    private final ComboBox<Empleado> vendedor = new ComboBox<>("Vendedor");
    private final TextField comprobanteFiscal = new TextField("Comprobante fiscal");
    private final DatePicker fechaVencimientoPago = new DatePicker("Vencimiento de pago");
    private final Checkbox llevaDespacho = new Checkbox("Lleva despacho");

    private final ComboBox<Producto> producto = new ComboBox<>("Producto");
    private final BigDecimalField cantidad = new BigDecimalField("Cantidad");
    private final BigDecimalField impuesto = new BigDecimalField("Impuesto");
    private final BigDecimalField descuento = new BigDecimalField("Descuento");
    private final BigDecimalField montoPagado = new BigDecimalField("Monto pagado");
    private final ComboBox<MetodoPago> metodoPago = new ComboBox<>("Metodo de pago");

    private final Grid<LineaVentaForm> gridLineas = new Grid<>(LineaVentaForm.class, false);
    private final List<LineaVentaForm> lineas = new ArrayList<>();

    private final Span subtotal = new Span(formatMoney(BigDecimal.ZERO));
    private final Span descuentoResumen = new Span(formatMoney(BigDecimal.ZERO));
    private final Span total = new Span(formatMoney(BigDecimal.ZERO));
    private final Span pagado = new Span(formatMoney(BigDecimal.ZERO));
    private final Span balance = new Span(formatMoney(BigDecimal.ZERO));
    private final Span estado = new Span("Pendiente");

    public VentaView(
            VentaService ventaService,
            ClienteService clienteService,
            EmpleadoService empleadoService,
            ProductoService productoService
    ) {
        this.ventaService = ventaService;
        this.clienteService = clienteService;
        this.empleadoService = empleadoService;
        this.productoService = productoService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("venta-view");

        configurarCampos();
        configurarGrid();

        HorizontalLayout workspace = new HorizontalLayout(crearFormulario(), crearResumen());
        workspace.setSizeFull();
        workspace.setPadding(false);
        workspace.setSpacing(true);
        workspace.expand(workspace.getComponentAt(0));

        add(workspace);
        expand(workspace);
    }

    private VerticalLayout crearFormulario() {
        H3 clienteTitulo = new H3("Cliente");
        Button nuevoCliente = new Button("Nuevo cliente", new Icon(VaadinIcon.PLUS), event -> limpiarCliente());
        nuevoCliente.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buscarFila = new HorizontalLayout(clienteExistente, nuevoCliente);
        buscarFila.setWidthFull();
        buscarFila.setAlignItems(FlexComponent.Alignment.END);
        buscarFila.expand(clienteExistente);

        HorizontalLayout clienteFila1 = new HorizontalLayout(cedulaCliente, nombreCliente);
        HorizontalLayout clienteFila2 = new HorizontalLayout(telefonoCliente, direccionCliente, tipoCliente);
        clienteFila1.setWidthFull();
        clienteFila2.setWidthFull();
        clienteFila1.expand(nombreCliente);
        clienteFila2.expand(direccionCliente);

        H3 ventaTitulo = new H3("Venta");
        HorizontalLayout ventaFila = new HorizontalLayout(vendedor, comprobanteFiscal, fechaVencimientoPago);
        ventaFila.setWidthFull();
        ventaFila.expand(vendedor);

        HorizontalLayout pagoFila = new HorizontalLayout(descuento, montoPagado, metodoPago, llevaDespacho);
        pagoFila.setWidthFull();
        pagoFila.setAlignItems(FlexComponent.Alignment.END);

        H3 productosTitulo = new H3("Productos");
        Button agregarProducto = new Button("Agregar", new Icon(VaadinIcon.PLUS), event -> agregarLinea());
        agregarProducto.addClassName("btn-nuevo");
        agregarProducto.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout productoFila = new HorizontalLayout(producto, cantidad, impuesto, agregarProducto);
        productoFila.setWidthFull();
        productoFila.setAlignItems(FlexComponent.Alignment.END);
        productoFila.expand(producto);

        Button guardar = new Button("Registrar venta", new Icon(VaadinIcon.CHECK), event -> registrarVenta());
        guardar.addClassName("btn-nuevo");
        guardar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button limpiar = new Button("Limpiar", new Icon(VaadinIcon.REFRESH), event -> limpiarFormulario());
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout acciones = new HorizontalLayout(guardar, limpiar);
        acciones.setWidthFull();
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout formulario = new VerticalLayout(
                clienteTitulo,
                buscarFila,
                clienteFila1,
                clienteFila2,
                productosTitulo,
                productoFila,
                gridLineas,
                ventaTitulo,
                ventaFila,
                pagoFila,
                acciones
        );
        formulario.setSizeFull();
        formulario.setPadding(true);
        formulario.setSpacing(true);
        formulario.expand(gridLineas);
        return formulario;
    }

    private VerticalLayout crearResumen() {
        VerticalLayout resumen = new VerticalLayout();
        resumen.setWidth("320px");
        resumen.setPadding(true);
        resumen.setSpacing(false);
        resumen.addClassName("venta-resumen");

        H3 titulo = new H3("Resumen");
        resumen.add(
                titulo,
                filaResumen("Subtotal", subtotal),
                filaResumen("Descuento", descuentoResumen),
                filaResumen("Total", total),
                filaResumen("Pagado", pagado),
                filaResumen("Balance", balance),
                filaResumen("Estado", estado)
        );
        return resumen;
    }

    private HorizontalLayout filaResumen(String etiqueta, Span valor) {
        Span label = new Span(etiqueta);
        label.getStyle().set("font-weight", "600");
        valor.getStyle().set("font-weight", "600");

        HorizontalLayout fila = new HorizontalLayout(label, valor);
        fila.setWidthFull();
        fila.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return fila;
    }

    private void configurarCampos() {
        clienteExistente.setItems(clienteService.findAll());
        clienteExistente.setItemLabelGenerator(this::nombreCliente);
        clienteExistente.setPlaceholder("Buscar por nombre o cedula...");
        clienteExistente.setClearButtonVisible(true);
        clienteExistente.addValueChangeListener(event -> cargarCliente(event.getValue()));

        cedulaCliente.setPlaceholder("000-0000000-0");
        telefonoCliente.setPlaceholder("000-000-0000");
        direccionCliente.setPlaceholder("Direccion del cliente");

        tipoCliente.setItems(clienteService.findTiposCliente());
        tipoCliente.setItemLabelGenerator(TipoCliente::getNombreTipoCliente);
        tipoCliente.addValueChangeListener(event -> actualizarResumen());

        vendedor.setItems(empleadoService.findVendedores());
        vendedor.setItemLabelGenerator(this::nombreEmpleado);

        producto.setItems(productoService.listarTodos().stream()
                .filter(item -> item.getStatus() == StatusEntidad.ACTIVO)
                .toList());
        producto.setItemLabelGenerator(Producto::getNombre);

        cantidad.setValue(BigDecimal.ONE);
        cantidad.setValueChangeMode(ValueChangeMode.EAGER);

        impuesto.setValue(BigDecimal.ZERO);
        impuesto.setPrefixComponent(new Span("RD$"));

        descuento.setValue(BigDecimal.ZERO);
        descuento.setPrefixComponent(new Span("RD$"));
        descuento.setValueChangeMode(ValueChangeMode.EAGER);
        descuento.addValueChangeListener(event -> actualizarResumen());

        montoPagado.setValue(BigDecimal.ZERO);
        montoPagado.setPrefixComponent(new Span("RD$"));
        montoPagado.setValueChangeMode(ValueChangeMode.EAGER);
        montoPagado.addValueChangeListener(event -> actualizarResumen());

        metodoPago.setItems(MetodoPago.values());
        metodoPago.setItemLabelGenerator(MetodoPago::getEtiqueta);
    }

    private void configurarGrid() {
        gridLineas.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        gridLineas.setHeight("320px");

        gridLineas.addColumn(linea -> linea.getProducto().getNombre())
                .setHeader("Producto")
                .setFlexGrow(1);
        gridLineas.addColumn(linea -> linea.getCantidad().toPlainString())
                .setHeader("Cantidad")
                .setWidth("110px")
                .setFlexGrow(0);
        gridLineas.addColumn(linea -> formatMoney(linea.getPrecioUnitario()))
                .setHeader("Precio")
                .setWidth("130px")
                .setFlexGrow(0);
        gridLineas.addColumn(linea -> formatMoney(linea.getImpuesto()))
                .setHeader("Impuesto")
                .setWidth("130px")
                .setFlexGrow(0);
        gridLineas.addColumn(linea -> formatMoney(linea.getSubtotal()))
                .setHeader("Subtotal")
                .setWidth("140px")
                .setFlexGrow(0);
        gridLineas.addComponentColumn(linea -> {
            Button eliminar = new Button(new Icon(VaadinIcon.TRASH));
            eliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            eliminar.setAriaLabel("Eliminar producto");
            eliminar.addClickListener(event -> {
                lineas.remove(linea);
                refrescarLineas();
            });
            return eliminar;
        }).setHeader("").setWidth("70px").setFlexGrow(0);

        refrescarLineas();
    }

    private void cargarCliente(Cliente cliente) {
        if (cliente == null) {
            return;
        }
        cedulaCliente.setValue(cliente.getPersona() != null ? cliente.getPersona().getCedula() : "");
        nombreCliente.setValue(cliente.getPersona() != null ? cliente.getPersona().getNombre() : "");
        telefonoCliente.setValue(cliente.getPersona() != null ? cliente.getPersona().getTelefono() : "");
        direccionCliente.setValue(cliente.getPersona() != null ? cliente.getPersona().getDireccion() : "");
        tipoCliente.setValue(cliente.getTipoCliente());
    }

    private void limpiarCliente() {
        clienteExistente.clear();
        cedulaCliente.clear();
        nombreCliente.clear();
        telefonoCliente.clear();
        direccionCliente.clear();
        tipoCliente.clear();
    }

    private void agregarLinea() {
        if (producto.getValue() == null) {
            mostrarError("Debes seleccionar un producto.");
            return;
        }
        if (cantidad.getValue() == null || cantidad.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            mostrarError("La cantidad debe ser mayor que cero.");
            return;
        }

        try {
            LineaVentaForm linea = new LineaVentaForm(
                    producto.getValue(),
                    cantidad.getValue().setScale(2, RoundingMode.HALF_UP),
                    impuesto.getValue() != null ? impuesto.getValue().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO
            );
            lineas.add(linea);
            producto.clear();
            cantidad.setValue(BigDecimal.ONE);
            impuesto.setValue(BigDecimal.ZERO);
            refrescarLineas();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void registrarVenta() {
        try {
            Venta venta = ventaService.registrarVenta(crearSolicitud());
            Notification notification = Notification.show(
                    "Venta #" + venta.getIdVenta() + " registrada como " + venta.getEstado().getEtiqueta(),
                    3500,
                    Notification.Position.BOTTOM_END
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            limpiarFormulario();
        } catch (Exception ex) {
            mostrarError(ex.getMessage());
        }
    }

    private VentaService.SolicitudVenta crearSolicitud() {
        Cliente cliente = clienteExistente.getValue();
        TipoCliente tipo = tipoCliente.getValue();
        Empleado emp = vendedor.getValue();

        return new VentaService.SolicitudVenta(
                new VentaService.ClienteVentaRequest(
                        cliente != null ? cliente.getIdCliente() : null,
                        cedulaCliente.getValue(),
                        nombreCliente.getValue(),
                        telefonoCliente.getValue(),
                        direccionCliente.getValue(),
                        tipo != null ? tipo.getIdTipoCliente() : null
                ),
                emp != null ? emp.getIdEmpleado() : null,
                llevaDespacho.getValue(),
                fechaVencimientoPago.getValue(),
                comprobanteFiscal.getValue(),
                descuento.getValue(),
                montoPagado.getValue(),
                metodoPago.getValue(),
                lineas.stream()
                        .map(linea -> new VentaService.LineaVentaRequest(
                                linea.getProducto().getIdProducto(),
                                linea.getCantidad(),
                                linea.getImpuesto()
                        ))
                        .toList()
        );
    }

    private void refrescarLineas() {
        gridLineas.setItems(lineas);
        actualizarResumen();
    }

    private void actualizarResumen() {
        if (lineas.isEmpty()) {
            subtotal.setText(formatMoney(BigDecimal.ZERO));
            descuentoResumen.setText(formatMoney(BigDecimal.ZERO));
            total.setText(formatMoney(BigDecimal.ZERO));
            pagado.setText(formatMoney(BigDecimal.ZERO));
            balance.setText(formatMoney(BigDecimal.ZERO));
            estado.setText("Pendiente");
            return;
        }

        try {
            VentaService.ResumenVenta resumen = ventaService.calcularResumen(crearSolicitud());
            subtotal.setText(formatMoney(resumen.subtotal()));
            descuentoResumen.setText(formatMoney(resumen.descuento()));
            total.setText(formatMoney(resumen.total()));
            pagado.setText(formatMoney(resumen.montoPagado()));
            balance.setText(formatMoney(resumen.balancePendiente()));
            estado.setText(resumen.estado().getEtiqueta());
        } catch (Exception ignored) {
            subtotal.setText(formatMoney(lineas.stream()
                    .map(LineaVentaForm::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
            descuentoResumen.setText(formatMoney(BigDecimal.ZERO));
            total.setText(subtotal.getText());
            pagado.setText(formatMoney(BigDecimal.ZERO));
            balance.setText(subtotal.getText());
            estado.setText("Pendiente");
        }
    }

    private void limpiarFormulario() {
        limpiarCliente();
        vendedor.clear();
        comprobanteFiscal.clear();
        fechaVencimientoPago.clear();
        llevaDespacho.setValue(false);
        descuento.setValue(BigDecimal.ZERO);
        montoPagado.setValue(BigDecimal.ZERO);
        metodoPago.clear();
        producto.clear();
        cantidad.setValue(BigDecimal.ONE);
        impuesto.setValue(BigDecimal.ZERO);
        lineas.clear();
        refrescarLineas();
        clienteExistente.setItems(clienteService.findAll());
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String nombreEmpleado(Empleado empleado) {
        if (empleado == null || empleado.getPersona() == null) {
            return "";
        }
        return empleado.getPersona().getNombre();
    }

    private String nombreCliente(Cliente cliente) {
        if (cliente == null || cliente.getPersona() == null) {
            return "";
        }
        return cliente.getPersona().getNombre() + " - " + cliente.getPersona().getCedula();
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value != null ? value : BigDecimal.ZERO);
    }

    private static class LineaVentaForm {
        private final Producto producto;
        private final BigDecimal cantidad;
        private final BigDecimal impuesto;

        LineaVentaForm(Producto producto, BigDecimal cantidad, BigDecimal impuesto) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.impuesto = impuesto;
        }

        Producto getProducto() {
            return producto;
        }

        BigDecimal getCantidad() {
            return cantidad;
        }

        BigDecimal getImpuesto() {
            return impuesto;
        }

        BigDecimal getPrecioUnitario() {
            boolean fraccionada = Boolean.TRUE.equals(producto.getPermiteFraccionamiento())
                    && cantidad.stripTrailingZeros().scale() > 0;
            BigDecimal precio = fraccionada ? producto.getPrecioFraccion() : producto.getPrecioEmpaque();
            if (precio == null) {
                throw new IllegalArgumentException("El producto no tiene precio configurado para esta venta.");
            }
            return precio.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal getSubtotal() {
            return getPrecioUnitario().multiply(cantidad).add(impuesto).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
