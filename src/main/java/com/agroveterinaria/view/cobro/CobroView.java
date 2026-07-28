package com.agroveterinaria.view.cobro;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.component.DatosTransferenciaForm;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Cobro;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.NotaDeCredito;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.enums.MetodoPago;
import com.agroveterinaria.service.CuentaBancariaTransferenciaPdfService;
import com.agroveterinaria.service.VentaService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class CobroView extends VerticalLayout {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "DO"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final VentaService ventaService;
    private final CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService;
    private final TextField buscar = new TextField();
    private final Grid<CarteraFila> gridCartera = new Grid<>(CarteraFila.class, false);
    private final Grid<Cobro> gridHistorial = new Grid<>(Cobro.class, false);
    private final GridPaginator<CarteraFila> carteraPaginator = new GridPaginator<>(gridCartera, 10, "ventas");
    private final GridPaginator<Cobro> historialPaginator = new GridPaginator<>(gridHistorial, 10, "cobros");

    private final Span ventasPendientes = new Span();
    private final Span montoPendiente = new Span();
    private final Span montoCobrado = new Span();
    private final Span cobrosRegistrados = new Span();

    private final Span clienteSeleccionado = new Span("Selecciona una venta");
    private final Span ventaSeleccionada = new Span("-");
    private final Span totalSeleccionado = new Span(formatMoney(BigDecimal.ZERO));
    private final Span cobradoSeleccionado = new Span(formatMoney(BigDecimal.ZERO));
    private final Span balanceSeleccionado = new Span(formatMoney(BigDecimal.ZERO));
    private final Span vencimientoSeleccionado = new Span("-");
    private final BigDecimalField monto = new BigDecimalField("Monto a cobrar");
    private final ComboBox<MetodoPago> metodoPago = new ComboBox<>("Metodo de pago");
    private final ComboBox<NotaDeCredito> notaCredito = new ComboBox<>("Nota de crédito");
    private final DatosTransferenciaForm datosTransferencia = new DatosTransferenciaForm();
    private final Button registrar = new Button("Registrar cobro", new Icon(VaadinIcon.CHECK));

    private CarteraFila filaSeleccionada;

    public CobroView(
            VentaService ventaService,
            CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService
    ) {
        this.ventaService = ventaService;
        this.cuentaBancariaTransferenciaPdfService = cuentaBancariaTransferenciaPdfService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);
        addClassName("cobro-view");

        configurarFiltros();
        configurarGridCartera();
        configurarFormulario();
        configurarGridHistorial();

        HorizontalLayout workspace = new HorizontalLayout(crearPanelCartera(), crearPanelCobro());
        workspace.addClassName("cobro-workspace");
        workspace.setSizeFull();
        workspace.setPadding(false);
        workspace.setSpacing(true);
        workspace.expand(workspace.getComponentAt(0));

        add(crearMetricas(), workspace, crearPanelHistorial());
        expand(workspace);
        actualizarVista();
    }

    private void configurarFiltros() {
        buscar.setPlaceholder("Buscar por cliente, vendedor, cedula o ID de venta...");
        buscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscar.setClearButtonVisible(true);
        buscar.setValueChangeMode(ValueChangeMode.LAZY);
        buscar.addValueChangeListener(event -> actualizarVista());
    }

    private HorizontalLayout crearMetricas() {
        HorizontalLayout metricas = new HorizontalLayout(
                crearMetrica("Ventas pendientes", ventasPendientes),
                crearMetrica("Por cobrar", montoPendiente),
                crearMetrica("Cobrado registrado", montoCobrado),
                crearMetrica("Cobros", cobrosRegistrados)
        );
        metricas.addClassName("cobro-metrics");
        metricas.setWidthFull();
        metricas.setSpacing(true);
        return metricas;
    }

    private VerticalLayout crearMetrica(String etiqueta, Span valor) {
        Span label = new Span(etiqueta);
        label.addClassName("cobro-metric-label");
        valor.addClassName("cobro-metric-value");

        VerticalLayout metrica = new VerticalLayout(valor, label);
        metrica.addClassName("cobro-metric");
        metrica.setPadding(false);
        metrica.setSpacing(false);
        return metrica;
    }

    private VerticalLayout crearPanelCartera() {
        H3 titulo = new H3("Cartera pendiente");
        titulo.addClassName("cobro-panel-title");

        HorizontalLayout toolbar = new HorizontalLayout(buscar);
        toolbar.addClassName("cobro-toolbar");
        toolbar.setWidthFull();
        toolbar.expand(buscar);

        VerticalLayout panel = new VerticalLayout(titulo, toolbar, carteraPaginator, gridCartera);
        panel.addClassName("cobro-list-panel");
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setSizeFull();
        return panel;
    }

    private VerticalLayout crearPanelCobro() {
        H3 titulo = new H3("Aplicar cobro");
        titulo.addClassName("cobro-panel-title");

        VerticalLayout resumen = new VerticalLayout(
                crearFilaResumen("Cliente", clienteSeleccionado),
                crearFilaResumen("Venta", ventaSeleccionada),
                crearFilaResumen("Total", totalSeleccionado),
                crearFilaResumen("Cobrado", cobradoSeleccionado),
                crearFilaResumen("Balance", balanceSeleccionado),
                crearFilaResumen("Vencimiento", vencimientoSeleccionado)
        );
        resumen.addClassName("cobro-summary-list");
        resumen.setPadding(false);
        resumen.setSpacing(false);

        registrar.addClassName("btn-nuevo");
        registrar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registrar.setWidthFull();
        registrar.addClickListener(event -> registrarCobro());

        VerticalLayout panel = new VerticalLayout(
                titulo,
                resumen,
                monto,
                metodoPago,
                notaCredito,
                datosTransferencia,
                crearDescargaCuentaBancaria(),
                registrar
        );
        panel.addClassName("cobro-form-panel");
        panel.setPadding(false);
        panel.setSpacing(true);
        return panel;
    }

    private HorizontalLayout crearFilaResumen(String etiqueta, Span valor) {
        Span label = new Span(etiqueta);
        label.addClassName("cobro-summary-label");
        valor.addClassName("cobro-summary-value");

        HorizontalLayout fila = new HorizontalLayout(label, valor);
        fila.addClassName("cobro-summary-row");
        fila.setWidthFull();
        fila.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return fila;
    }

    private VerticalLayout crearPanelHistorial() {
        H3 titulo = new H3("Historial de cobros");
        titulo.addClassName("cobro-panel-title");

        VerticalLayout panel = new VerticalLayout(titulo, historialPaginator, gridHistorial);
        panel.addClassName("cobro-history-panel");
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setWidthFull();
        return panel;
    }

    private void configurarGridCartera() {
        gridCartera.addClassName("cobro-grid");
        gridCartera.addThemeNames("row-stripes");
        gridCartera.setWidthFull();
        gridCartera.setHeight("390px");

        gridCartera.addColumn(fila -> "#" + fila.venta().getIdVenta())
                .setHeader("Venta")
                .setWidth("90px")
                .setFlexGrow(0);

        gridCartera.addComponentColumn(fila -> crearPersonaCell(
                        nombreCliente(fila.venta().getCliente()),
                        cedulaCliente(fila.venta().getCliente())
                ))
                .setHeader("Cliente")
                .setFlexGrow(2);

        gridCartera.addColumn(fila -> nombreVendedor(fila.venta().getVendedor()))
                .setHeader("Vendedor")
                .setFlexGrow(1);

        gridCartera.addColumn(fila -> formatDateTime(fila.venta().getFechaVencimientoPago()))
                .setHeader("Vence")
                .setWidth("150px")
                .setFlexGrow(0);

        gridCartera.addColumn(fila -> formatMoney(fila.venta().getMontoTotal()))
                .setHeader("Total")
                .setWidth("125px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);

        gridCartera.addColumn(fila -> formatMoney(fila.balance()))
                .setHeader("Balance")
                .setWidth("125px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);

        gridCartera.addComponentColumn(fila -> crearBadgeEstado(fila.venta().getEstado()))
                .setHeader("Estado")
                .setWidth("120px")
                .setFlexGrow(0);

        gridCartera.asSingleSelect().addValueChangeListener(event -> seleccionarVenta(event.getValue()));
    }

    private void configurarFormulario() {
        monto.setPrefixComponent(new Span("RD$"));
        monto.setValueChangeMode(ValueChangeMode.EAGER);
        monto.setEnabled(false);
        monto.setWidthFull();

        metodoPago.setItems(MetodoPago.EFECTIVO, MetodoPago.TRANSFERENCIA, MetodoPago.NOTA_CREDITO);
        metodoPago.setItemLabelGenerator(MetodoPago::getEtiqueta);
        metodoPago.setValue(MetodoPago.EFECTIVO);
        metodoPago.setEnabled(false);
        metodoPago.setWidthFull();
        metodoPago.addValueChangeListener(event -> {
            boolean esTransferencia = event.getValue() == MetodoPago.TRANSFERENCIA;
            boolean esNotaCredito = event.getValue() == MetodoPago.NOTA_CREDITO;
            datosTransferencia.setVisible(esTransferencia);
            notaCredito.setVisible(esNotaCredito);
            if (!esTransferencia) {
                datosTransferencia.limpiar();
            }
            if (!esNotaCredito) {
                notaCredito.clear();
            }
        });

        notaCredito.setVisible(false);
        notaCredito.setWidthFull();
        notaCredito.setItemLabelGenerator(nota -> "#" + nota.getIdNotaCredito()
                + " — disponible " + formatMoney(nota.getSaldoDisponible()));

        registrar.setEnabled(false);
    }

    private Anchor crearDescargaCuentaBancaria() {
        StreamResource resource = new StreamResource("cuenta-bancaria-transferencia.pdf", () ->
                new ByteArrayInputStream(cuentaBancariaTransferenciaPdfService.generarCuentaBancariaPdf()));
        resource.setContentType("application/pdf");
        resource.setCacheTime(0);

        Button descargar = new Button("Cuenta bancaria", new Icon(VaadinIcon.MONEY));
        descargar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        descargar.setWidthFull();
        descargar.setAriaLabel("Descargar datos de cuenta bancaria");
        descargar.setTooltipText("Descargar datos de cuenta bancaria");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.setWidthFull();
        anchor.add(descargar);
        return anchor;
    }

    private void configurarGridHistorial() {
        gridHistorial.addClassName("cobro-grid");
        gridHistorial.addThemeNames("row-stripes");
        gridHistorial.setHeight("240px");

        gridHistorial.addColumn(cobro -> "#" + cobro.getIdCobro())
                .setHeader("Cobro")
                .setWidth("90px")
                .setFlexGrow(0);

        gridHistorial.addColumn(cobro -> cobro.getVenta() != null ? "#" + cobro.getVenta().getIdVenta() : "Sin venta")
                .setHeader("Venta")
                .setWidth("100px")
                .setFlexGrow(0);

        gridHistorial.addColumn(cobro -> nombreCliente(cobro.getCliente()))
                .setHeader("Cliente")
                .setFlexGrow(2);

        gridHistorial.addColumn(cobro -> cobro.getMetodoPago() != null ? cobro.getMetodoPago().getEtiqueta() : "")
                .setHeader("Metodo")
                .setFlexGrow(1);

        gridHistorial.addColumn(cobro -> cobro.getNotaDeCredito() != null
                        ? "#" + cobro.getNotaDeCredito().getIdNotaCredito()
                        : "-")
                .setHeader("Nota")
                .setWidth("90px")
                .setFlexGrow(0);

        gridHistorial.addColumn(cobro -> valorOrDefault(cobro.getReferenciaTransferencia(), "-"))
                .setHeader("Referencia")
                .setFlexGrow(1);

        gridHistorial.addColumn(cobro -> valorOrDefault(cobro.getTitularTransferencia(), "-"))
                .setHeader("Titular transferencia")
                .setFlexGrow(2);

        gridHistorial.addComponentColumn(this::crearDescargaComprobante)
                .setHeader("Comprobante")
                .setWidth("130px")
                .setFlexGrow(0);

        gridHistorial.addColumn(cobro -> formatMoney(cobro.getMontoTotal()))
                .setHeader("Monto")
                .setWidth("130px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);
    }

    private Div crearPersonaCell(String nombre, String detalle) {
        Span titulo = new Span(nombre);
        titulo.addClassName("cobro-cell-title");

        Span subtitulo = new Span(detalle);
        subtitulo.addClassName("cobro-cell-subtitle");

        Div cell = new Div(titulo, subtitulo);
        cell.addClassName("cobro-persona-cell");
        return cell;
    }

    private Span crearBadgeEstado(EstadoVenta estado) {
        Span badge = new Span(estado != null ? estado.getEtiqueta() : "");
        if (estado == EstadoVenta.CERRADA) {
            badge.getElement().getThemeList().add("badge success");
        } else if (estado == EstadoVenta.PENDIENTE) {
            badge.getElement().getThemeList().add("badge warning");
        } else {
            badge.getElement().getThemeList().add("badge contrast");
        }
        return badge;
    }

    private void actualizarVista() {
        List<CarteraFila> filas = ventaService.listarTodos().stream()
                .sorted(Comparator.comparing(
                        Venta::getFechaHoraVenta,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(venta -> new CarteraFila(
                        venta,
                        ventaService.calcularTotalCobrado(venta),
                        ventaService.calcularDeudaRestante(venta)
                ))
                .filter(fila -> fila.balance().compareTo(BigDecimal.ZERO) > 0)
                .filter(this::cumpleFiltro)
                .toList();

        List<Cobro> cobros = ventaService.listarCobros().stream()
                .sorted(Comparator.comparing(
                        Cobro::getIdCobro,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        carteraPaginator.setItems(filas);
        historialPaginator.setItems(cobros);
        actualizarMetricas(filas, cobros);

        boolean mantieneSeleccion = filaSeleccionada != null && filas.stream()
                .anyMatch(fila -> fila.venta().getIdVenta().equals(filaSeleccionada.venta().getIdVenta()));

        if (mantieneSeleccion) {
            filas.stream()
                    .filter(fila -> fila.venta().getIdVenta().equals(filaSeleccionada.venta().getIdVenta()))
                    .findFirst()
                    .ifPresent(gridCartera::select);
        } else if (!filas.isEmpty()) {
            gridCartera.select(filas.get(0));
        } else {
            gridCartera.deselectAll();
            seleccionarVenta(null);
        }
    }

    private boolean cumpleFiltro(CarteraFila fila) {
        String texto = buscar.getValue() != null ? buscar.getValue().trim().toLowerCase(Locale.ROOT) : "";
        if (texto.isBlank()) {
            return true;
        }

        Venta venta = fila.venta();
        return String.valueOf(venta.getIdVenta()).contains(texto)
                || contiene(nombreCliente(venta.getCliente()), texto)
                || contiene(cedulaCliente(venta.getCliente()), texto)
                || contiene(nombreVendedor(venta.getVendedor()), texto);
    }

    private void actualizarMetricas(List<CarteraFila> filas, List<Cobro> cobros) {
        BigDecimal totalPendiente = filas.stream()
                .map(CarteraFila::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCobrado = cobros.stream()
                .map(cobro -> montoSeguro(cobro.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ventasPendientes.setText(String.valueOf(filas.size()));
        montoPendiente.setText(formatMoney(totalPendiente));
        montoCobrado.setText(formatMoney(totalCobrado));
        cobrosRegistrados.setText(String.valueOf(cobros.size()));
    }

    private void seleccionarVenta(CarteraFila fila) {
        filaSeleccionada = fila;
        boolean tieneVenta = fila != null;

        clienteSeleccionado.setText(tieneVenta ? nombreCliente(fila.venta().getCliente()) : "Selecciona una venta");
        ventaSeleccionada.setText(tieneVenta ? "#" + fila.venta().getIdVenta() : "-");
        totalSeleccionado.setText(tieneVenta ? formatMoney(fila.venta().getMontoTotal()) : formatMoney(BigDecimal.ZERO));
        cobradoSeleccionado.setText(tieneVenta ? formatMoney(fila.cobrado()) : formatMoney(BigDecimal.ZERO));
        balanceSeleccionado.setText(tieneVenta ? formatMoney(fila.balance()) : formatMoney(BigDecimal.ZERO));
        vencimientoSeleccionado.setText(tieneVenta ? formatDateTime(fila.venta().getFechaVencimientoPago()) : "-");

        monto.setEnabled(tieneVenta);
        metodoPago.setEnabled(tieneVenta);
        registrar.setEnabled(tieneVenta);
        monto.setValue(tieneVenta ? fila.balance() : null);
        if (tieneVenta) {
            datosTransferencia.sugerirTitular(nombreCliente(fila.venta().getCliente()));
            List<NotaDeCredito> notas = ventaService.listarNotasCreditoDisponibles(
                    fila.venta().getCliente().getIdCliente()
            );
            notaCredito.setItems(notas);
            if (notas.size() == 1) {
                notaCredito.setValue(notas.getFirst());
            }
        } else {
            notaCredito.clear();
            notaCredito.setItems(List.of());
        }
    }

    private void registrarCobro() {
        if (filaSeleccionada == null) {
            mostrarError("Debes seleccionar una venta pendiente.");
            return;
        }

        try {
            ventaService.registrarCobro(
                    filaSeleccionada.venta().getCliente(),
                    filaSeleccionada.venta(),
                    metodoPago.getValue(),
                    monto.getValue(),
                    metodoPago.getValue() == MetodoPago.TRANSFERENCIA
                            ? datosTransferencia.obtenerDatos()
                            : null,
                    notaCredito.getValue() != null
                            ? notaCredito.getValue().getIdNotaCredito()
                            : null
            );
            Notification notification = Notification.show(
                    "Cobro registrado correctamente.",
                    3000,
                    Notification.Position.BOTTOM_END
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            datosTransferencia.limpiar();
            metodoPago.setValue(MetodoPago.EFECTIVO);
            actualizarVista();
        } catch (Exception ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private Component crearDescargaComprobante(Cobro cobro) {
        byte[] contenido = cobro.getComprobanteTransferencia();
        if (contenido == null || contenido.length == 0) {
            return new Span("-");
        }

        String nombre = valorOrDefault(cobro.getNombreComprobante(), "comprobante-transferencia");
        StreamResource resource = new StreamResource(nombre, () ->
                new ByteArrayInputStream(contenido));
        resource.setContentType(valorOrDefault(cobro.getTipoContenidoComprobante(), "application/octet-stream"));
        resource.setCacheTime(0);

        Anchor descarga = new Anchor(resource, "Descargar");
        descarga.getElement().setAttribute("download", true);
        return descarga;
    }

    private boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private String nombreCliente(Cliente cliente) {
        Persona persona = cliente != null ? cliente.getPersona() : null;
        return persona != null ? valorOrDefault(persona.getNombre(), "Cliente sin nombre") : "Cliente sin nombre";
    }

    private String cedulaCliente(Cliente cliente) {
        Persona persona = cliente != null ? cliente.getPersona() : null;
        return persona != null ? valorOrDefault(persona.getCedula(), "Sin cedula") : "Sin cedula";
    }

    private String nombreVendedor(Empleado empleado) {
        Persona persona = empleado != null ? empleado.getPersona() : null;
        return persona != null ? valorOrDefault(persona.getNombre(), "Sin vendedor") : "Sin vendedor";
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMAT) : "-";
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(montoSeguro(value));
    }

    private BigDecimal montoSeguro(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String valorOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private record CarteraFila(Venta venta, BigDecimal cobrado, BigDecimal balance) {
    }
}
