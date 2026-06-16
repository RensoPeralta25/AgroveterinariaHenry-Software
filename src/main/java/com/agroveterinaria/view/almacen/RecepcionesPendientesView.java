package com.agroveterinaria.view.almacen;

import com.agroveterinaria.dto.recepcion.GastoOperativoUI;
import com.agroveterinaria.dto.recepcion.RecepcionItemUI;
import com.agroveterinaria.dto.recepcion.RecepcionResumenDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@Route("almacen/recepciones-pendientes")
@PageTitle("Recepciones Pendientes")
@RolesAllowed({"ADMINISTRADOR", "ASISTENTE"})
public class RecepcionesPendientesView extends VerticalLayout {

    private final AlmacenService almacenService;
    private final LoteService loteService;
    private final VehiculoService vehiculoService;
    private final EmpleadoService empleadoService;
    private final RutaService rutaService;
    private final RecepcionService recepcionService;

    private Grid<RecepcionResumenDTO> gridRecepciones;
    private ListDataProvider<RecepcionResumenDTO> dataProvider;

    public RecepcionesPendientesView(AlmacenService almacenService, LoteService loteService,
                                     VehiculoService vehiculoService, EmpleadoService empleadoService,
                                     RutaService rutaService, RecepcionService recepcionService) {
        this.almacenService = almacenService;
        this.loteService = loteService;
        this.vehiculoService = vehiculoService;
        this.empleadoService = empleadoService;
        this.rutaService = rutaService;
        this.recepcionService = recepcionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Recepciones Pendientes");
        titulo.getStyle().set("margin-top", "0");

        HorizontalLayout filtros = construirFiltros();
        construirGrid();
        actualizarGrid();

        add(titulo, filtros, gridRecepciones);
    }

    private HorizontalLayout construirFiltros() {
        Button btnTodos = new Button("Todas", e -> filtrarPorTipo(""));
        btnTodos.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button btnCompras = new Button("Compras", e -> filtrarPorTipo("Compra"));
        Button btnTransferencias = new Button("Transferencias", e -> filtrarPorTipo("Transferencia"));

        return new HorizontalLayout(btnTodos, btnCompras, btnTransferencias);
    }

    private void construirGrid() {
        gridRecepciones = new Grid<>(RecepcionResumenDTO.class, false);
        gridRecepciones.setSizeFull();
        gridRecepciones.addThemeNames("row-stripes");
        gridRecepciones.addClassName("transferencia-grid");

        gridRecepciones.addColumn(RecepcionResumenDTO::getCodigo).setHeader("Documento").setWidth("120px").setFlexGrow(0);

        gridRecepciones.addComponentColumn(dto -> {
            Span badge = new Span(dto.getTipo());
            badge.getElement().getThemeList().add("badge " + (dto.getTipo().equals("Compra") ? "success" : "contrast"));
            return badge;
        }).setHeader("Tipo").setFlexGrow(0).setWidth("130px");

        gridRecepciones.addColumn(RecepcionResumenDTO::getOrigen).setHeader("Origen").setFlexGrow(2);

        gridRecepciones.addColumn(RecepcionResumenDTO::getFechaFormateada)
                .setComparator((d1, d2) -> d1.getFechaRaw().compareTo(d2.getFechaRaw()))
                .setHeader("Fecha de Emisión").setFlexGrow(1);

        gridRecepciones.addColumn(RecepcionResumenDTO::getEstado).setHeader("Estado").setFlexGrow(1);

        gridRecepciones.addComponentColumn(dto -> {
            Button btnRecibir = new Button("Recibir Mercancía", new Icon(VaadinIcon.TRUCK));
            btnRecibir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnRecibir.addClickListener(e -> abrirDialogoRecepcion(dto));
            return btnRecibir;
        }).setHeader("Acción").setWidth("220px").setFlexGrow(0);
    }

    private void actualizarGrid() {
        List<RecepcionResumenDTO> lista = recepcionService.obtenerColaRecepciones();
        dataProvider = new ListDataProvider<>(lista);
        gridRecepciones.setDataProvider(dataProvider);
    }

    private void filtrarPorTipo(String tipo) {
        if (dataProvider != null) {
            dataProvider.setFilter(dto -> tipo.isEmpty() || dto.getTipo().equals(tipo));
        }
    }

