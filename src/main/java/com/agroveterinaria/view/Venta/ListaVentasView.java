package com.agroveterinaria.view.Venta;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Venta;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.service.CuentaBancariaTransferenciaPdfService;
import com.agroveterinaria.service.FacturaVentaPdfService;
import com.agroveterinaria.service.FacturaVentaTermicaPdfService;
import com.agroveterinaria.service.VentaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
public class ListaVentasView extends VerticalLayout {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "DO"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final VentaService ventaService;
    private final FacturaVentaPdfService facturaVentaPdfService;
    private final CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService;
    private final FacturaVentaTermicaPdfService facturaVentaTermicaPdfService;
    private final TextField buscar = new TextField();
    private final ComboBox<EstadoVenta> estadoFiltro = new ComboBox<>();
    private final Grid<VentaFila> gridVentas = new Grid<>(VentaFila.class, false);
    private final GridPaginator<VentaFila> paginator = new GridPaginator<>(gridVentas, 10, "ventas");

    private final Span ventasRegistradas = new Span();
    private final Span montoTotal = new Span();
    private final Span montoCobrado = new Span();
    private final Span balancePendiente = new Span();

    public ListaVentasView(
            VentaService ventaService,
            FacturaVentaPdfService facturaVentaPdfService,
            CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService,
            FacturaVentaTermicaPdfService facturaVentaTermicaPdfService
    ) {
        this.ventaService = ventaService;
        this.facturaVentaPdfService = facturaVentaPdfService;
        this.cuentaBancariaTransferenciaPdfService = cuentaBancariaTransferenciaPdfService;
        this.facturaVentaTermicaPdfService = facturaVentaTermicaPdfService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);
        addClassName("venta-list-view");

        configurarFiltros();
        configurarGrid();

