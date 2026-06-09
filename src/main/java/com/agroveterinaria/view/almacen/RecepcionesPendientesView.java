package com.agroveterinaria.view.almacen;

import com.agroveterinaria.dto.recepcion.GastoOperativoUI;
import com.agroveterinaria.dto.recepcion.RecepcionItemUI;
import com.agroveterinaria.entity.Almacen;
import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.entity.DetalleCompra;
import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@Route("almacen/recepciones-pendientes")
@PageTitle("Recepciones Pendientes")
@RolesAllowed({"ADMINISTRADOR", "ASISTENTE"})
public class RecepcionesPendientesView extends VerticalLayout {

    private final CompraService compraService;
    private final AlmacenService almacenService;
    private final LoteService loteService;
    private final VehiculoService vehiculoService;
    private final EmpleadoService empleadoService;
    private final RutaService rutaService;
    private final RecepcionService recepcionService;
    private final Grid<Compra> gridCompras;

    public RecepcionesPendientesView(CompraService compraService, AlmacenService almacenService, LoteService loteService, VehiculoService vehiculoService, EmpleadoService empleadoService, RutaService rutaService, RecepcionService recepcionService) {
        this.compraService = compraService;
        this.almacenService = almacenService;
        this.loteService = loteService;
        this.vehiculoService = vehiculoService;
        this.empleadoService = empleadoService;
        this.rutaService = rutaService;
        this.recepcionService = recepcionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Recepciones de Mercancía Pendientes");
        titulo.getStyle().set("margin-top", "0");

        gridCompras = new Grid<>(Compra.class, false);
        gridCompras.setSizeFull();
        gridCompras.addThemeNames("row-stripes");
        gridCompras.addClassName("recepciones-pendientes-grid");

        configurarGrid();
        actualizarGrid();

        add(titulo, gridCompras);
    }

    private void configurarGrid() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        gridCompras.addColumn(Compra::getIdCompra)
                .setHeader("No. Orden").setWidth("100px").setFlexGrow(0);

        gridCompras.addColumn(compra -> compra.getFechaHoraCompra().format(formatter))
                .setHeader("Fecha de Compra").setFlexGrow(1);

        gridCompras.addColumn(compra -> compra.getProveedor().getNombre())
                .setHeader("Proveedor").setFlexGrow(2);

        gridCompras.addColumn(compra -> String.format("RD$ %,.2f", compra.getTotal()))
                .setHeader("Total Factura").setFlexGrow(1);

        gridCompras.addComponentColumn(compra -> {
            Span badge = new Span(compra.getEstadoRecepcion().getEtiqueta());
            badge.getElement().getThemeList().add("badge warning");
            return badge;
        }).setHeader("Estado").setFlexGrow(1);

