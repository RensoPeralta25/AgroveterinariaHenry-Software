package com.agroveterinaria.view.devolucion;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoVenta;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.security.SecurityService;
import com.agroveterinaria.service.*;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.agroveterinaria.component.CantidadFraccionadaField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class RegistroDevolucionView extends VerticalLayout {

    private final VentaService ventaService;
    private final AlmacenService almacenService;
    private final DevolucionVentaService devolucionService;
    private final SecurityService securityService;
    private final Runnable accionVolver;

    private ComboBox<Venta> cbVentasCerradas;
    private final Span lblAlertaTiempo = new Span();
    private Grid<LineaDevolucionUI> gridItems;
    private final List<LineaDevolucionUI> lineasVista = new ArrayList<>();

    private TextArea txtJustificacion;
    private Checkbox chkNotaCredito;
    private final Span lblMontoNotaCredito = new Span("RD$ 0.00");
    private BigDecimal montoReembolsoCalculado = BigDecimal.ZERO;

    public RegistroDevolucionView(VentaService ventaService, AlmacenService almacenService,
                                  DevolucionVentaService devolucionService, SecurityService securityService,
                                  Runnable accionVolver) {
        this.ventaService = ventaService;
        this.almacenService = almacenService;
        this.devolucionService = devolucionService;
        this.securityService = securityService;
        this.accionVolver = accionVolver;

        setSizeFull();
        setPadding(false);
        setSpacing(true);

        Button btnVolver = new Button("Volver al Historial", new Icon(VaadinIcon.ARROW_LEFT), e -> accionVolver.run());
        btnVolver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        configurarCabeceraBuscador();

        SplitLayout panelDivisor = new SplitLayout(construirPanelArtculos(), construirPanelFinanciero());
        panelDivisor.setSplitterPosition(65);
        panelDivisor.setSizeFull();

        add(btnVolver, cbVentasCerradas, lblAlertaTiempo, panelDivisor);
        expand(panelDivisor);
    }

    private void configurarCabeceraBuscador() {
        cbVentasCerradas = new ComboBox<>("Buscar Factura de Venta Cerrada");
        cbVentasCerradas.setWidthFull();
        cbVentasCerradas.setPlaceholder("Escriba el ID de venta o nombre del cliente...");

        cbVentasCerradas.setItems(ventaService.findByEstado(EstadoVenta.CERRADA));
        cbVentasCerradas.setItemLabelGenerator(v -> "Factura #" + v.getIdVenta() + " - Cliente: " + v.getCliente().getPersona().getNombre());

        lblAlertaTiempo.getElement().getThemeList().add("badge warning");
        lblAlertaTiempo.getStyle().set("padding", "10px").set("font-weight", "600");
        lblAlertaTiempo.setVisible(false);

        cbVentasCerradas.addValueChangeListener(e -> cargarDatosVenta(e.getValue()));
    }

    private VerticalLayout construirPanelArtculos() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(true);

        H3 tituloTabla = new H3("Artículos de la Factura Elegibles");
        tituloTabla.getStyle().set("margin", "0");

        gridItems = new Grid<>();
        gridItems.addClassName("devolucion-grid");
        gridItems.addThemeNames("row-stripes");
        gridItems.setSizeFull();

        gridItems.addColumn(linea -> linea.getDetalleOriginal().getProducto().getNombre()).setHeader("Producto").setFlexGrow(2);
        gridItems.addColumn(linea -> {
            Lote lote = linea.getDetalleOriginal().getLote();
            return (lote != null && lote.getNumeroLote() != null) ? lote.getNumeroLote() : "Sin asignar (PEPS)";
        }).setHeader("Lote").setFlexGrow(1);

        gridItems.addColumn(linea -> FormatoInventarioUtil.formatearCantidad(
                linea.getDetalleOriginal().getCantidad(),
                linea.getDetalleOriginal().getProducto().getContenidoPorEmpaque(),
                Boolean.TRUE.equals(linea.getDetalleOriginal().getProducto().getPermiteFraccionamiento()),
                false,
                FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(linea.getDetalleOriginal().getProducto()),
                FormatoInventarioUtil.getNombreUnidadFraccionSafe(linea.getDetalleOriginal().getProducto())
        )).setHeader("Comprado").setFlexGrow(1);

        gridItems.addComponentColumn(linea -> linea.getFieldCantidad()).setHeader("Cant. a Devolver").setWidth("280px").setFlexGrow(0);
        gridItems.addComponentColumn(linea -> linea.getCbAlmacenDestino()).setHeader("Almacén Reingreso").setWidth("200px").setFlexGrow(0);

        panel.add(tituloTabla, gridItems);
        panel.expand(gridItems);
        return panel;
    }

    private VerticalLayout construirPanelFinanciero() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(true);
        panel.getStyle().set("background-color", "var(--lumo-contrast-5pct)").set("border-radius", "8px");

        H3 tituloFinanciero = new H3("Ajuste Financiero");
        tituloFinanciero.getStyle().set("margin-top", "0");

        txtJustificacion = new TextArea("Justificación / Razón de la Devolución");
        txtJustificacion.setWidthFull();
        txtJustificacion.setPlaceholder("Ej. Producto defectuoso de fábrica / Cliente cambió de parecer...");
        txtJustificacion.setRequired(true);

        chkNotaCredito = new Checkbox("Emitir Nota de Crédito Financiera");
        chkNotaCredito.setValue(true);

        HorizontalLayout resumenMonto = new HorizontalLayout(new Span("Monto a Reembolsar:"), lblMontoNotaCredito);
        resumenMonto.setWidthFull();
        resumenMonto.setJustifyContentMode(JustifyContentMode.BETWEEN);
        resumenMonto.getStyle().set("font-weight", "bold").set("font-size", "18px");

        Button btnProcesar = new Button("Procesar Devolución", new Icon(VaadinIcon.CHECK_CIRCLE), e -> ejecutarDevolucion());
        btnProcesar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnProcesar.setWidthFull();
        btnProcesar.getStyle().set("height", "50px");

        panel.add(tituloFinanciero, txtJustificacion, chkNotaCredito, resumenMonto, btnProcesar);
        return panel;
    }

    private void cargarDatosVenta(Venta ventaSeleccionadaLigera) {
        lineasVista.clear();
        montoReembolsoCalculado = BigDecimal.ZERO;
        lblMontoNotaCredito.setText("RD$ 0.00");
        lblAlertaTiempo.setVisible(false);

        if (ventaSeleccionadaLigera == null) {
            gridItems.setItems(new ArrayList<>());
            return;
        }

        Venta venta = ventaService.obtenerVentaConDetalles(ventaSeleccionadaLigera.getIdVenta());
        if (venta == null) {
            gridItems.setItems(new ArrayList<>());
            return;
        }

        LocalDate fechaCompra = venta.getFechaHoraVenta().toLocalDate();
        long diasTranscurridos = ChronoUnit.DAYS.between(fechaCompra, LocalDate.now());

        if (diasTranscurridos > 2) {
            lblAlertaTiempo.setText("⚠️ PRECAUCIÓN: Han transcurrido " + diasTranscurridos + " días desde la compra. Esta devolución excede el límite estándar de 2 días.");
            lblAlertaTiempo.setVisible(true);
        }

        List<Almacen> almacenesActivos = almacenService.listarTodos().stream()
                .filter(a -> a.getStatus() == StatusEntidad.ACTIVO).toList();

        for (DetalleVenta dv : venta.getDetallesVentas()) {
            LineaDevolucionUI lineaUI = new LineaDevolucionUI(dv, almacenesActivos, this::recalcularTotalesUI);
            lineasVista.add(lineaUI);
        }

        gridItems.setItems(lineasVista);
    }

    private void recalcularTotalesUI() {
        montoReembolsoCalculado = BigDecimal.ZERO;

        for (LineaDevolucionUI linea : lineasVista) {
            BigDecimal cant = linea.getCantidadIngresada();
            if (cant.compareTo(BigDecimal.ZERO) > 0) {
                montoReembolsoCalculado = montoReembolsoCalculado.add(
                        devolucionService.calcularMontoDetalle(linea.getDetalleOriginal(), cant)
                );
            }
        }

        montoReembolsoCalculado = montoReembolsoCalculado.setScale(2, RoundingMode.HALF_UP);
        lblMontoNotaCredito.setText("RD$ " + String.format("%,.2f", montoReembolsoCalculado));
    }

    private void ejecutarDevolucion() {
        Venta ventaSelected = cbVentasCerradas.getValue();
        if (ventaSelected == null) {
            mostrarNotificacion("Debe seleccionar una factura de venta válida", NotificationVariant.LUMO_ERROR);
            return;
        }
        if (txtJustificacion.isEmpty()) {
            mostrarNotificacion("La justificación es completamente obligatoria", NotificationVariant.LUMO_ERROR);
            return;
        }

        boolean hayArticulosAProcesar = lineasVista.stream().anyMatch(l -> l.getCantidadIngresada().compareTo(BigDecimal.ZERO) > 0);
        if (!hayArticulosAProcesar) {
            mostrarNotificacion("Debe especificar al menos un artículo con cantidad mayor a cero para devolver", NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            DevolucionVenta dev = new DevolucionVenta();
            dev.setCliente(ventaSelected.getCliente());
            dev.setEmpleado(securityService.obtenerEmpleadoAutenticado());
            dev.setRazonDevolucion(txtJustificacion.getValue().trim());
            dev.setMontoTotal(montoReembolsoCalculado);

            for (LineaDevolucionUI ui : lineasVista) {
                if (ui.getCantidadIngresada().compareTo(BigDecimal.ZERO) > 0) {
                    if (ui.getCbAlmacenDestino().isEmpty()) {
                        throw new IllegalArgumentException("Debe especificar el almacén de reingreso para el producto: " + ui.getDetalleOriginal().getProducto().getNombre());
                    }

                    DetalleDevVenta ddv = new DetalleDevVenta();
                    ddv.setDetalleVenta(ui.getDetalleOriginal());
                    ddv.setLote(ui.getDetalleOriginal().getLote());
                    ddv.setCantidadDevuelta(ui.getCantidadIngresada());
                    ddv.setAlmacenEntrada(ui.getCbAlmacenDestino().getValue());

                    dev.agregarDetalle(ddv);
                }
            }

            devolucionService.registrarDevolucion(dev, chkNotaCredito.getValue());
            mostrarNotificacion("Devolución procesada con éxito y stock reingresado.", NotificationVariant.LUMO_SUCCESS);

            limpiarPantalla();
            accionVolver.run();

        } catch (Exception ex) {
            mostrarNotificacion("Error: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void limpiarPantalla() {
        cbVentasCerradas.clear();
        txtJustificacion.clear();
        lineasVista.clear();
        gridItems.setItems(new ArrayList<>());
        montoReembolsoCalculado = BigDecimal.ZERO;
        lblMontoNotaCredito.setText("RD$ 0.00");
        lblAlertaTiempo.setVisible(false);
    }

    private void mostrarNotificacion(String msg, NotificationVariant variante) {
        Notification n = Notification.show(msg, 4000, Notification.Position.MIDDLE);
        n.addThemeVariants(variante);
    }


    private static class LineaDevolucionUI {
        private final DetalleVenta detalleOriginal;
        private final CantidadFraccionadaField fieldCantidad;
        private final ComboBox<Almacen> cbAlmacenDestino;

        public LineaDevolucionUI(DetalleVenta detalleOriginal, List<Almacen> almacenes, Runnable onValueChange) {
            this.detalleOriginal = detalleOriginal;

            this.fieldCantidad = new CantidadFraccionadaField();
            Producto prod = detalleOriginal.getProducto();
            this.fieldCantidad.configurarProducto(
                    prod.getContenidoPorEmpaque(),
                    Boolean.TRUE.equals(prod.getPermiteFraccionamiento()),
                    false,
                    FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(prod),
                    FormatoInventarioUtil.getNombreUnidadFraccionSafe(prod)
            );
            this.fieldCantidad.setValue(BigDecimal.ZERO);
            this.fieldCantidad.addValueChangeListener(e -> onValueChange.run());

            this.cbAlmacenDestino = new ComboBox<>();
            this.cbAlmacenDestino.setWidthFull();
            this.cbAlmacenDestino.setItems(almacenes);
            this.cbAlmacenDestino.setItemLabelGenerator(Almacen::getNombre);
            this.cbAlmacenDestino.setPlaceholder("Seleccione...");

            if (detalleOriginal.getLote() == null) {
                this.fieldCantidad.setEnabled(false);
                this.cbAlmacenDestino.setEnabled(false);
                this.cbAlmacenDestino.setPlaceholder("No despachado");
            }
        }

        public DetalleVenta getDetalleOriginal() { return detalleOriginal; }
        public CantidadFraccionadaField getFieldCantidad() { return fieldCantidad; }
        public ComboBox<Almacen> getCbAlmacenDestino() { return cbAlmacenDestino; }
        public BigDecimal getCantidadIngresada() { return fieldCantidad.getValue() != null ? fieldCantidad.getValue() : BigDecimal.ZERO; }
    }
}