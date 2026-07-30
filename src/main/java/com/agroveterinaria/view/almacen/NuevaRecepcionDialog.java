package com.agroveterinaria.view.almacen;

import com.agroveterinaria.dto.recepcion.GastoOperativoUI;
import com.agroveterinaria.dto.recepcion.RecepcionItemUI;
import com.agroveterinaria.dto.recepcion.RecepcionResumenDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.*;
import com.agroveterinaria.util.FormatoInventarioUtil;
import com.agroveterinaria.component.CantidadFraccionadaField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NuevaRecepcionDialog extends Dialog {

    private final AlmacenService almacenService;
    private final LoteService loteService;
    private final VehiculoService vehiculoService;
    private final EmpleadoService empleadoService;
    private final RutaService rutaService;
    private final RecepcionService recepcionService;
    private final Runnable alGuardarExitosamente;

    private ComboBox<RecepcionResumenDTO> cbDocumentoPendiente;
    private Grid<RecepcionItemUI> gridRecepcion;
    private List<RecepcionItemUI> itemsFisicos = new ArrayList<>();

    private VerticalLayout seccionLogistica;
    private com.vaadin.flow.component.radiobutton.RadioButtonGroup<String> rbgTipoLogistica;
    private com.vaadin.flow.component.textfield.BigDecimalField txtCostoFlete;
    private HorizontalLayout formCamionYChofer;
    private ComboBox<Vehiculo> cbVehiculo;
    private ComboBox<Empleado> cbConductor;
    private ComboBox<Ruta> cbRuta;
    private VerticalLayout layoutTablaGastos;
    private List<GastoOperativoUI> listaGastos = new ArrayList<>();
    private Grid<GastoOperativoUI> gridGastos;

    public NuevaRecepcionDialog(AlmacenService almacenService, LoteService loteService,
                                VehiculoService vehiculoService, EmpleadoService empleadoService,
                                RutaService rutaService, RecepcionService recepcionService,
                                Runnable alGuardarExitosamente) {
        this.almacenService = almacenService;
        this.loteService = loteService;
        this.vehiculoService = vehiculoService;
        this.empleadoService = empleadoService;
        this.rutaService = rutaService;
        this.recepcionService = recepcionService;
        this.alGuardarExitosamente = alGuardarExitosamente;

        setWidth("95vw");
        setMaxWidth("2000px");
        setCloseOnOutsideClick(false);

        H3 titulo = new H3("Procesar Nueva Recepción (Entrada)");
        titulo.getStyle().set("margin-top", "0");

        construirComboBuscador();
        construirGridRecepcion();
        construirSeccionLogistica();

        Button btnProcesar = new Button("Confirmar Recepción", new Icon(VaadinIcon.CHECK_CIRCLE));
        btnProcesar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnProcesar.addClickListener(e -> procesarRecepcion());

        Button btnCancelar = new Button("Cancelar", e -> close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnCancelar, btnProcesar);
        botones.setWidthFull();
        botones.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "20px");

        VerticalLayout contenido = new VerticalLayout(titulo, cbDocumentoPendiente, gridRecepcion, seccionLogistica, botones);
        contenido.setPadding(false);
        add(contenido);
    }

    private void construirComboBuscador() {
        cbDocumentoPendiente = new ComboBox<>("Documento Pendiente (Compra o Transferencia)");
        cbDocumentoPendiente.setWidthFull();
        cbDocumentoPendiente.setItems(recepcionService.obtenerColaRecepciones());
        cbDocumentoPendiente.setItemLabelGenerator(dto -> dto.getCodigo() + " - Origen: " + dto.getOrigen());

        cbDocumentoPendiente.addValueChangeListener(e -> {
            RecepcionResumenDTO dto = e.getValue();
            if (dto != null) {
                Long idDoc = "Compra".equals(dto.getTipo()) ? dto.getCompraOriginal().getIdCompra() : dto.getTransferenciaOriginal().getIdTransferencia();
                itemsFisicos = recepcionService.obtenerItemsPendientes(dto.getTipo(), idDoc);
                gridRecepcion.setItems(itemsFisicos);

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
                    cbVehiculo.clear(); cbConductor.clear(); cbRuta.clear();
                }
            } else {
                itemsFisicos.clear();
                gridRecepcion.setItems(itemsFisicos);
                seccionLogistica.setVisible(false);
            }
        });
    }

    private void construirGridRecepcion() {
        gridRecepcion = new Grid<>(RecepcionItemUI.class, false);
        gridRecepcion.addThemeNames("row-stripes");
        gridRecepcion.setHeight("400px");

        gridRecepcion.addColumn(item -> item.getProducto().getNombre())
                .setHeader("Producto")
                .setWidth("180px")
                .setFlexGrow(2);

        gridRecepcion.addComponentColumn(item -> {
            Producto prod = item.getProducto();
            CantidadFraccionadaField txtCant = new CantidadFraccionadaField();
            txtCant.configurarProducto(prod.getContenidoPorEmpaque(), Boolean.TRUE.equals(prod.getPermiteFraccionamiento()), false, FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(prod), FormatoInventarioUtil.getNombreUnidadFraccionSafe(prod));
            txtCant.setValue(item.getCantidadRecibida());

            txtCant.addValueChangeListener(e -> {
                BigDecimal nuevaCant = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;
                item.setCantidadRecibida(BigDecimal.ZERO);

                BigDecimal sumaOtros = calcularSumaDelMismoProducto(itemsFisicos, item);
                BigDecimal maxPermitido = item.getCantidadMaximaPermitida().subtract(sumaOtros);

                if (nuevaCant.compareTo(maxPermitido) > 0) {
                    String maxFormateado = FormatoInventarioUtil.formatearCantidad(maxPermitido, prod.getContenidoPorEmpaque(), Boolean.TRUE.equals(prod.getPermiteFraccionamiento()), false, FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(prod), FormatoInventarioUtil.getNombreUnidadFraccionSafe(prod));
                    Notification.show("Máximo permitido: " + maxFormateado, 3500, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
                    txtCant.setValue(maxPermitido);
                    item.setCantidadRecibida(maxPermitido);
                } else {
                    item.setCantidadRecibida(nuevaCant);
                }
                gridRecepcion.getDataProvider().refreshAll();
            });
            return txtCant;
        }).setHeader("Cant. a Recibir").setWidth("260px").setFlexGrow(0);

        gridRecepcion.addComponentColumn(item -> {
            Producto prod = item.getProducto();
            CantidadFraccionadaField txtMerma = new CantidadFraccionadaField();
            txtMerma.configurarProducto(prod.getContenidoPorEmpaque(), Boolean.TRUE.equals(prod.getPermiteFraccionamiento()), false, FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(prod), FormatoInventarioUtil.getNombreUnidadFraccionSafe(prod));
            txtMerma.setValue(item.getCantidadMerma());

            txtMerma.addValueChangeListener(e -> {
                BigDecimal nuevaMerma = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;
                item.setCantidadMerma(BigDecimal.ZERO);

                BigDecimal sumaOtros = calcularSumaDelMismoProducto(itemsFisicos, item);
                BigDecimal maxPermitido = item.getCantidadMaximaPermitida().subtract(sumaOtros);

                if (nuevaMerma.compareTo(maxPermitido) > 0) {
                    txtMerma.setValue(maxPermitido);
                    item.setCantidadMerma(maxPermitido);
                } else {
                    item.setCantidadMerma(nuevaMerma);
                }
                gridRecepcion.getDataProvider().refreshAll();
            });
            return txtMerma;
        }).setHeader("Merma/Rotos").setWidth("260px").setFlexGrow(0);

        gridRecepcion.addComponentColumn(item -> {
            TextField txtJustificacion = new TextField();
            txtJustificacion.setWidthFull();
            txtJustificacion.setValue(item.getJustificacionMerma() != null ? item.getJustificacionMerma() : "");
            boolean tieneMerma = item.getCantidadMerma() != null && item.getCantidadMerma().compareTo(BigDecimal.ZERO) > 0;
            txtJustificacion.setEnabled(tieneMerma);
            txtJustificacion.addValueChangeListener(e -> item.setJustificacionMerma(e.getValue()));
            return txtJustificacion;
        }).setHeader("Justificación").setWidth("160px").setFlexGrow(1);

        gridRecepcion.addColumn(item -> {
            BigDecimal disponible = calcularRestanteTotal(itemsFisicos, item);
            BigDecimal valFinal = disponible.compareTo(BigDecimal.ZERO) > 0 ? disponible : BigDecimal.ZERO;
            Producto prod = item.getProducto();
            return FormatoInventarioUtil.formatearCantidad(
                    valFinal,
                    prod.getContenidoPorEmpaque(),
                    Boolean.TRUE.equals(prod.getPermiteFraccionamiento()),
                    false,
                    FormatoInventarioUtil.getNombreUnidadEmpaqueSafe(prod),
                    FormatoInventarioUtil.getNombreUnidadFraccionSafe(prod)
            );
        }).setHeader("Pendiente Global").setWidth("130px").setFlexGrow(0);

        gridRecepcion.addComponentColumn(item -> {
            ComboBox<Almacen> cbAlmacen = new ComboBox<>();
            cbAlmacen.setItems(almacenService.listarTodos().stream().filter(a -> a.getStatus() == StatusEntidad.ACTIVO).toList());
            cbAlmacen.setItemLabelGenerator(Almacen::getNombre);
            cbAlmacen.setWidthFull();
            cbAlmacen.setValue(item.getAlmacenDestino());
            if (item.getDetalleTransferencia() != null) cbAlmacen.setReadOnly(true);
            else cbAlmacen.addValueChangeListener(e -> item.setAlmacenDestino(e.getValue()));
            return cbAlmacen;
        }).setHeader("Almacén Destino").setWidth("220px").setFlexGrow(2);

        gridRecepcion.addComponentColumn(item -> {
            ComboBox<Lote> cbLote = new ComboBox<>();
            cbLote.setPlaceholder("Escriba...");
            cbLote.setWidthFull();
            List<Lote> lotesExistentes = loteService.buscarPorProducto(item.getProducto());
            cbLote.setItems(lotesExistentes);
            cbLote.setItemLabelGenerator(l -> l.getNumeroLote() != null ? l.getNumeroLote() : "S/N");

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
        }).setHeader("No. Lote").setWidth("140px").setFlexGrow(1);

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
        }).setHeader("Vencimiento").setFlexGrow(0).setWidth("150px");

        gridRecepcion.addComponentColumn(item -> {
            if (item.getDetalleTransferencia() != null) return new Span("-");
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
        }).setHeader("Dividir").setWidth("100px").setFlexGrow(0);
    }

    private void construirSeccionLogistica() {
        seccionLogistica = new VerticalLayout();
        seccionLogistica.setPadding(false);
        seccionLogistica.getStyle().set("border", "1px solid #e0e0e0").set("border-radius", "8px").set("padding", "15px").set("margin-top", "15px");
        seccionLogistica.setVisible(false);

        H4 tituloLogistica = new H4("Logística de Abastecimiento");
        tituloLogistica.getStyle().set("margin", "0");

        rbgTipoLogistica = new com.vaadin.flow.component.radiobutton.RadioButtonGroup<>();
        rbgTipoLogistica.setLabel("Modalidad de traslado:");
        rbgTipoLogistica.setItems("Entrega Regular (Sin costos)", "Transporte Interno (Vehículo propio)", "Flete / Delivery Externo", "Mixto (Flete parcial + Transporte propio)");
        rbgTipoLogistica.setValue("Entrega Regular (Sin costos)");

        txtCostoFlete = new com.vaadin.flow.component.textfield.BigDecimalField("Costo de Flete Externo");
        txtCostoFlete.setWidthFull();
        txtCostoFlete.setVisible(false);

        formCamionYChofer = new HorizontalLayout();
        formCamionYChofer.setWidthFull();
        formCamionYChofer.setVisible(false);

        cbVehiculo = new ComboBox<>("Vehículo");
        cbVehiculo.setItems(vehiculoService.listarTodos());
        cbVehiculo.setItemLabelGenerator(v -> v.getPlaca() + " - " + v.getModelo());
        cbVehiculo.setWidthFull();

        cbConductor = new ComboBox<>("Conductor");
        cbConductor.setItems(empleadoService.findByCargo(RolEmpleado.CONDUCTOR));
        cbConductor.setItemLabelGenerator(e -> e.getPersona().getNombre());
        cbConductor.setWidthFull();

        cbRuta = new ComboBox<>("Ruta");
        cbRuta.setItems(rutaService.listarTodos());
        cbRuta.setItemLabelGenerator(r -> r.getNombre() + " km");
        cbRuta.setWidthFull();

        formCamionYChofer.add(cbVehiculo, cbConductor, cbRuta);

        layoutTablaGastos = new VerticalLayout();
        layoutTablaGastos.setPadding(false);
        layoutTablaGastos.setVisible(false);

        gridGastos = new Grid<>(GastoOperativoUI.class, false);
        gridGastos.setItems(listaGastos);
        gridGastos.addThemeNames("row-stripes");
        gridGastos.setHeight("160px");

        gridGastos.addComponentColumn(gasto -> {
            TextField txtNota = new TextField();
            txtNota.setWidthFull();
            txtNota.setValue(gasto.getNotas() != null ? gasto.getNotas() : "");
            txtNota.addValueChangeListener(ev -> gasto.setNotas(ev.getValue()));
            return txtNota;
        }).setHeader("Concepto").setFlexGrow(2);

        gridGastos.addComponentColumn(gasto -> {
            com.vaadin.flow.component.textfield.BigDecimalField txtMonto = new com.vaadin.flow.component.textfield.BigDecimalField();
            txtMonto.setWidthFull();
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

        Button btnAgregarGasto = new Button("Añadir Gasto", e -> {
            listaGastos.add(new GastoOperativoUI());
            gridGastos.getDataProvider().refreshAll();
        });

        layoutTablaGastos.add(gridGastos, btnAgregarGasto);

        rbgTipoLogistica.addValueChangeListener(e -> {
            String sel = e.getValue();
            formCamionYChofer.setVisible(sel.contains("Interno") || sel.contains("Mixto"));
            txtCostoFlete.setVisible(sel.contains("Externo") || sel.contains("Mixto"));
            layoutTablaGastos.setVisible(sel.contains("Interno") || sel.contains("Mixto"));
        });

        seccionLogistica.add(tituloLogistica, rbgTipoLogistica, txtCostoFlete, formCamionYChofer, layoutTablaGastos);
    }

    private void procesarRecepcion() {
        if (cbDocumentoPendiente.isEmpty()) {
            Notification.show("Debes seleccionar un documento pendiente.").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        RecepcionResumenDTO dto = cbDocumentoPendiente.getValue();
        Long idDoc = "Compra".equals(dto.getTipo()) ? dto.getCompraOriginal().getIdCompra() : dto.getTransferenciaOriginal().getIdTransferencia();

        try {
            recepcionService.procesarRecepcionTransaccional(
                    dto.getTipo(), idDoc, itemsFisicos, rbgTipoLogistica.getValue(),
                    txtCostoFlete.getValue(), cbVehiculo.getValue(), cbConductor.getValue(),
                    cbRuta.getValue(), listaGastos
            );
            Notification.show("Recepción consolidada exitosamente.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            alGuardarExitosamente.run();
            close();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private BigDecimal calcularSumaDelMismoProducto(List<RecepcionItemUI> items, RecepcionItemUI itemActual) {
        boolean isCompra = itemActual.getDetalleCompra() != null;
        return items.stream()
                .filter(i -> isCompra ? i.getDetalleCompra().equals(itemActual.getDetalleCompra()) : i.getDetalleTransferencia().equals(itemActual.getDetalleTransferencia()))
                .map(i -> {
                    BigDecimal rec = i.getCantidadRecibida() != null ? i.getCantidadRecibida() : BigDecimal.ZERO;
                    BigDecimal mer = i.getCantidadMerma() != null ? i.getCantidadMerma() : BigDecimal.ZERO;
                    return rec.add(mer);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularRestanteTotal(List<RecepcionItemUI> items, RecepcionItemUI itemActual) {
        BigDecimal sumaTotal = calcularSumaDelMismoProducto(items, itemActual);
        return itemActual.getCantidadMaximaPermitida().subtract(sumaTotal);
    }
}