        gridCompras.addComponentColumn(compra -> {
            Button btnRecibir = new Button("Recibir Mercancía", new Icon(VaadinIcon.TRUCK));
            btnRecibir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnRecibir.addClickListener(e -> abrirDialogoRecepcion(compra));
            return btnRecibir;
        }).setHeader("Acción").setWidth("220px").setFlexGrow(0);
    }

    private void actualizarGrid() {
        gridCompras.setItems(compraService.listarComprasPendientes());
    }

    private void abrirDialogoRecepcion(Compra compra) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1050px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3("Recepción de Mercancía - Orden #" + compra.getIdCompra());
        titulo.getStyle().set("margin-top", "0");

        List<DetalleCompra> detallesCompra = compraService.obtenerDetallesPorCompra(compra.getIdCompra());
        List<RecepcionItemUI> itemsFisicos = new ArrayList<>();

        for (DetalleCompra dc : detallesCompra) {
            BigDecimal pendiente = compraService.calcularCantidadPendiente(dc);
            if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
                itemsFisicos.add(new RecepcionItemUI(dc, pendiente));
            }
        }

        Grid<RecepcionItemUI> gridRecepcion = new Grid<>(RecepcionItemUI.class, false);
        gridRecepcion.setItems(itemsFisicos);
        gridRecepcion.addThemeNames("row-stripes");
        gridRecepcion.setHeight("350px");

        gridRecepcion.addColumn(item -> item.getDetalle().getProducto().getNombre())
                .setHeader("Producto").setFlexGrow(2);

        gridRecepcion.addComponentColumn(item -> {
            com.vaadin.flow.component.textfield.BigDecimalField txtCant = new com.vaadin.flow.component.textfield.BigDecimalField();
            txtCant.setWidthFull();
            txtCant.setValue(item.getCantidadRecibida());
            txtCant.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.ON_BLUR);
            txtCant.addValueChangeListener(e -> {
                BigDecimal nuevaCant = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;

                item.setCantidadRecibida(BigDecimal.ZERO);
                BigDecimal sumaOtros = calcularSumaDelMismoProducto(itemsFisicos, item);
                BigDecimal maxPermitidoParaEsteCampo = item.getCantidadMaximaPermitida().subtract(sumaOtros);

                if (nuevaCant.compareTo(maxPermitidoParaEsteCampo) > 0) {
                    Notification.show("No puedes recibir más de " + maxPermitidoParaEsteCampo + " en esta fila.", 3000, Notification.Position.MIDDLE);
                    txtCant.setValue(maxPermitidoParaEsteCampo);
                    item.setCantidadRecibida(maxPermitidoParaEsteCampo);
                } else {
                    item.setCantidadRecibida(nuevaCant);
                }
                gridRecepcion.getDataProvider().refreshAll();
            });
            return txtCant;
        }).setHeader("Cant. a Recibir").setWidth("120px").setFlexGrow(0);

        gridRecepcion.addColumn(item -> {
            BigDecimal disponible = calcularRestanteTotal(itemsFisicos, item);
            return disponible.compareTo(BigDecimal.ZERO) > 0 ? disponible.toString() : "0.00";
        }).setHeader("Pendiente Global").setWidth("130px").setFlexGrow(0);

        gridRecepcion.addComponentColumn(item -> {
            ComboBox<Almacen> cbAlmacen = new ComboBox<>();
            cbAlmacen.setItems(almacenService.listarTodos().stream().filter(a -> a.getStatus() == StatusEntidad.ACTIVO).toList());
            cbAlmacen.setItemLabelGenerator(Almacen::getNombre);
            cbAlmacen.setWidthFull();
            cbAlmacen.setValue(item.getAlmacenDestino());
            cbAlmacen.addValueChangeListener(e -> item.setAlmacenDestino(e.getValue()));
            return cbAlmacen;
        }).setHeader("Almacén Destino").setFlexGrow(2);

        gridRecepcion.addComponentColumn(item -> {
            ComboBox<Lote> cbLote = new ComboBox<>();
            cbLote.setPlaceholder("Seleccione o escriba...");
            cbLote.setWidthFull();

            List<Lote> lotesExistentes = loteService.buscarPorProducto(item.getDetalle().getProducto());
            cbLote.setItems(lotesExistentes);
            cbLote.setItemLabelGenerator(lote -> lote.getNumeroLote() != null ? lote.getNumeroLote() : "Sin número");

            if (item.getNumeroLote() != null) {
                Lote loteMatcheado = lotesExistentes.stream()
                        .filter(l -> item.getNumeroLote().equals(l.getNumeroLote()))
                        .findFirst().orElse(null);

                if (loteMatcheado != null) {
                    cbLote.setValue(loteMatcheado);
                } else {
                    cbLote.setAllowCustomValue(true);
                    cbLote.setValue(new Lote(null, item.getDetalle().getProducto(), null, item.getNumeroLote()));
                }
            }
            cbLote.setAllowCustomValue(true);
            cbLote.addCustomValueSetListener(e -> {
                String textoNuevoLote = e.getDetail();
                item.setNumeroLote(textoNuevoLote);
                item.setFechaVencimiento(null);
                gridRecepcion.getDataProvider().refreshItem(item);
            });

            cbLote.addValueChangeListener(e -> {
                Lote loteSeleccionado = e.getValue();
                if (loteSeleccionado != null) {
                    item.setNumeroLote(loteSeleccionado.getNumeroLote());
                    if (loteSeleccionado.getFechaVencimiento() != null) {
                        item.setFechaVencimiento(loteSeleccionado.getFechaVencimiento());
                    }
                    gridRecepcion.getDataProvider().refreshItem(item);
                }
            });

            return cbLote;
        }).setHeader("No. Lote").setFlexGrow(1);

        gridRecepcion.addComponentColumn(item -> {
            DatePicker dpVencimiento = new DatePicker();
            dpVencimiento.setWidthFull();
            dpVencimiento.setValue(item.getFechaVencimiento());
            boolean esLoteExistente = loteService.buscarPorProducto(item.getDetalle().getProducto())
                    .stream().anyMatch(l -> item.getNumeroLote() != null && item.getNumeroLote().equals(l.getNumeroLote()));

            dpVencimiento.setReadOnly(esLoteExistente);

            dpVencimiento.addValueChangeListener(e -> {
                if(e.isFromClient()) {
                    item.setFechaVencimiento(e.getValue());
                }
            });
            return dpVencimiento;
        }).setHeader("Vencimiento").setFlexGrow(1);

        gridRecepcion.addComponentColumn(item -> {
            HorizontalLayout acciones = new HorizontalLayout();

            Button btnAdd = new Button(new Icon(VaadinIcon.PLUS));
            btnAdd.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            btnAdd.addClickListener(e -> {
                BigDecimal restante = calcularRestanteTotal(itemsFisicos, item);
                if (restante.compareTo(BigDecimal.ZERO) > 0) {
                    RecepcionItemUI nuevoItem = new RecepcionItemUI(item.getDetalle(), item.getCantidadMaximaPermitida());
                    nuevoItem.setCantidadRecibida(restante);
                    itemsFisicos.add(itemsFisicos.indexOf(item) + 1, nuevoItem);
                    gridRecepcion.getDataProvider().refreshAll();
                } else {
                    Notification.show("No queda cantidad pendiente para fraccionar de este producto.", 3000, Notification.Position.MIDDLE);
                }
            });

            Button btnRemove = new Button(new Icon(VaadinIcon.MINUS));
            btnRemove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnRemove.addClickListener(e -> {
                long copias = itemsFisicos.stream().filter(i -> i.getDetalle().getIdDetalleCompra().equals(item.getDetalle().getIdDetalleCompra())).count();
                if (copias > 1) {
                    itemsFisicos.remove(item);
                    gridRecepcion.getDataProvider().refreshAll();
                } else {
                    Notification.show("No puedes eliminar la única fila del producto.", 3000, Notification.Position.MIDDLE);
                }
            });

            acciones.add(btnAdd, btnRemove);
            return acciones;
        }).setHeader("Dividir").setWidth("110px").setFlexGrow(0);


        VerticalLayout seccionLogistica = new VerticalLayout();
        seccionLogistica.setPadding(false);
        seccionLogistica.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("padding", "15px")
                .set("margin-top", "15px");

        H4 tituloLogistica = new H4("Logística de Abastecimiento");
        tituloLogistica.getStyle().set("margin", "0");

        com.vaadin.flow.component.radiobutton.RadioButtonGroup<String> rbgTipoLogistica = new com.vaadin.flow.component.radiobutton.RadioButtonGroup<>();
        rbgTipoLogistica.setLabel("Seleccione la modalidad de traslado:");
        rbgTipoLogistica.setItems(
                "Entrega Regular (Sin costos)",
                "Transporte Interno (Vehículo propio)",
                "Flete / Delivery Externo",
                "Mixto (Flete parcial + Transporte propio)"
        );
        rbgTipoLogistica.setValue("Entrega Regular (Sin costos)");

        com.vaadin.flow.component.textfield.BigDecimalField txtCostoFlete = new com.vaadin.flow.component.textfield.BigDecimalField("Costo de Flete / Delivery Externo");
        txtCostoFlete.setWidthFull();
        txtCostoFlete.setPrefixComponent(new Span("RD$"));
        txtCostoFlete.setVisible(false);

        HorizontalLayout formCamionYChofer = new HorizontalLayout();
        formCamionYChofer.setWidthFull();
        formCamionYChofer.setVisible(false);

        ComboBox<com.agroveterinaria.entity.Vehiculo> cbVehiculo = new ComboBox<>("Vehículo");
        cbVehiculo.setItems(vehiculoService.listarTodos());
        cbVehiculo.setItemLabelGenerator(v -> v.getPlaca() + " - " + v.getModelo());
        cbVehiculo.setWidthFull();

        ComboBox<com.agroveterinaria.entity.Empleado> cbConductor = new ComboBox<>("Conductor");
        cbConductor.setItems(empleadoService.findByCargo(RolEmpleado.CONDUCTOR));
        cbConductor.setItemLabelGenerator(e -> e.getPersona().getNombre());
        cbConductor.setWidthFull();

        ComboBox<com.agroveterinaria.entity.Ruta> cbRuta = new ComboBox<>("Ruta");
        cbRuta.setItems(rutaService.listarTodos());
        cbRuta.setItemLabelGenerator(r -> r.getDistanciaKm() + " km");
        cbRuta.setWidthFull();

        formCamionYChofer.add(cbVehiculo, cbConductor, cbRuta);

        VerticalLayout layoutTablaGastos = new VerticalLayout();
        layoutTablaGastos.setPadding(false);
        layoutTablaGastos.setVisible(false);

        H4 tituloGastos = new H4("Gastos Operativos de Nuestro Vehículo");
        tituloGastos.getStyle().set("margin-top", "10px").set("margin-bottom", "5px");

        List<GastoOperativoUI> listaGastos = new ArrayList<>();
        Grid<GastoOperativoUI> gridGastos = new Grid<>(GastoOperativoUI.class, false);
        gridGastos.setItems(listaGastos);
        gridGastos.addThemeNames("row-stripes");
        gridGastos.setHeight("160px");

        gridGastos.addComponentColumn(gasto -> {
            TextField txtNota = new TextField();
            txtNota.setWidthFull();
            txtNota.setPlaceholder("Ej. Combustible, Peaje, Dieta...");
            txtNota.setValue(gasto.getNotas() != null ? gasto.getNotas() : "");
            txtNota.addValueChangeListener(e -> gasto.setNotas(e.getValue()));
            return txtNota;
        }).setHeader("Concepto / Descripción").setFlexGrow(2);

        gridGastos.addComponentColumn(gasto -> {
            com.vaadin.flow.component.textfield.BigDecimalField txtMonto = new com.vaadin.flow.component.textfield.BigDecimalField();
            txtMonto.setWidthFull();
            txtMonto.setPrefixComponent(new Span("RD$"));
            txtMonto.setValue(gasto.getMonto());
            txtMonto.addValueChangeListener(e -> gasto.setMonto(e.getValue()));
            return txtMonto;
        }).setHeader("Monto").setWidth("150px").setFlexGrow(0);

        gridGastos.addComponentColumn(gasto -> {
            Button btnQuitarGasto = new Button(new Icon(VaadinIcon.TRASH));
            btnQuitarGasto.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnQuitarGasto.addClickListener(e -> {
                listaGastos.remove(gasto);
                gridGastos.getDataProvider().refreshAll();
            });
            return btnQuitarGasto;
        }).setWidth("80px").setFlexGrow(0);

        Button btnAgregarGasto = new Button("Añadir Concepto de Gasto", new Icon(VaadinIcon.PLUS));
        btnAgregarGasto.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnAgregarGasto.addClickListener(e -> {
            listaGastos.add(new GastoOperativoUI());
            gridGastos.getDataProvider().refreshAll();
        });

        layoutTablaGastos.add(tituloGastos, gridGastos, btnAgregarGasto);

        rbgTipoLogistica.addValueChangeListener(e -> {
            String seleccion = e.getValue();

            if ("Entrega Regular (Sin costos)".equals(seleccion)) {
                formCamionYChofer.setVisible(false);
                txtCostoFlete.setVisible(false);
                layoutTablaGastos.setVisible(false);

                txtCostoFlete.clear();
                listaGastos.clear();

            } else if ("Transporte Interno (Vehículo propio)".equals(seleccion)) {
                formCamionYChofer.setVisible(true);
                txtCostoFlete.setVisible(false);
                layoutTablaGastos.setVisible(true);

                txtCostoFlete.clear();

            } else if ("Flete / Delivery Externo".equals(seleccion)) {
                formCamionYChofer.setVisible(false);
                txtCostoFlete.setVisible(true);
                layoutTablaGastos.setVisible(false);

                listaGastos.clear();

            } else if ("Mixto (Flete parcial + Transporte propio)".equals(seleccion)) {
                formCamionYChofer.setVisible(true);
                txtCostoFlete.setVisible(true);
                layoutTablaGastos.setVisible(true);
            }
            gridGastos.getDataProvider().refreshAll();
        });

        seccionLogistica.add(tituloLogistica, rbgTipoLogistica, txtCostoFlete, formCamionYChofer, layoutTablaGastos);


        Button btnProcesar = new Button("Procesar Recepción", new Icon(VaadinIcon.CHECK_CIRCLE));
        btnProcesar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnProcesar.addClickListener(e -> {
            boolean hayProductosARecibir = false;
            for (RecepcionItemUI item : itemsFisicos) {
                if (item.getCantidadRecibida() != null && item.getCantidadRecibida().compareTo(BigDecimal.ZERO) > 0) {
                    hayProductosARecibir = true;
                    if (item.getAlmacenDestino() == null) {
                        Notification notif = Notification.show("Error: Asigne un Almacén Destino para " + item.getDetalle().getProducto().getNombre(), 4000, Notification.Position.MIDDLE);
                        notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                }
            }
            if (!hayProductosARecibir) {
                Notification.show("No has asignado cantidades a recibir.", 3000, Notification.Position.MIDDLE);
                return;
            }
            mostrarDialogoConfirmacion(
                    compra, itemsFisicos, rbgTipoLogistica.getValue(), txtCostoFlete.getValue(),
                    cbVehiculo.getValue(), cbConductor.getValue(), cbRuta.getValue(), listaGastos, dialog
            );
        });

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnCancelar, btnProcesar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "20px");

        VerticalLayout contenido = new VerticalLayout(titulo, gridRecepcion, seccionLogistica, botones);
        contenido.setPadding(true);

        dialog.add(contenido);
        dialog.open();
    }


    private BigDecimal calcularSumaDelMismoProducto(List<RecepcionItemUI> items, RecepcionItemUI itemActual) {
        return items.stream()
                .filter(i -> i.getDetalle().getIdDetalleCompra().equals(itemActual.getDetalle().getIdDetalleCompra()))
                .map(RecepcionItemUI::getCantidadRecibida)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularRestanteTotal(List<RecepcionItemUI> items, RecepcionItemUI itemActual) {
        BigDecimal sumaTotal = calcularSumaDelMismoProducto(items, itemActual);
        return itemActual.getCantidadMaximaPermitida().subtract(sumaTotal);
    }

    private void mostrarDialogoConfirmacion(
            Compra compra, List<RecepcionItemUI> itemsFisicos, String tipoLogistica,
            BigDecimal costoFleteExterno, com.agroveterinaria.entity.Vehiculo vehiculo,
            com.agroveterinaria.entity.Empleado conductor, com.agroveterinaria.entity.Ruta ruta,
            List<GastoOperativoUI> gastosInternos, Dialog dialogPrincipal) {

        Dialog dialogConfirmacion = new Dialog();
        dialogConfirmacion.setWidth("450px");

        H3 titulo = new H3("Confirmar Recepción");
        titulo.getStyle().set("margin-top", "0").set("color", "var(--lumo-primary-color)");

        long cantProductos = itemsFisicos.stream()
                .filter(i -> i.getCantidadRecibida() != null && i.getCantidadRecibida().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal totalGastosInternos = gastosInternos.stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoFlete = costoFleteExterno != null ? costoFleteExterno : BigDecimal.ZERO;
        BigDecimal totalLogistica = totalGastosInternos.add(costoFlete);

        VerticalLayout resumen = new VerticalLayout();
        resumen.setPadding(false);
        resumen.setSpacing(false);
        resumen.add(new Span("¿Estás seguro de procesar esta entrada al inventario?"));
        resumen.add(new com.vaadin.flow.component.html.Hr());

        Span lblArticulos = new Span("Productos a ingresar: " + cantProductos + " filas.");
        lblArticulos.getStyle().set("font-weight", "bold");
        resumen.add(lblArticulos);

        Span lblLogistica = new Span("Modalidad: " + tipoLogistica);
        resumen.add(lblLogistica);

        if (totalLogistica.compareTo(BigDecimal.ZERO) > 0) {
            Span lblCostos = new Span(String.format("Costos Logísticos: RD$ %,.2f", totalLogistica));
            lblCostos.getStyle().set("color", "var(--lumo-error-text-color)").set("font-weight", "bold");
            resumen.add(lblCostos);
        }


        Button btnConfirmar = new Button("Sí, guardar entrada", new Icon(VaadinIcon.CHECK));
        btnConfirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnConfirmar.setDisableOnClick(true);
        btnConfirmar.addClickListener(e -> {
            try {
                recepcionService.procesarRecepcionTransaccional(
                        compra.getIdCompra(), itemsFisicos, tipoLogistica,
                        costoFleteExterno, vehiculo, conductor, ruta, gastosInternos
                );

                Notification successNotif = Notification.show("Recepción consolidada exitosamente.", 4000, Notification.Position.BOTTOM_END);
                successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialogConfirmacion.close();
                dialogPrincipal.close();
                actualizarGrid();
            } catch (Exception ex) {
                Notification errorNotif = Notification.show("Error al guardar: " + ex.getMessage(), 6000, Notification.Position.MIDDLE);
                errorNotif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                dialogConfirmacion.close();
                btnConfirmar.setEnabled(true);
            }
        });

        Button btnCancelar = new Button("Revisar de nuevo", e -> dialogConfirmacion.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnCancelar, btnConfirmar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "20px");

        dialogConfirmacion.add(titulo, resumen, botones);
        dialogConfirmacion.open();
    }
}