        add(crearMetricas(), crearToolbar(), paginator, gridVentas);
        actualizarVista();
    }

    private void configurarFiltros() {
        buscar.setPlaceholder("Buscar por cliente, vendedor, NCF o ID...");
        buscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscar.setClearButtonVisible(true);
        buscar.setValueChangeMode(ValueChangeMode.LAZY);
        buscar.addValueChangeListener(event -> actualizarVista());

        estadoFiltro.setPlaceholder("Estado");
        estadoFiltro.setItems(EstadoVenta.values());
        estadoFiltro.setItemLabelGenerator(EstadoVenta::getEtiqueta);
        estadoFiltro.setClearButtonVisible(true);
        estadoFiltro.addValueChangeListener(event -> actualizarVista());
    }

    private HorizontalLayout crearMetricas() {
        HorizontalLayout metricas = new HorizontalLayout(
                crearMetrica("Ventas", ventasRegistradas),
                crearMetrica("Total facturado", montoTotal),
                crearMetrica("Cobrado", montoCobrado),
                crearMetrica("Balance pendiente", balancePendiente)
        );
        metricas.addClassName("venta-metrics");
        metricas.setWidthFull();
        metricas.setSpacing(true);
        return metricas;
    }

    private VerticalLayout crearMetrica(String etiqueta, Span valor) {
        Span label = new Span(etiqueta);
        label.addClassName("venta-metric-label");
        valor.addClassName("venta-metric-value");

        VerticalLayout metrica = new VerticalLayout(valor, label);
        metrica.addClassName("venta-metric");
        metrica.setPadding(false);
        metrica.setSpacing(false);
        return metrica;
    }

    private HorizontalLayout crearToolbar() {
        Button refrescar = new Button(new Icon(VaadinIcon.REFRESH), event -> {
            buscar.clear();
            estadoFiltro.clear();
            actualizarVista();
        });
        refrescar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refrescar.setAriaLabel("Refrescar lista de ventas");
        refrescar.setTooltipText("Refrescar lista de ventas");

        HorizontalLayout toolbar = new HorizontalLayout(buscar, estadoFiltro, crearDescargaCuentaBancaria(), refrescar);
        toolbar.addClassName("venta-list-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.expand(buscar);
        return toolbar;
    }

    private void configurarGrid() {
        gridVentas.addClassName("venta-list-grid");
        gridVentas.addThemeNames("row-stripes");
        gridVentas.setWidthFull();
        gridVentas.setHeight("390px");

        gridVentas.addColumn(fila -> "#" + fila.venta().getIdVenta())
                .setHeader("ID")
                .setWidth("90px")
                .setFlexGrow(0);

        gridVentas.addColumn(fila -> formatDateTime(fila.venta().getFechaHoraVenta()))
                .setHeader("Fecha / hora")
                .setWidth("170px")
                .setFlexGrow(0);

        gridVentas.addComponentColumn(fila -> crearPersonaCell(
                        nombreCliente(fila.venta().getCliente()),
                        cedulaCliente(fila.venta().getCliente())
                ))
                .setHeader("Cliente")
                .setFlexGrow(2);

        gridVentas.addColumn(fila -> nombreVendedor(fila.venta().getVendedor()))
                .setHeader("Vendedor")
                .setFlexGrow(1);

        gridVentas.addColumn(fila -> formatMoney(fila.venta().getMontoTotal()))
                .setHeader("Total")
                .setWidth("130px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);

        gridVentas.addColumn(fila -> formatMoney(fila.cobrado()))
                .setHeader("Cobrado")
                .setWidth("130px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);

        gridVentas.addColumn(fila -> formatMoney(fila.balance()))
                .setHeader("Balance")
                .setWidth("130px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);

        gridVentas.addComponentColumn(fila -> crearBadgeEstado(fila.venta().getEstado()))
                .setHeader("Estado")
                .setWidth("130px")
                .setFlexGrow(0);

        gridVentas.addColumn(fila -> Boolean.TRUE.equals(fila.venta().getLlevaDespacho()) ? "Si" : "No")
                .setHeader("Despacho")
                .setWidth("110px")
                .setFlexGrow(0);

        gridVentas.addComponentColumn(fila -> {
            Button detalle = new Button(new Icon(VaadinIcon.EYE), event -> abrirDetalle(fila));
            detalle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            detalle.setAriaLabel("Ver detalle de venta");
            detalle.setTooltipText("Ver detalle");

            Button imprimir = new Button(new Icon(VaadinIcon.PRINT), event -> imprimirTicketEnNavegador(fila.venta()));
            imprimir.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            imprimir.setAriaLabel("Imprimir ticket");
            imprimir.setTooltipText("Imprimir ticket térmico");

            HorizontalLayout acciones = new HorizontalLayout(detalle, imprimir, crearDescargaPdf(fila), crearDescargaCuentaBancariaIcono());
            acciones.setPadding(false);
            acciones.setSpacing(false);
            acciones.setAlignItems(FlexComponent.Alignment.CENTER);
            return acciones;
        }).setHeader("Opciones").setWidth("210px").setFlexGrow(0);
    }

    private Anchor crearDescargaCuentaBancaria() {
        StreamResource resource = recursoCuentaBancaria();

        Button descargar = new Button("Cuenta bancaria", new Icon(VaadinIcon.MONEY));
        descargar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        descargar.setAriaLabel("Descargar datos de cuenta bancaria");
        descargar.setTooltipText("Descargar datos de cuenta bancaria");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.add(descargar);
        return anchor;
    }

    private Anchor crearDescargaCuentaBancariaIcono() {
        StreamResource resource = recursoCuentaBancaria();

        Button descargar = new Button(new Icon(VaadinIcon.MONEY));
        descargar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        descargar.setAriaLabel("Descargar datos de cuenta bancaria");
        descargar.setTooltipText("Descargar datos de cuenta bancaria");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.add(descargar);
        return anchor;
    }

    private StreamResource recursoCuentaBancaria() {
        StreamResource resource = new StreamResource("cuenta-bancaria-transferencia.pdf", () ->
                new ByteArrayInputStream(cuentaBancariaTransferenciaPdfService.generarCuentaBancariaPdf()));
        resource.setContentType("application/pdf");
        resource.setCacheTime(0);
        return resource;
    }

    private Anchor crearDescargaPdf(VentaFila fila) {
        Venta venta = fila.venta();
        StreamResource resource = new StreamResource(nombreArchivoFactura(venta), () ->
                new ByteArrayInputStream(facturaVentaPdfService.generarFacturaVentaPdf(venta.getIdVenta())));
        resource.setContentType("application/pdf");
        resource.setCacheTime(0);

        Button descargar = new Button(new Icon(VaadinIcon.DOWNLOAD_ALT));
        descargar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        descargar.setAriaLabel("Descargar factura PDF");
        descargar.setTooltipText("Descargar factura PDF");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.add(descargar);
        return anchor;
    }

    private String nombreArchivoFactura(Venta venta) {
        Long idVenta = venta != null ? venta.getIdVenta() : null;
        return "factura-venta-" + (idVenta != null ? idVenta : "sin-id") + ".pdf";
    }

    private Div crearPersonaCell(String nombre, String detalle) {
        Span titulo = new Span(nombre);
        titulo.addClassName("venta-cell-title");

        Span subtitulo = new Span(detalle);
        subtitulo.addClassName("venta-cell-subtitle");

        Div cell = new Div(titulo, subtitulo);
        cell.addClassName("venta-persona-cell");
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

    private void abrirDetalle(VentaFila fila) {
        Venta venta = fila.venta();

        Dialog dialog = new Dialog();
        dialog.setWidth("520px");
        dialog.setCloseOnOutsideClick(true);

        H3 titulo = new H3("Venta #" + venta.getIdVenta());
        titulo.getStyle().set("margin", "0");

        VerticalLayout contenido = new VerticalLayout(
                filaDetalle("Fecha", formatDateTime(venta.getFechaHoraVenta())),
                filaDetalle("Cliente", nombreCliente(venta.getCliente())),
                filaDetalle("Vendedor", nombreVendedor(venta.getVendedor())),
                filaDetalle("Comprobante fiscal", valorOrDefault(venta.getComprobanteFiscal(), "Sin comprobante")),
                filaDetalle("Total", formatMoney(venta.getMontoTotal())),
                filaDetalle("Cobrado", formatMoney(fila.cobrado())),
                filaDetalle("Balance pendiente", formatMoney(fila.balance())),
                filaDetalle("Estado", venta.getEstado() != null ? venta.getEstado().getEtiqueta() : ""),
                filaDetalle("Lleva despacho", Boolean.TRUE.equals(venta.getLlevaDespacho()) ? "Si" : "No"),
                filaDetalle("Vencimiento de pago", formatDateTime(venta.getFechaVencimientoPago()))
        );
        contenido.addClassName("venta-detail-list");
        contenido.setPadding(false);
        contenido.setSpacing(false);

        Button cerrar = new Button("Cerrar", event -> dialog.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout acciones = new HorizontalLayout(cerrar);
        acciones.setWidthFull();
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout layout = new VerticalLayout(titulo, contenido, acciones);
        layout.setPadding(false);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open();
    }

    private HorizontalLayout filaDetalle(String etiqueta, String valor) {
        Span label = new Span(etiqueta);
        label.addClassName("venta-detail-label");

        Span value = new Span(valorOrDefault(valor, "-"));
        value.addClassName("venta-detail-value");

        HorizontalLayout fila = new HorizontalLayout(label, value);
        fila.addClassName("venta-detail-row");
        fila.setWidthFull();
        fila.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return fila;
    }

    private void actualizarVista() {
        List<VentaFila> filas = ventaService.listarTodos().stream()
                .sorted(Comparator.comparing(
                        Venta::getFechaHoraVenta,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(venta -> new VentaFila(
                        venta,
                        ventaService.calcularTotalCobrado(venta),
                        ventaService.calcularDeudaRestante(venta)
                ))
                .filter(this::cumpleFiltros)
                .toList();

        paginator.setItems(filas);
        actualizarMetricas(filas);
    }

    private boolean cumpleFiltros(VentaFila fila) {
        Venta venta = fila.venta();
        EstadoVenta estadoSeleccionado = estadoFiltro.getValue();
        if (estadoSeleccionado != null && venta.getEstado() != estadoSeleccionado) {
            return false;
        }

        String texto = buscar.getValue() != null ? buscar.getValue().trim().toLowerCase(Locale.ROOT) : "";
        if (texto.isBlank()) {
            return true;
        }

        return String.valueOf(venta.getIdVenta()).contains(texto)
                || contiene(nombreCliente(venta.getCliente()), texto)
                || contiene(cedulaCliente(venta.getCliente()), texto)
                || contiene(nombreVendedor(venta.getVendedor()), texto)
                || contiene(venta.getComprobanteFiscal(), texto);
    }

    private void actualizarMetricas(List<VentaFila> filas) {
        BigDecimal total = filas.stream()
                .map(fila -> montoSeguro(fila.venta().getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cobrado = filas.stream()
                .map(VentaFila::cobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendiente = filas.stream()
                .map(VentaFila::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ventasRegistradas.setText(String.valueOf(filas.size()));
        montoTotal.setText(formatMoney(total));
        montoCobrado.setText(formatMoney(cobrado));
        balancePendiente.setText(formatMoney(pendiente));
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

    private void imprimirTicketEnNavegador(Venta venta) {
        try {
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
        } catch (Exception ex) {
            com.vaadin.flow.component.notification.Notification.show("Error al imprimir el ticket: " + ex.getMessage());
        }
    }

    private record VentaFila(Venta venta, BigDecimal cobrado, BigDecimal balance) {
    }
}