    private void abrirDialogoRecepcion(RecepcionResumenDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1050px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3("Recepción de Mercancía - " + dto.getCodigo());
        titulo.getStyle().set("margin-top", "0");

        Long idDocumento = "Compra".equals(dto.getTipo()) ?
                dto.getCompraOriginal().getIdCompra() :
                dto.getTransferenciaOriginal().getIdTransferencia();

        List<RecepcionItemUI> itemsFisicos = recepcionService.obtenerItemsPendientes(dto.getTipo(), idDocumento);

        Grid<RecepcionItemUI> gridRecepcion = new Grid<>(RecepcionItemUI.class, false);
        gridRecepcion.setItems(itemsFisicos);
        gridRecepcion.addThemeNames("row-stripes");
        gridRecepcion.setHeight("350px");

        gridRecepcion.addColumn(item -> item.getProducto().getNombre()).setHeader("Producto").setFlexGrow(2);

        gridRecepcion.addComponentColumn(item -> {
            com.vaadin.flow.component.textfield.BigDecimalField txtCant = new com.vaadin.flow.component.textfield.BigDecimalField();
            txtCant.setWidthFull();
            txtCant.setValue(item.getCantidadRecibida());
            txtCant.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.ON_BLUR);
            txtCant.addValueChangeListener(e -> {
                BigDecimal nuevaCant = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;
                item.setCantidadRecibida(BigDecimal.ZERO);
                BigDecimal sumaOtros = calcularSumaDelMismoProducto(itemsFisicos, item);
                BigDecimal maxPermitido = item.getCantidadMaximaPermitida().subtract(sumaOtros);

                if (nuevaCant.compareTo(maxPermitido) > 0) {
                    Notification.show("No puedes recibir más de " + maxPermitido + " en esta fila.", 3000, Notification.Position.MIDDLE);
                    txtCant.setValue(maxPermitido);
                    item.setCantidadRecibida(maxPermitido);
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
            if (item.getDetalleTransferencia() != null) {
                cbAlmacen.setReadOnly(true);
            } else {
                cbAlmacen.addValueChangeListener(e -> item.setAlmacenDestino(e.getValue()));
            }
            return cbAlmacen;
        }).setHeader("Almacén Destino").setFlexGrow(2);

        gridRecepcion.addComponentColumn(item -> {
            ComboBox<Lote> cbLote = new ComboBox<>();
            cbLote.setPlaceholder("Seleccione o escriba...");
            cbLote.setWidthFull();

            List<Lote> lotesExistentes = loteService.buscarPorProducto(item.getProducto());
            cbLote.setItems(lotesExistentes);
            cbLote.setItemLabelGenerator(lote -> lote.getNumeroLote() != null ? lote.getNumeroLote() : "Sin número");

            if (item.getDetalleTransferencia() != null) {
                cbLote.setValue(item.getDetalleTransferencia().getLote());
                cbLote.setReadOnly(true);
            } else {
                if (item.getNumeroLote() != null) {
                    Lote loteMatcheado = lotesExistentes.stream().filter(l -> item.getNumeroLote().equals(l.getNumeroLote())).findFirst().orElse(null);
                    if (loteMatcheado != null) cbLote.setValue(loteMatcheado);
                    else {
                        cbLote.setAllowCustomValue(true);
                        cbLote.setValue(new Lote(null, item.getProducto(), null, item.getNumeroLote()));
                    }
                }
                cbLote.setAllowCustomValue(true);
                cbLote.addCustomValueSetListener(e -> {
                    item.setNumeroLote(e.getDetail());
                    item.setFechaVencimiento(null);
                    gridRecepcion.getDataProvider().refreshItem(item);
                });
                cbLote.addValueChangeListener(e -> {
                    if (e.getValue() != null) {
                        item.setNumeroLote(e.getValue().getNumeroLote());
                        if (e.getValue().getFechaVencimiento() != null) item.setFechaVencimiento(e.getValue().getFechaVencimiento());
                        gridRecepcion.getDataProvider().refreshItem(item);
                    }
                });
            }
            return cbLote;
        }).setHeader("No. Lote").setFlexGrow(1);

        gridRecepcion.addComponentColumn(item -> {
            DatePicker dpVencimiento = new DatePicker();
            dpVencimiento.setWidthFull();
            dpVencimiento.setValue(item.getFechaVencimiento());

            if (item.getDetalleTransferencia() != null) {
                dpVencimiento.setReadOnly(true);
            } else {
                boolean esLoteExistente = loteService.buscarPorProducto(item.getProducto())
                        .stream().anyMatch(l -> item.getNumeroLote() != null && item.getNumeroLote().equals(l.getNumeroLote()));
                dpVencimiento.setReadOnly(esLoteExistente);
                dpVencimiento.addValueChangeListener(e -> {
                    if(e.isFromClient()) item.setFechaVencimiento(e.getValue());
                });
            }
            return dpVencimiento;
        }).setHeader("Vencimiento").setFlexGrow(1);

        gridRecepcion.addComponentColumn(item -> {

            if (item.getDetalleTransferencia() != null) {
                Span textoBloqueado = new Span("-");
                textoBloqueado.getStyle()
                        .set("color", "var(--lumo-disabled-text-color)")
                        .set("text-align", "center")
                        .set("display", "block");
                return textoBloqueado;
            }

            HorizontalLayout acciones = new HorizontalLayout();

            Button btnAdd = new Button(new Icon(VaadinIcon.PLUS));
            btnAdd.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            btnAdd.addClickListener(e -> {
                BigDecimal restante = calcularRestanteTotal(itemsFisicos, item);
                if (restante.compareTo(BigDecimal.ZERO) > 0) {
                    RecepcionItemUI nuevoItem = new RecepcionItemUI(item.getDetalleCompra(), item.getCantidadMaximaPermitida());
                    nuevoItem.setCantidadRecibida(restante);
                    itemsFisicos.add(itemsFisicos.indexOf(item) + 1, nuevoItem);
                    gridRecepcion.getDataProvider().refreshAll();
                }
            });

            Button btnRemove = new Button(new Icon(VaadinIcon.MINUS));
            btnRemove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnRemove.addClickListener(e -> {
                long copias = itemsFisicos.stream().filter(i -> i.getDetalleCompra().equals(item.getDetalleCompra())).count();
                if (copias > 1) {
                    itemsFisicos.remove(item);
                    gridRecepcion.getDataProvider().refreshAll();
                }
            });

            acciones.add(btnAdd, btnRemove);
            return acciones;

        }).setHeader("Dividir").setWidth("110px").setFlexGrow(0);


        VerticalLayout seccionLogistica = new VerticalLayout();
        seccionLogistica.setPadding(false);
        seccionLogistica.getStyle().set("border", "1px solid #e0e0e0").set("border-radius", "8px").set("padding", "15px").set("margin-top", "15px");

        H4 tituloLogistica = new H4("Logística de Abastecimiento");
        tituloLogistica.getStyle().set("margin", "0");

        com.vaadin.flow.component.radiobutton.RadioButtonGroup<String> rbgTipoLogistica = new com.vaadin.flow.component.radiobutton.RadioButtonGroup<>();
        rbgTipoLogistica.setLabel("Seleccione la modalidad de traslado:");
        rbgTipoLogistica.setItems("Entrega Regular (Sin costos)", "Transporte Interno (Vehículo propio)", "Flete / Delivery Externo", "Mixto (Flete parcial + Transporte propio)");
        rbgTipoLogistica.setValue("Entrega Regular (Sin costos)");

        com.vaadin.flow.component.textfield.BigDecimalField txtCostoFlete = new com.vaadin.flow.component.textfield.BigDecimalField("Costo de Flete / Delivery Externo");
        txtCostoFlete.setWidthFull();
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

        H4 tituloGastos = new H4("Gastos Operativos (Liquidación de Viaje)");
        tituloGastos.getStyle().set("margin-top", "10px").set("margin-bottom", "5px");

        List<GastoOperativoUI> listaGastos = new ArrayList<>();
        Grid<GastoOperativoUI> gridGastos = new Grid<>(GastoOperativoUI.class, false);
        gridGastos.setItems(listaGastos);
        gridGastos.addThemeNames("row-stripes");
        gridGastos.setHeight("160px");

        gridGastos.addComponentColumn(gasto -> {
            TextField txtNota = new TextField();
            txtNota.setWidthFull();
            txtNota.setPlaceholder("Ej. Combustible, Peaje...");
            txtNota.setValue(gasto.getNotas() != null ? gasto.getNotas() : "");
            txtNota.addValueChangeListener(ev -> gasto.setNotas(ev.getValue()));
            return txtNota;
        }).setHeader("Concepto / Descripción").setFlexGrow(2);

        gridGastos.addComponentColumn(gasto -> {
            com.vaadin.flow.component.textfield.BigDecimalField txtMonto = new com.vaadin.flow.component.textfield.BigDecimalField();
            txtMonto.setWidthFull();
            txtMonto.setPrefixComponent(new Span("RD$"));
            txtMonto.setValue(gasto.getMonto());
            txtMonto.addValueChangeListener(ev -> gasto.setMonto(ev.getValue()));
            return txtMonto;
        }).setHeader("Monto").setWidth("150px").setFlexGrow(0);

        gridGastos.addComponentColumn(gasto -> {
            Button btnQuitarGasto = new Button(new Icon(VaadinIcon.TRASH));
            btnQuitarGasto.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnQuitarGasto.addClickListener(ev -> {
                listaGastos.remove(gasto);
                gridGastos.getDataProvider().refreshAll();
            });
            return btnQuitarGasto;
        }).setWidth("80px").setFlexGrow(0);

        Button btnAgregarGasto = new Button("Añadir Concepto de Gasto", new Icon(VaadinIcon.PLUS));
        btnAgregarGasto.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnAgregarGasto.addClickListener(ev -> {
            listaGastos.add(new GastoOperativoUI());
            gridGastos.getDataProvider().refreshAll();
        });

        layoutTablaGastos.add(tituloGastos, gridGastos, btnAgregarGasto);

        rbgTipoLogistica.addValueChangeListener(e -> {
            String sel = e.getValue();
            formCamionYChofer.setVisible(sel.contains("Interno") || sel.contains("Mixto"));
            txtCostoFlete.setVisible(sel.contains("Externo") || sel.contains("Mixto"));
            layoutTablaGastos.setVisible(sel.contains("Interno") || sel.contains("Mixto"));
        });

        seccionLogistica.add(tituloLogistica, rbgTipoLogistica, txtCostoFlete, formCamionYChofer, layoutTablaGastos);

        if ("Transferencia".equals(dto.getTipo())) {
            seccionLogistica.setVisible(true);

            rbgTipoLogistica.setValue("Transporte Interno (Vehículo propio)");
            rbgTipoLogistica.setReadOnly(true);

            formCamionYChofer.setVisible(true);
            layoutTablaGastos.setVisible(true);
            txtCostoFlete.setVisible(false);

            if (dto.getTransporteDespacho() != null) {
                cbVehiculo.setValue(dto.getTransporteDespacho().getVehiculo());
                cbVehiculo.setReadOnly(true);

                cbConductor.setValue(dto.getTransporteDespacho().getConductor());
                cbConductor.setReadOnly(true);

                if(dto.getTransporteDespacho().getRuta() != null) {
                    cbRuta.setValue(dto.getTransporteDespacho().getRuta());
                    cbRuta.setReadOnly(true);
                }
            }
        } else {
            seccionLogistica.setVisible(true);
            rbgTipoLogistica.setReadOnly(false);
            cbVehiculo.setReadOnly(false);
            cbConductor.setReadOnly(false);
            cbRuta.setReadOnly(false);
        }

        Button btnProcesar = new Button("Procesar Recepción", new Icon(VaadinIcon.CHECK_CIRCLE));
        btnProcesar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnProcesar.addClickListener(e -> {
            Long idDoc = "Compra".equals(dto.getTipo()) ? dto.getCompraOriginal().getIdCompra() : dto.getTransferenciaOriginal().getIdTransferencia();
            try {
                recepcionService.procesarRecepcionTransaccional(
                        dto.getTipo(), idDoc, itemsFisicos, rbgTipoLogistica.getValue(),
                        txtCostoFlete.getValue(), cbVehiculo.getValue(), cbConductor.getValue(),
                        cbRuta.getValue(), listaGastos
                );
                Notification.show("Recepción consolidada exitosamente.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                actualizarGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
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
        boolean isCompra = itemActual.getDetalleCompra() != null;
        return items.stream()
                .filter(i -> isCompra ? i.getDetalleCompra().equals(itemActual.getDetalleCompra()) : i.getDetalleTransferencia().equals(itemActual.getDetalleTransferencia()))
                .map(RecepcionItemUI::getCantidadRecibida)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularRestanteTotal(List<RecepcionItemUI> items, RecepcionItemUI itemActual) {
        BigDecimal sumaTotal = calcularSumaDelMismoProducto(items, itemActual);
        return itemActual.getCantidadMaximaPermitida().subtract(sumaTotal);
    }
}