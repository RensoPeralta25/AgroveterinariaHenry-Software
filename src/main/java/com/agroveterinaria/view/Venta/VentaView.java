package com.agroveterinaria.view.Venta;

import com.agroveterinaria.component.CantidadFraccionadaField;
import com.agroveterinaria.component.DatosTransferenciaForm;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.EstrategiaPrecioVenta;
import com.agroveterinaria.enums.MetodoPago;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.*;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
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
    private final AlmacenService almacenService;
    private final LoteService loteService;
    private final CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService;
    private final FacturaVentaTermicaPdfService facturaVentaTermicaPdfService;

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
    private final Checkbox chkImprimirTicket = new Checkbox("Imprimir ticket térmico", true);

    private final ComboBox<Producto> producto = new ComboBox<>("Producto");
    private final ComboBox<Almacen> cbAlmacen = new ComboBox<>("Almacén Origen");
    private final Checkbox chkLoteAutomatico = new Checkbox("Lote Auto. (PEPS)");
    private final ComboBox<Lote> cbLote = new ComboBox<>("Lote Específico");

    private final ComboBox<String> modoIngreso = new ComboBox<>("Ingreso por");
    private final BigDecimalField montoIngresado = new BigDecimalField("Monto a convertir");
    private final ComboBox<String> estrategiaPrecio = new ComboBox<>("Estrategia de Precio");

    private final CantidadFraccionadaField cantidad = new CantidadFraccionadaField();
    private final BigDecimalField impuesto = new BigDecimalField("Impuesto");
    private final BigDecimalField descuento = new BigDecimalField("Descuento");
    private final BigDecimalField descuentoPorcentaje = new BigDecimalField("Descuento (%)");
    private boolean calculandoDescuento = false;
    private final BigDecimalField montoPagado = new BigDecimalField("Monto pagado");
    private final ComboBox<MetodoPago> metodoPago = new ComboBox<>("Metodo de pago");
    private final DatosTransferenciaForm datosTransferencia = new DatosTransferenciaForm();

    private final BigDecimalField costoEnvio = new BigDecimalField("Costo envío");
    private final Span transporteResumen = new Span(formatMoney(BigDecimal.ZERO));

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
            ProductoService productoService,
            AlmacenService almacenService,
            LoteService loteService,
            CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService,
            FacturaVentaTermicaPdfService facturaVentaTermicaPdfService
    ) {
        this.ventaService = ventaService;
        this.clienteService = clienteService;
        this.empleadoService = empleadoService;
        this.productoService = productoService;
        this.almacenService = almacenService;
        this.loteService = loteService;
        this.cuentaBancariaTransferenciaPdfService = cuentaBancariaTransferenciaPdfService;
        this.facturaVentaTermicaPdfService = facturaVentaTermicaPdfService;

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

        HorizontalLayout pagoFila = new HorizontalLayout(descuentoPorcentaje, descuento, costoEnvio, montoPagado, metodoPago, llevaDespacho);
        descuentoPorcentaje.setWidth("130px");
        descuento.setWidth("150px");
        pagoFila.setWidthFull();
        pagoFila.setAlignItems(FlexComponent.Alignment.END);

        H3 productosTitulo = new H3("Productos");

        HorizontalLayout fraccionamientoFila = new HorizontalLayout(modoIngreso, montoIngresado, estrategiaPrecio);
        fraccionamientoFila.setWidthFull();
        fraccionamientoFila.setAlignItems(FlexComponent.Alignment.END);

        Button agregarProducto = new Button("Agregar", new Icon(VaadinIcon.PLUS), event -> agregarLinea());
        agregarProducto.addClassName("btn-nuevo");
        agregarProducto.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout productoFila = new HorizontalLayout(
                producto,
                cbAlmacen,
                chkLoteAutomatico,
                cbLote,
                cantidad,
                impuesto,
                agregarProducto
        );
        productoFila.setWidthFull();
        productoFila.setAlignItems(FlexComponent.Alignment.END);
        productoFila.expand(producto);
        cbAlmacen.setWidth("140px");
        cbLote.setWidth("130px");
        impuesto.setWidth("110px");
        impuesto.setMinWidth("110px");
        impuesto.getStyle().set("flex-shrink", "0");
        chkLoteAutomatico.getStyle().set("padding-bottom", "10px");
        cantidad.getStyle().set("margin-bottom", "-8px");

        Button guardar = new Button("Registrar venta", new Icon(VaadinIcon.CHECK), event -> registrarVenta());
        guardar.addClassName("btn-nuevo");
        guardar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button limpiar = new Button("Limpiar", new Icon(VaadinIcon.REFRESH), event -> limpiarFormulario());
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout opcionesImpresion = new HorizontalLayout(
                chkImprimirTicket,
                crearDescargaCuentaBancaria()
        );
        opcionesImpresion.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout accionesVenta = new HorizontalLayout(guardar, limpiar);
        accionesVenta.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout acciones = new HorizontalLayout(opcionesImpresion, accionesVenta);
        acciones.setWidthFull();
        acciones.setAlignItems(FlexComponent.Alignment.CENTER);
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout formulario = new VerticalLayout(
                clienteTitulo,
                buscarFila,
                clienteFila1,
                clienteFila2,
                productosTitulo,
                fraccionamientoFila,
                productoFila,
                gridLineas,
                ventaTitulo,
                ventaFila,
                pagoFila,
                datosTransferencia,
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
                filaResumen("Transporte", transporteResumen),
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
        tipoCliente.addValueChangeListener(event -> {
            TipoCliente tc = event.getValue();
            if (tc != null && tc.getDescuento() != null) {
                descuentoPorcentaje.setValue(tc.getDescuento());
            } else {
                descuentoPorcentaje.setValue(BigDecimal.ZERO);
            }
            actualizarResumen();
        });

        vendedor.setItems(empleadoService.findVendedores());
        vendedor.setItemLabelGenerator(this::nombreEmpleado);

        producto.setItems(productoService.listarTodos().stream()
                .filter(item -> item.getStatus() == StatusEntidad.ACTIVO)
                .toList());
        producto.setItemLabelGenerator(Producto::getNombre);

        cbAlmacen.setItems(almacenService.listarTodos().stream()
                .filter(a -> a.getStatus() == StatusEntidad.ACTIVO).toList());
        cbAlmacen.setItemLabelGenerator(Almacen::getNombre);
        cbAlmacen.setPlaceholder("Seleccione...");

        chkLoteAutomatico.setValue(true);
        chkLoteAutomatico.getStyle().set("padding-bottom", "10px");
        chkLoteAutomatico.getStyle().set("white-space", "nowrap");
        chkLoteAutomatico.setMinWidth("145px");
        cbLote.setEnabled(false);
        cbLote.setItemLabelGenerator(l -> l.getNumeroLote() != null ? l.getNumeroLote() : "Sin Lote");

        chkLoteAutomatico.addValueChangeListener(e -> {
            cbLote.setEnabled(!e.getValue());
            if (e.getValue()) cbLote.clear();
        });

        modoIngreso.setItems("Cantidad", "Monto (RD$)");
        modoIngreso.setValue("Cantidad");

        estrategiaPrecio.setItems(EstrategiaPrecioVenta.NORMAL.getEtiqueta(), EstrategiaPrecioVenta.TODO_PRECIO_EMPAQUE.getEtiqueta(), EstrategiaPrecioVenta.TODO_PRECIO_FRACCION.getEtiqueta());
        estrategiaPrecio.setValue(EstrategiaPrecioVenta.NORMAL.getEtiqueta());

        montoIngresado.setPrefixComponent(new Span("RD$"));
        montoIngresado.setVisible(false);
        montoIngresado.setValueChangeMode(ValueChangeMode.EAGER);

        modoIngreso.addValueChangeListener(e -> {
            boolean porMonto = "Monto (RD$)".equals(e.getValue());
            montoIngresado.setVisible(porMonto);
            cantidad.setEnabled(!porMonto);
            if (porMonto) montoIngresado.focus();
            calcularCantidadDesdeMonto();
        });

        montoIngresado.addValueChangeListener(e -> calcularCantidadDesdeMonto());
        estrategiaPrecio.addValueChangeListener(e -> {
            calcularCantidadDesdeMonto();
            actualizarImpuestoSugerido();
        });
        cantidad.addValueChangeListener(e -> actualizarImpuestoSugerido());

        Runnable actualizarLotes = () -> {
            if (producto.getValue() != null && cbAlmacen.getValue() != null) {
                cbLote.setItems(loteService.buscarPorProducto(producto.getValue()));
            } else {
                cbLote.clear();
                cbLote.setItems(new ArrayList<>());
            }
        };

        producto.addValueChangeListener(e -> {
            Producto p = e.getValue();
            if (p != null) {
                boolean esServicio = p.getCategoria() == CategoriaProducto.SERVICIO;
                boolean esFraccionable = Boolean.TRUE.equals(p.getPermiteFraccionamiento());

                cbAlmacen.setEnabled(!esServicio);
                chkLoteAutomatico.setEnabled(!esServicio);
                cbLote.setEnabled(!esServicio && !chkLoteAutomatico.getValue());

                modoIngreso.setVisible(esFraccionable);
                estrategiaPrecio.setVisible(esFraccionable);
                montoIngresado.setVisible(esFraccionable && "Monto (RD$)".equals(modoIngreso.getValue()));

                if (esServicio) {
                    cbAlmacen.clear();
                    cbLote.clear();
                }

                cantidad.configurarProducto(
                        p.getContenidoPorEmpaque(),
                        p.getPermiteFraccionamiento(),
                        false
                );
                cantidad.setValue(BigDecimal.ONE);
                montoIngresado.clear();
            } else {
                cantidad.clear();
                cbAlmacen.setEnabled(true);
                chkLoteAutomatico.setEnabled(true);
                modoIngreso.setVisible(false);
                estrategiaPrecio.setVisible(false);
                montoIngresado.setVisible(false);
            }
            actualizarLotes.run();
            actualizarImpuestoSugerido();
        });
        cbAlmacen.addValueChangeListener(e -> actualizarLotes.run());

        cantidad.setValue(BigDecimal.ONE);

        impuesto.setValue(BigDecimal.ZERO);
        impuesto.setPrefixComponent(new Span("RD$"));
        impuesto.setValueChangeMode(ValueChangeMode.EAGER);

        descuentoPorcentaje.setValue(BigDecimal.ZERO);
        descuentoPorcentaje.setSuffixComponent(new Span("%"));
        descuentoPorcentaje.setValueChangeMode(ValueChangeMode.EAGER);
        descuento.setValue(BigDecimal.ZERO);
        descuento.setPrefixComponent(new Span("RD$"));
        descuento.setValueChangeMode(ValueChangeMode.EAGER);
        descuentoPorcentaje.addValueChangeListener(e -> {
            if (calculandoDescuento) return;
            calculandoDescuento = true;

            BigDecimal subtotalActual = obtenerSubtotalActual();
            BigDecimal porc = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;

            BigDecimal montoAbsoluto = subtotalActual.multiply(porc).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            descuento.setValue(montoAbsoluto);

            actualizarResumen();
            calculandoDescuento = false;
        });
        descuento.addValueChangeListener(e -> {
            if (calculandoDescuento) return;
            calculandoDescuento = true;

            BigDecimal subtotalActual = obtenerSubtotalActual();
            BigDecimal montoAbs = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;

            if (subtotalActual.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal porcCalculado = montoAbs.multiply(new BigDecimal("100")).divide(subtotalActual, 2, RoundingMode.HALF_UP);
                descuentoPorcentaje.setValue(porcCalculado);
            } else {
                descuentoPorcentaje.setValue(BigDecimal.ZERO);
            }

            actualizarResumen();
            calculandoDescuento = false;
        });

        montoPagado.setValue(BigDecimal.ZERO);
        montoPagado.setPrefixComponent(new Span("RD$"));
        montoPagado.setValueChangeMode(ValueChangeMode.EAGER);
        montoPagado.addValueChangeListener(event -> actualizarResumen());

        metodoPago.setItems(MetodoPago.EFECTIVO, MetodoPago.TRANSFERENCIA);
        metodoPago.setItemLabelGenerator(MetodoPago::getEtiqueta);
        metodoPago.addValueChangeListener(event -> {
            boolean esTransferencia = event.getValue() == MetodoPago.TRANSFERENCIA;
            datosTransferencia.setVisible(esTransferencia);
            if (!esTransferencia) {
                datosTransferencia.limpiar();
            }
        });

        costoEnvio.setValue(BigDecimal.ZERO);
        costoEnvio.setPrefixComponent(new Span("RD$"));
        costoEnvio.setEnabled(false);
        costoEnvio.setValueChangeMode(ValueChangeMode.EAGER);
        costoEnvio.addValueChangeListener(event -> actualizarResumen());

        llevaDespacho.addValueChangeListener(event -> {
            boolean lleva = event.getValue();
            costoEnvio.setEnabled(lleva);
            if (!lleva) {
                costoEnvio.setValue(BigDecimal.ZERO);
            }
        });
    }

    private Anchor crearDescargaCuentaBancaria() {
        StreamResource resource = new StreamResource("cuenta-bancaria-transferencia.pdf", () ->
                new ByteArrayInputStream(cuentaBancariaTransferenciaPdfService.generarCuentaBancariaPdf()));
        resource.setContentType("application/pdf");
        resource.setCacheTime(0);

        Button descargar = new Button("Cuenta bancaria", new Icon(VaadinIcon.MONEY));
        descargar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        descargar.setAriaLabel("Descargar datos de cuenta bancaria");
        descargar.setTooltipText("Descargar datos de cuenta bancaria");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.add(descargar);
        return anchor;
    }

    private void calcularCantidadDesdeMonto() {
        if (!"Monto (RD$)".equals(modoIngreso.getValue())) return;

        Producto p = producto.getValue();
        BigDecimal monto = montoIngresado.getValue();

        if (p == null || monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            cantidad.setValue(BigDecimal.ONE);
            return;
        }

        BigDecimal precioEmpaque = p.getPrecioEmpaque();
        BigDecimal factor = p.getContenidoPorEmpaque() != null ? p.getContenidoPorEmpaque() : BigDecimal.ONE;
        BigDecimal precioFraccion = p.getPrecioFraccion() != null ? p.getPrecioFraccion() : precioEmpaque.divide(factor, 4, RoundingMode.HALF_UP);

        BigDecimal cantidadResultante = BigDecimal.ZERO;
        EstrategiaPrecioVenta estrategia = EstrategiaPrecioVenta.fromEtiqueta(estrategiaPrecio.getValue());

        if (EstrategiaPrecioVenta.TODO_PRECIO_EMPAQUE == estrategia) {
            BigDecimal precioUnidadProporcional = precioEmpaque.divide(factor, 6, RoundingMode.HALF_UP);
            cantidadResultante = monto.divide(precioUnidadProporcional, 4, RoundingMode.HALF_UP);

        } else if (EstrategiaPrecioVenta.TODO_PRECIO_FRACCION == estrategia) {
            cantidadResultante = monto.divide(precioFraccion, 4, RoundingMode.HALF_UP);

        } else {
            BigDecimal[] division = monto.divideAndRemainder(precioEmpaque);
            BigDecimal cajasTotales = division[0];
            BigDecimal residuoDinero = division[1];
            BigDecimal unidadesSueltas = residuoDinero.divide(precioFraccion, 4, RoundingMode.HALF_UP);

            cantidadResultante = cajasTotales.multiply(factor).add(unidadesSueltas);
        }

        cantidad.setValue(cantidadResultante);
    }

    private void configurarGrid() {
        gridLineas.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        gridLineas.setHeight("320px");

        gridLineas.addColumn(linea -> linea.getProducto().getNombre())
                .setHeader("Producto")
                .setFlexGrow(1);
        gridLineas.addColumn(linea -> {
            if (linea.getProducto().getCategoria() == CategoriaProducto.SERVICIO) return "N/A";
            return linea.getAlmacen() != null ? linea.getAlmacen().getNombre() : "-";
        }).setHeader("Almacén").setWidth("150px").setFlexGrow(0);
        gridLineas.addColumn(linea -> {
            if (linea.getProducto().getCategoria() == CategoriaProducto.SERVICIO) return "N/A";
            return linea.getLote() != null ? linea.getLote().getNumeroLote() : "Auto (PEPS)";
        }).setHeader("Lote").setWidth("100px").setFlexGrow(0);
        gridLineas.addColumn(linea -> FormatoInventarioUtil.formatearCantidad(
                        linea.getCantidad(),
                        linea.getProducto().getContenidoPorEmpaque(),
                        Boolean.TRUE.equals(linea.getProducto().getPermiteFraccionamiento()),
                        false)
                ).setHeader("Cantidad").setWidth("160px").setFlexGrow(0);

        gridLineas.addColumn(LineaVentaForm::getEstrategiaPrecio).setHeader("Estrategia").setWidth("120px").setFlexGrow(0);

        gridLineas.addColumn(linea -> formatMoney(linea.getImpuesto())).setHeader("Impuesto").setWidth("100px").setFlexGrow(0);
        gridLineas.addColumn(linea -> formatMoney(linea.getSubtotal())).setHeader("Subtotal").setWidth("140px").setFlexGrow(0);

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
        datosTransferencia.sugerirTitular(
                cliente.getPersona() != null ? cliente.getPersona().getNombre() : null
        );
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
        Producto prodSeleccionado = producto.getValue();
        if (prodSeleccionado == null) {
            mostrarError("Debes seleccionar un producto o servicio.");
            return;
        }
        boolean esServicio = prodSeleccionado.getCategoria() == CategoriaProducto.SERVICIO;
        if (!esServicio && cbAlmacen.getValue() == null) {
            mostrarError("Debes seleccionar el almacén de origen para los productos físicos.");
            return;
        }
        if (cantidad.getValue() == null || cantidad.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            mostrarError("La cantidad debe ser mayor que cero.");
            return;
        }

        try {
            LineaVentaForm linea = new LineaVentaForm(
                    producto.getValue(),
                    cbAlmacen.getValue(),
                    cbLote.getValue(),
                    cantidad.getValue().setScale(4, RoundingMode.HALF_UP),
                    impuesto.getValue() != null
                            ? impuesto.getValue().setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    estrategiaPrecio.getValue() != null ? estrategiaPrecio.getValue() : EstrategiaPrecioVenta.NORMAL.getEtiqueta()
            );
            lineas.add(linea);
            producto.clear();
            cbAlmacen.clear();
            cbLote.clear();
            cantidad.setValue(BigDecimal.ONE);
            montoIngresado.clear();
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

            if (chkImprimirTicket.getValue()) {
                byte[] pdfBytes = facturaVentaTermicaPdfService.generarFacturaTermicaPdf(venta.getIdVenta());
                StreamResource resource = new StreamResource("Ticket-" + venta.getIdVenta() + ".pdf", () -> new ByteArrayInputStream(pdfBytes));
                resource.setContentType("application/pdf");

                com.vaadin.flow.server.StreamRegistration registration =
                        com.vaadin.flow.server.VaadinSession.getCurrent().getResourceRegistry().registerResource(resource);

                com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                        "const iframe = document.createElement('iframe');" +
                                "iframe.style.display = 'none';" +
                                "iframe.src = $0;" +
                                "document.body.appendChild(iframe);" +
                                "iframe.onload = function() { setTimeout(function() { iframe.contentWindow.print(); }, 800); };",
                        registration.getResourceUri().toString()
                );
            }

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
                costoEnvio.getValue(),
                descuento.getValue(),
                montoPagado.getValue(),
                metodoPago.getValue(),
                metodoPago.getValue() == MetodoPago.TRANSFERENCIA
                        ? datosTransferencia.obtenerDatos()
                        : null,
                lineas.stream()
                        .map(linea -> new VentaService.LineaVentaRequest(
                                linea.getProducto().getIdProducto(),
                                linea.getCantidad(),
                                linea.getImpuesto(),
                                linea.getAlmacen() != null ? linea.getAlmacen().getIdAlmacen() : null,
                                linea.getLote() != null ? linea.getLote().getIdLote() : null,
                                linea.getEstrategiaPrecio()
                        ))
                        .toList()
        );
    }

    private void refrescarLineas() {
        gridLineas.setItems(lineas);
        if (!calculandoDescuento) {
            calculandoDescuento = true;
            BigDecimal subtotalActual = obtenerSubtotalActual();
            BigDecimal porc = descuentoPorcentaje.getValue() != null ? descuentoPorcentaje.getValue() : BigDecimal.ZERO;
            BigDecimal montoAbsoluto = subtotalActual.multiply(porc).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            descuento.setValue(montoAbsoluto);
            calculandoDescuento = false;
        }
        actualizarResumen();
    }

    private void actualizarResumen() {
        BigDecimal envio = costoEnvio.getValue() != null ? costoEnvio.getValue() : BigDecimal.ZERO;

        if (lineas.isEmpty()) {
            subtotal.setText(formatMoney(BigDecimal.ZERO));
            transporteResumen.setText(formatMoney(envio));
            descuentoResumen.setText(formatMoney(BigDecimal.ZERO));
            total.setText(formatMoney(envio));
            pagado.setText(formatMoney(BigDecimal.ZERO));
            balance.setText(formatMoney(envio));
            estado.setText("Pendiente");
            return;
        }

        try {
            VentaService.ResumenVenta resumen = ventaService.calcularResumen(crearSolicitud());
            subtotal.setText(formatMoney(resumen.subtotal()));
            transporteResumen.setText(formatMoney(envio));
            descuentoResumen.setText(formatMoney(resumen.descuento()));
            total.setText(formatMoney(resumen.total()));
            pagado.setText(formatMoney(resumen.montoPagado()));
            balance.setText(formatMoney(resumen.balancePendiente()));
            estado.setText(resumen.estado().getEtiqueta());
        } catch (Exception ignored) {
            BigDecimal sub = lineas.stream().map(LineaVentaForm::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal desc = descuento.getValue() != null ? descuento.getValue() : BigDecimal.ZERO;

            subtotal.setText(formatMoney(sub));
            transporteResumen.setText(formatMoney(envio));
            descuentoResumen.setText(formatMoney(desc));

            BigDecimal totalMatematico = sub.add(envio).subtract(desc);
            total.setText(formatMoney(totalMatematico));

            BigDecimal pag = montoPagado.getValue() != null ? montoPagado.getValue() : BigDecimal.ZERO;
            pagado.setText(formatMoney(pag));
            balance.setText(formatMoney(totalMatematico.subtract(pag)));
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
        descuentoPorcentaje.setValue(BigDecimal.ZERO);
        montoPagado.setValue(BigDecimal.ZERO);
        metodoPago.clear();
        datosTransferencia.limpiar();
        producto.clear();
        cbAlmacen.clear();
        cbLote.clear();
        chkLoteAutomatico.setValue(true);
        cantidad.setValue(BigDecimal.ONE);
        montoIngresado.clear();
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

    private BigDecimal obtenerSubtotalActual() {
        return lineas.stream()
                .map(LineaVentaForm::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void actualizarImpuestoSugerido() {
        Producto productoSeleccionado = producto.getValue();
        BigDecimal cantidadSeleccionada = cantidad.getValue();

        if (productoSeleccionado == null
                || cantidadSeleccionada == null
                || cantidadSeleccionada.compareTo(BigDecimal.ZERO) <= 0) {
            impuesto.setValue(BigDecimal.ZERO);
            return;
        }

        EstrategiaPrecioVenta estrategia = EstrategiaPrecioVenta.fromEtiqueta(estrategiaPrecio.getValue());
        BigDecimal subtotalSinImpuesto = LineaVentaForm.calcularSubtotalSinImpuesto(
                productoSeleccionado,
                cantidadSeleccionada,
                estrategia
        );
        BigDecimal porcentaje = productoSeleccionado.getPorcentajeImpuesto() != null
                ? productoSeleccionado.getPorcentajeImpuesto()
                : BigDecimal.ZERO;

        impuesto.setValue(
                subtotalSinImpuesto
                        .multiply(porcentaje)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
        );
    }

    private static class LineaVentaForm {
        private final Producto producto;
        private final Almacen almacen;
        private final Lote lote;
        private final BigDecimal cantidad;
        private final BigDecimal impuesto;
        private final EstrategiaPrecioVenta estrategiaPrecio;

        LineaVentaForm(
                Producto producto,
                Almacen almacen,
                Lote lote,
                BigDecimal cantidad,
                BigDecimal impuesto,
                String estrategiaPrecio
        ) {
            this.producto = producto;
            this.almacen = almacen;
            this.lote = lote;
            this.cantidad = cantidad;
            this.impuesto = impuesto;
            this.estrategiaPrecio = EstrategiaPrecioVenta.fromEtiqueta(estrategiaPrecio);
        }

        Producto getProducto() { return producto; }
        Almacen getAlmacen() { return almacen; }
        Lote getLote() { return lote; }
        BigDecimal getCantidad() { return cantidad; }
        BigDecimal getImpuesto() { return impuesto; }
        EstrategiaPrecioVenta getEstrategiaPrecio() { return estrategiaPrecio; }

        BigDecimal getSubtotal() {
            return getSubtotalSinImpuesto().add(getImpuesto()).setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal getSubtotalSinImpuesto() {
            return calcularSubtotalSinImpuesto(producto, cantidad, estrategiaPrecio);
        }

        private static BigDecimal calcularSubtotalSinImpuesto(
                Producto producto,
                BigDecimal cantidad,
                EstrategiaPrecioVenta estrategiaPrecio
        ) {
            if (!Boolean.TRUE.equals(producto.getPermiteFraccionamiento())) {
                return producto.getPrecioEmpaque().multiply(cantidad);
            }

            BigDecimal factor = producto.getContenidoPorEmpaque();
            if (factor == null || factor.compareTo(BigDecimal.ONE) <= 0) {
                return producto.getPrecioEmpaque().multiply(cantidad);
            }

            BigDecimal precioEmp = producto.getPrecioEmpaque();
            BigDecimal precioFracc = producto.getPrecioFraccion() != null ? producto.getPrecioFraccion() : precioEmp.divide(factor, 4, RoundingMode.HALF_UP);

            BigDecimal subtotalCalculado = BigDecimal.ZERO;

            switch (estrategiaPrecio) {
                case EstrategiaPrecioVenta.TODO_PRECIO_EMPAQUE:
                    BigDecimal precioUnidadProporcional = precioEmp.divide(factor, 6, RoundingMode.HALF_UP);
                    subtotalCalculado = cantidad.multiply(precioUnidadProporcional);
                    break;
                case EstrategiaPrecioVenta.TODO_PRECIO_FRACCION:
                    subtotalCalculado = cantidad.multiply(precioFracc);
                    break;
                case EstrategiaPrecioVenta.NORMAL:
                default:
                    BigDecimal[] division = cantidad.divideAndRemainder(factor);
                    BigDecimal cajas = division[0];
                    BigDecimal unidadesSueltas = division[1];
                    subtotalCalculado = cajas.multiply(precioEmp).add(unidadesSueltas.multiply(precioFracc));
                    break;
            }

            return subtotalCalculado;
        }
    }
}
