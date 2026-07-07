package com.agroveterinaria.view.nomina;


import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.EstadoCorrida;
import com.agroveterinaria.enums.PeriodoNomina;
import com.agroveterinaria.enums.TipoConcepto;
import com.agroveterinaria.enums.TipoCorrida;
import com.agroveterinaria.service.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import org.vaadin.crudui.crud.impl.GridCrud;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;


@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class NominaView extends VerticalLayout {
    private final CorridaNominaService corridaNominaService;
    private final DetalleNominaService detalleNominaService;
    private final ConfiguracionNominaService configuracionNominaService;
    private final DiaFeriadoService diaFeriadoService;
    private final EmpleadoService empleadoService;
    private final PeriodoFiscalService periodoFiscalService;

    public NominaView(CorridaNominaService corridaNominaService, DetalleNominaService detalleNominaService,
                      ConfiguracionNominaService configuracionNominaService, DiaFeriadoService diaFeriadoService,
                      EmpleadoService empleadoService, PeriodoFiscalService periodoFiscalService) {
        this.corridaNominaService = corridaNominaService;
        this.detalleNominaService = detalleNominaService;
        this.configuracionNominaService = configuracionNominaService;
        this.diaFeriadoService = diaFeriadoService;
        this.empleadoService = empleadoService;
        this.periodoFiscalService = periodoFiscalService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        Tab tabCorridas = new Tab("Corridas de nómina");

        Tab separador1 = new Tab("|");
        separador1.getStyle()
                .set("color", "#cccccc")
                .set("pointer-events", "none")
                .set("cursor", "default")
                .set("padding", "0 4px")
                .set("min-width", "0");
        separador1.setEnabled(false);

        Tab separador2 = new Tab("|");
        separador2.getStyle()
                .set("color", "#cccccc")
                .set("pointer-events", "none")
                .set("cursor", "default")
                .set("padding", "0 4px")
                .set("min-width", "0");
        separador2.setEnabled(false);

        Tab separador3 = new Tab("|");
        separador3.getStyle()
                .set("color", "#cccccc")
                .set("pointer-events", "none")
                .set("cursor", "default")
                .set("padding", "0 4px")
                .set("min-width", "0");
        separador3.setEnabled(false);

        Tab tabConfiguracion = new Tab("Configuración");
        Tab tabFeriados = new Tab("Días Feriados");
        Tab tabPeriodos = new Tab("Períodos Fiscales");

        Tabs tabs = new Tabs(tabCorridas, separador1,tabConfiguracion, separador2, tabFeriados, separador3, tabPeriodos);
        tabs.setWidthFull();

        VerticalLayout contenidoCorridas = crearContenidoCorridas();
        VerticalLayout contenidoConfiguracion = new ConfiguracionNominaView(configuracionNominaService);
        VerticalLayout vistaFeriados = new DiaFeriadoView(diaFeriadoService);
        VerticalLayout vistaPeriodos = new PeriodoFiscalView(periodoFiscalService);

        vistaFeriados.setVisible(false);
        contenidoConfiguracion.setVisible(false);
        vistaPeriodos.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            Tab selected = tabs.getSelectedTab();
            contenidoCorridas.setVisible(selected.equals(tabCorridas));
            contenidoConfiguracion.setVisible(selected.equals(tabConfiguracion));
            vistaFeriados.setVisible(selected.equals(tabFeriados));
            vistaPeriodos.setVisible(selected.equals(tabPeriodos));
        });

        tabs.getStyle().set("border-bottom", "1px solid #e0e0e0");
        tabs.getStyle().set("width", "fit-content");
        add(tabs, contenidoCorridas, contenidoConfiguracion, vistaFeriados, vistaPeriodos);
    }

    private VerticalLayout crearContenidoCorridas() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.getStyle().set("margin-top", "12px");

        Grid<CorridaNomina> gridCorridas = new Grid<>(CorridaNomina.class, false);
        GridPaginator<CorridaNomina> paginator = new GridPaginator<>(gridCorridas, 10, "corridas");
        gridCorridas.addClassName("usuario-grid");
        gridCorridas.addThemeNames("row-stripes");
        gridCorridas.setWidthFull();
        gridCorridas.setHeight("390px");

        gridCorridas.addColumn(CorridaNomina::getIdCorrida)
                .setHeader("ID").setSortable(true).setWidth("70px").setFlexGrow(0);

        gridCorridas.addColumn(c -> c.getFechaEmision().toString())
                .setHeader("Fecha emisión").setWidth("140px").setFlexGrow(0);

        gridCorridas.addColumn(CorridaNomina::getPeriodo)
                .setHeader("Período").setWidth("110px").setFlexGrow(0);

        gridCorridas.addColumn(CorridaNomina::getCantidadEmpleados)
                .setHeader("Empleados").setWidth("110px").setFlexGrow(0);

        gridCorridas.addColumn(c -> "RD$ " + formatearMonto(c.getTotalGeneral()))
                .setHeader("Total neto").setFlexGrow(1);

        gridCorridas.addColumn(CorridaNomina::getEstado)
                .setHeader("Estado").setWidth("120px").setFlexGrow(0);

        gridCorridas.addComponentColumn(corrida -> {
            Button btnVer = new Button(new Icon(VaadinIcon.EYE));
            btnVer.addClassName("btn-accion-editar");
            btnVer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnVer.addClickListener(e -> dialogResultadoCorrida(corrida, gridCorridas, paginator, false));

            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.setEnabled(corrida.getEstado() == EstadoCorrida.PENDIENTE);
            btnEditar.addClickListener(e -> dialogResultadoCorrida(corrida, gridCorridas, paginator, true));

            Button btnAprobar = new Button(new Icon(VaadinIcon.CHECK));
            btnAprobar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnAprobar.setEnabled(corrida.getEstado() == EstadoCorrida.PENDIENTE);
            btnAprobar.addClickListener(e -> {
                corridaNominaService.aprobarCorrida(corrida);
                refrescarGrid(paginator);
                mostrarExito("Corrida aprobada correctamente.");
            });

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addClassName("btn-accion-eliminar");
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEliminar.setEnabled(corrida.getEstado() == EstadoCorrida.PENDIENTE);
            btnEliminar.addClickListener(e -> confirmarEliminar(corrida, paginator));

            HorizontalLayout acciones = new HorizontalLayout(btnVer, btnEditar,btnAprobar, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("190px").setFlexGrow(0);

        Button btnGenerar = new Button("Nueva corrida", new Icon(VaadinIcon.PLUS));
        btnGenerar.addClassName("btn-nuevo");
        btnGenerar.addClickListener(e -> dialogGeneracion(gridCorridas, paginator));

        ComboBox<TipoCorrida> filtroTipo = new ComboBox<>();
        filtroTipo.setPlaceholder("Filtrar por Tipo...");
        filtroTipo.setTooltipText("Filtrar por Tipo");
        filtroTipo.setItems(TipoCorrida.values());
        filtroTipo.setClearButtonVisible(true);
        filtroTipo.setMinWidth("280px");
        filtroTipo.setItemLabelGenerator(TipoCorrida::getDescripcion);

        filtroTipo.addValueChangeListener(e -> actualizarFiltroGrid(paginator, filtroTipo.getValue(), corridaNominaService));

        HorizontalLayout toolbar = new HorizontalLayout(btnGenerar, filtroTipo);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin-bottom", "12px");

        refrescarGrid(paginator);
        layout.add(toolbar, paginator, gridCorridas);
        return layout;
    }

    private void dialogGeneracion(Grid<CorridaNomina> gridCorridas, GridPaginator<CorridaNomina> paginator) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nueva corrida de nómina");
        dialog.setWidth("450px");

        ComboBox<TipoCorrida> cmbTipo = new ComboBox<>("Tipo de Corrida");
        cmbTipo.setItems(TipoCorrida.values());
        cmbTipo.setValue(TipoCorrida.ORDINARIA);
        cmbTipo.setItemLabelGenerator(TipoCorrida::getDescripcion);
        cmbTipo.setWidthFull();

        ComboBox<PeriodoNomina> cmbPeriodo = new ComboBox<>("Período");
        cmbPeriodo.setItems(PeriodoNomina.values());
        cmbPeriodo.setItemLabelGenerator(PeriodoNomina::getDescripcion);
        cmbPeriodo.setWidthFull();

        DatePicker fechaEmision = new DatePicker("Fecha de emisión");
        fechaEmision.setValue(LocalDate.now());
        fechaEmision.setWidthFull();

        ComboBox<PeriodoFiscal> cmbPeriodoFiscal = new ComboBox<>("Período Fiscal");
        cmbPeriodoFiscal.setItems(periodoFiscalService.obtenerPeriodosDisponiblesParaBonificacion());
        cmbPeriodoFiscal.setItemLabelGenerator(p -> p.getAnio() + "");
        cmbPeriodoFiscal.setWidthFull();
        cmbPeriodoFiscal.setVisible(false);

        ComboBox<Empleado> cmbEmpleado = new ComboBox<>("Empleado (Solo Pago Individual)");
        cmbEmpleado.setItems(empleadoService.findByActivoTrue());
        cmbEmpleado.setItemLabelGenerator(e -> e.getPersona().getNombre());
        cmbEmpleado.setWidthFull();
        cmbEmpleado.setVisible(false);

        cmbTipo.addValueChangeListener(event -> {
            TipoCorrida tipo = event.getValue();
            cmbPeriodoFiscal.setVisible(tipo == TipoCorrida.BONIFICACION);
            cmbEmpleado.setVisible(tipo == TipoCorrida.VACACIONES_ANTICIPADAS);

            if (tipo == TipoCorrida.BONIFICACION || tipo == TipoCorrida.REGALIA_PASCUAL) {
                cmbPeriodo.setValue(PeriodoNomina.MES);
            }

            if (tipo == TipoCorrida.VACACIONES_ANTICIPADAS) {
                cmbPeriodo.setValue(PeriodoNomina.QUINCENA);
            }
        });

        VerticalLayout contenido = new VerticalLayout(cmbTipo, cmbPeriodo, fechaEmision, cmbPeriodoFiscal, cmbEmpleado);
        contenido.setPadding(false);
        contenido.setSpacing(true);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnCancelar.addClassName("btn-borde");

        Button btnGenerar = new Button("Generar", new Icon(VaadinIcon.PLAY));
        btnGenerar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnGenerar.addClickListener(e -> {
            if (cmbTipo.isEmpty() || cmbPeriodo.isEmpty() || fechaEmision.isEmpty()) {
                mostrarError("Debes completar todos los campos principales.");
                return;
            }

            if (cmbTipo.getValue() == TipoCorrida.BONIFICACION && cmbPeriodoFiscal.isEmpty()) {
                mostrarError("Seleccione el Período Fiscal para la Bonificación.");
                return;
            }

            if (cmbTipo.getValue() == TipoCorrida.VACACIONES_ANTICIPADAS && cmbEmpleado.isEmpty()) {
                mostrarError("Seleccione un Empleado para la corrida anticipada.");
                return;
            }

            PeriodoNomina periodo = cmbPeriodo.getValue();
            LocalDate fecha = fechaEmision.getValue();
            TipoCorrida tipo = cmbTipo.getValue();

            try {
                CorridaNomina corrida = corridaNominaService.generarCorrida(
                        periodo, fecha, tipo, cmbPeriodoFiscal.getValue(), cmbEmpleado.getValue()
                );

                dialog.close();
                refrescarGrid(paginator);
                dialogResultadoCorrida(corrida, gridCorridas, paginator, true);

            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });

        dialog.add(contenido);
        dialog.getFooter().add(btnGenerar, btnCancelar);
        dialog.open();
    }

    private void dialogFormularioNovedades(Nomina nomina, Grid<Nomina> gridNominas) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Novedades — " + nomina.getEmpleado().getPersona().getNombre());
        dialog.setWidth("80vw");
        dialog.setMaxWidth("900px");

        NumberField horasExtras = new NumberField("Horas extras");
        horasExtras.setMin(0);
        horasExtras.setPlaceholder("0");
        horasExtras.setWidthFull();
        horasExtras.setValue(obtenerCantidadConcepto(nomina, TipoConcepto.HORAS_EXTRAS));

        BigDecimalField comisionesRegulares = crearCampoMoneda("Comisiones Regulares");
        comisionesRegulares.setValue(obtenerMontoConcepto(nomina, TipoConcepto.COMISIONES_REGULARES));

        BigDecimalField comisionesExtraordinarias = crearCampoMoneda("Comisiones Extraordinarias / Bonos");
        comisionesExtraordinarias.setValue(obtenerMontoConcepto(nomina, TipoConcepto.COMISIONES_EXTRAORDINARIAS));

        BigDecimalField dietasYViaticos = crearCampoMoneda("Dietas y viáticos");
        dietasYViaticos.setValue(obtenerMontoConcepto(nomina, TipoConcepto.DIETAS_Y_VIATICOS));

        NumberField ausencias = new NumberField("Ausencias no pagadas (días)");
        ausencias.setMin(0);
        ausencias.setStep(0.5);
        ausencias.setPlaceholder("0");
        ausencias.setWidthFull();
        ausencias.setValue(obtenerCantidadConcepto(nomina, TipoConcepto.AUSENCIAS_NO_PAGADAS));

        BigDecimalField anticipo = crearCampoMoneda("Anticipo de salario");
        anticipo.setValue(obtenerMontoConcepto(nomina, TipoConcepto.ANTICIPO_SALARIO));

        BigDecimalField otrosDescuentos = crearCampoMoneda("Otros descuentos");
        otrosDescuentos.setValue(obtenerMontoConcepto(nomina, TipoConcepto.OTRAS_DEDUCCIONES));

        FormLayout formIngresos = new FormLayout(horasExtras, comisionesRegulares, comisionesExtraordinarias, dietasYViaticos);
        formIngresos.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("300px", 2));

        VerticalLayout seccionIngresos = new VerticalLayout(new Span("Ingresos Adicionales"), formIngresos);
        seccionIngresos.addClassNames("caja-novedades", "seccion-ingresos");
        seccionIngresos.setPadding(false);

        FormLayout formDeducciones = new FormLayout(ausencias, anticipo, otrosDescuentos);
        formDeducciones.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("300px", 2));

        VerticalLayout seccionDeducciones = new VerticalLayout(new Span("Deducciones"), formDeducciones);
        seccionDeducciones.addClassNames("caja-novedades", "seccion-deducciones");
        seccionDeducciones.getStyle().set("margin-top", "24px");
        seccionDeducciones.setPadding(false);

        VerticalLayout layoutPrincipal = new VerticalLayout(seccionIngresos, seccionDeducciones);
        layoutPrincipal.setPadding(false);
        layoutPrincipal.setSpacing(false);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        Button btnGuardar = new Button("Guardar", new Icon(VaadinIcon.CHECK));
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnGuardar.addClickListener(e -> {
            if (horasExtras.getValue() != null && horasExtras.getValue() > 0) {
                BigDecimal montoHoras = configuracionNominaService.calcularHorasExtras(horasExtras.getValue());
                agregarOActualizarNovedad(nomina, TipoConcepto.HORAS_EXTRAS, "Horas extras", montoHoras, horasExtras.getValue());
            } else {
                nomina.getDetalles().removeIf(d -> d.getTipo() == TipoConcepto.HORAS_EXTRAS);
            }

            agregarOActualizarNovedad(nomina, TipoConcepto.COMISIONES_REGULARES, "Comisiones Regulares", comisionesRegulares.getValue(), 1.0);
            agregarOActualizarNovedad(nomina, TipoConcepto.COMISIONES_EXTRAORDINARIAS, "Comisiones Extraordinarias", comisionesExtraordinarias.getValue(), 1.0);
            agregarOActualizarNovedad(nomina, TipoConcepto.DIETAS_Y_VIATICOS, "Dietas y viáticos", dietasYViaticos.getValue(), 1.0);
            agregarOActualizarNovedad(nomina, TipoConcepto.ANTICIPO_SALARIO, "Anticipo de salario", anticipo.getValue(), 1.0);
            agregarOActualizarNovedad(nomina, TipoConcepto.OTRAS_DEDUCCIONES, "Otros descuentos", otrosDescuentos.getValue(), 1.0);

            if (ausencias.getValue() != null && ausencias.getValue() > 0) {
                BigDecimal valorDia = nomina.getEmpleado().getSalario().divide(BigDecimal.valueOf(30), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal montoAusencia = valorDia.multiply(BigDecimal.valueOf(ausencias.getValue()));
                agregarOActualizarNovedad(nomina, TipoConcepto.AUSENCIAS_NO_PAGADAS, "Ausencias no pagadas", montoAusencia, ausencias.getValue());
            } else {
                nomina.getDetalles().removeIf(d -> d.getTipo() == TipoConcepto.AUSENCIAS_NO_PAGADAS);
            }

            nomina.calcularSueldoNeto();
            gridNominas.getDataProvider().refreshItem(nomina);

            dialog.close();
            mostrarExito("Novedades actualizadas correctamente.");
        });

        dialog.add(layoutPrincipal);
        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private void dialogResultadoCorrida(
            CorridaNomina corrida,
            Grid<CorridaNomina> gridCorridas,
            GridPaginator<CorridaNomina> paginator,
            boolean esModoEdicion
    ) {
        if(esModoEdicion){
            for (Nomina nomina : corrida.getNominas()) {
                BigDecimal devengado = nomina.getDetalles().stream()
                        .filter(d -> d.getTipo().esIngreso())
                        .map(DetalleNomina::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                nomina.getDetalles().removeIf(d ->
                        d.getTipo() == TipoConcepto.FONDO_PENSIONES ||
                                d.getTipo() == TipoConcepto.SEGURO_FAMILIAR_SALUD ||
                                d.getTipo() == TipoConcepto.IMPUESTO_RENTA);

                nomina.getDetalles().add(crearDetalle(nomina, TipoConcepto.FONDO_PENSIONES,
                        "AFP", configuracionNominaService.calcularAFP(devengado), 1.0));
                nomina.getDetalles().add(crearDetalle(nomina, TipoConcepto.SEGURO_FAMILIAR_SALUD,
                        "SFS", configuracionNominaService.calcularSFS(devengado), 1.0));

                BigDecimal isr = configuracionNominaService.calcularISR(devengado, corrida.getPeriodo());
                if (isr.compareTo(BigDecimal.ZERO) > 0) {
                    nomina.getDetalles().add(crearDetalle(nomina, TipoConcepto.IMPUESTO_RENTA,
                            "ISR", isr, 1.0));
                }

                nomina.calcularSueldoNeto();
            }
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Resultado — " + corrida.getPeriodo() + " " + corrida.getFechaEmision());
        dialog.setWidth("85vw");
        dialog.setMaxWidth("850px");

        Grid<Nomina> gridNominas = new Grid<>(Nomina.class, false);
        gridNominas.addColumn(n -> n.getEmpleado().getPersona().getNombre())
                .setHeader("Empleado").setFlexGrow(1);
        gridNominas.addColumn(n -> "RD$ " + formatearMonto(n.getTotalDevengado()))
                .setHeader("Devengado").setWidth("150px").setFlexGrow(0);
        gridNominas.addColumn(n -> "RD$ " + formatearMonto(n.getTotalDeducciones()))
                .setHeader("Deducciones").setWidth("150px").setFlexGrow(0);
        gridNominas.addColumn(n -> "RD$ " + formatearMonto(n.getTotalDevengado()
                        .subtract(n.getTotalDeducciones())))
                .setHeader("Neto").setWidth("150px").setFlexGrow(0);

        gridNominas.addComponentColumn(nomina -> {
            Button btnVer = new Button(new Icon(VaadinIcon.EYE));
            btnVer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnVer.addClickListener(e -> dialogDetalleNomina(nomina));

            HorizontalLayout acciones = new HorizontalLayout(btnVer);

            if (esModoEdicion) {
                Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
                btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                btnEditar.addClickListener(e -> dialogFormularioNovedades(nomina, gridNominas));
                acciones.add(btnEditar);
            }

            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setWidth(esModoEdicion ? "110px" : "90px").setFlexGrow(0).setHeader("Acciones");

        gridNominas.setItems(corrida.getNominas());
        gridNominas.setHeight("300px");

        Span totalGeneral = new Span("Total general: RD$ " + formatearMonto(corrida.getTotalGeneral()));
        totalGeneral.getStyle().set("font-weight", "bold").set("color", "#1a56db")
                .set("align-self", "flex-end")
                .set("margin-top", "10px");;

        VerticalLayout contenido = new VerticalLayout(gridNominas, totalGeneral);
        contenido.setPadding(false);
        dialog.add(contenido);

        if (esModoEdicion) {
            Button btnDejarPendiente = new Button("Dejar pendiente", e -> {
                dialog.close();
                refrescarGrid(paginator);
                mostrarExito("La corrida se ha mantenido como pendiente.");
            });
            btnDejarPendiente.addClassName("btn-borde");

            Button btnAprobar = new Button("Aprobar corrida", new Icon(VaadinIcon.CHECK));
            btnAprobar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            btnAprobar.addClickListener(e -> {
                corridaNominaService.aprobarCorrida(corrida);
                dialog.close();
                refrescarGrid(paginator);
                mostrarExito("Corrida aprobada correctamente.");
            });
            dialog.getFooter().add(btnAprobar, btnDejarPendiente);
        } else {
            Button btnCerrar = new Button("Cerrar", e -> dialog.close());
            btnCerrar.addClassName("btn-borde");
            dialog.getFooter().add(btnCerrar);
        }

        dialog.open();
    }

    private void dialogDetalleNomina(Nomina nomina) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Detalle — " + nomina.getEmpleado().getPersona().getNombre());
        dialog.setWidth("75vw");
        dialog.setMaxWidth("600px");

        Grid<DetalleNomina> gridDetalle = new Grid<>(DetalleNomina.class, false);
        gridDetalle.addColumn(DetalleNomina::getDescripcion).setHeader("Concepto").setFlexGrow(1);
        gridDetalle.addColumn(d -> d.getTipo().esIngreso() ? "Ingreso" : "Deducción")
                .setHeader("Tipo").setWidth("120px").setFlexGrow(0);
        gridDetalle.addColumn(d -> "RD$ " + formatearMonto(d.getMonto()))
                .setHeader("Monto").setWidth("150px").setFlexGrow(0);
        gridDetalle.setItems(nomina.getDetalles());
        gridDetalle.setHeight("280px");

        Span sueldoNeto = new Span("Sueldo neto: RD$ " +
                formatearMonto(nomina.getTotalDevengado().subtract(nomina.getTotalDeducciones())));
        sueldoNeto.getStyle().set("font-weight", "bold").set("color", "#1a56db")
                .set("align-self", "flex-end")
                .set("margin-top", "10px");;

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        btnCerrar.addClassName("btn-borde");

        dialog.add(new VerticalLayout(gridDetalle, sueldoNeto));
        dialog.getFooter().add(btnCerrar);
        dialog.open();
    }

    private void confirmarEliminar(CorridaNomina corrida, GridPaginator<CorridaNomina> paginator) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("¿Eliminar corrida?");

        Span mensaje = new Span("Se eliminarán todas las nóminas de esta corrida. Esta acción no se puede deshacer.");

        Button btnSi = new Button("Sí, eliminar", e -> {
            corridaNominaService.delete(corrida);
            confirm.close();
            refrescarGrid(paginator);
            mostrarExito("Corrida eliminada.");
        });
        btnSi.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button btnNo = new Button("Cancelar", e -> confirm.close());
        btnNo.addClassName("btn-borde");

        confirm.add(new VerticalLayout(mensaje));
        confirm.getFooter().add(btnSi,btnNo);
        confirm.open();
    }

    private DetalleNomina crearDetalle(Nomina nomina, TipoConcepto tipo, String descripcion, BigDecimal monto, Double cantidad) {
        DetalleNomina detalle = new DetalleNomina();
        detalle.setNomina(nomina);
        detalle.setTipo(tipo);
        detalle.setDescripcion(descripcion);
        detalle.setMonto(monto);
        detalle.setCantidad(BigDecimal.valueOf(cantidad));
        return detalle;
    }

    private void agregarOActualizarNovedad(Nomina nomina, TipoConcepto tipo, String descripcion, BigDecimal monto, Double cantidad) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            nomina.getDetalles().removeIf(d -> d.getTipo() == tipo);
            nomina.getDetalles().add(crearDetalle(nomina, tipo, descripcion, monto, cantidad));
        }
    }

    private BigDecimalField crearCampoMoneda(String label) {
        BigDecimalField campo = new BigDecimalField(label);
        campo.setPrefixComponent(new Span("RD$"));
        campo.setWidthFull();
        campo.setClearButtonVisible(true);
        campo.setPlaceholder("0.00");
        return campo;
    }

    private String formatearMonto(BigDecimal monto) {
        if (monto == null) return "0.00";
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "DO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(monto);
    }

    private Double obtenerCantidadConcepto(Nomina nomina, TipoConcepto tipo) {
        return nomina.getDetalles().stream()
                .filter(d -> d.getTipo() == tipo)
                .map(d -> d.getCantidad().doubleValue())
                .findFirst().orElse(null);
    }

    private BigDecimal obtenerMontoConcepto(Nomina nomina, TipoConcepto tipo) {
        return nomina.getDetalles().stream()
                .filter(d -> d.getTipo() == tipo)
                .map(DetalleNomina::getMonto)
                .findFirst().orElse(null);
    }

    private void refrescarGrid(GridPaginator<CorridaNomina> paginator) {
        paginator.setItems(corridaNominaService.findAllConNominas());
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void actualizarFiltroGrid(
            GridPaginator<CorridaNomina> paginator,
            TipoCorrida tipoFiltro,
            CorridaNominaService corridaNominaServiceservice
    ) {
        List<CorridaNomina> listaFiltrada = corridaNominaService.findAllConNominas().stream()
                .filter(c -> tipoFiltro == null || c.getTipo() == tipoFiltro)
                .toList();

        paginator.setItems(listaFiltrada);
    }
}
