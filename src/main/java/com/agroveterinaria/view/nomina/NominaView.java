package com.agroveterinaria.view.nomina;


import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.dto.nomina.NovedadNominaDTO;
import com.agroveterinaria.entity.*;
import com.agroveterinaria.enums.*;
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
import java.util.ArrayList;
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

        Tab separador1 = crearSeparador();
        Tab separador2 = crearSeparador();
        Tab separador3 = crearSeparador();

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

    private Tab crearSeparador() {
        Tab separador = new Tab("|");
        separador.getStyle()
                .set("color", "#cccccc")
                .set("pointer-events", "none")
                .set("cursor", "default")
                .set("padding", "0 4px")
                .set("min-width", "0");
        separador.setEnabled(false);
        return separador;
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

        gridCorridas.addColumn(c -> c.getFechaEmision().toString())
                .setHeader("Fecha emisión").setWidth("140px").setFlexGrow(0).setSortable(true);

        gridCorridas.addColumn(c -> c.getPeriodo().getDescripcion())
                .setHeader("Período").setWidth("110px").setFlexGrow(0);

        gridCorridas.addColumn(c -> c.getTipo().getDescripcion())
                .setHeader("Tipo").setWidth("150px").setFlexGrow(0);

        gridCorridas.addColumn(CorridaNomina::getCantidadEmpleados)
                .setHeader("Empleados").setWidth("110px").setFlexGrow(0);

        gridCorridas.addColumn(c -> "RD$ " + formatearMonto(c.getTotalGeneral()))
                .setHeader("Total neto").setFlexGrow(1);

        gridCorridas.addComponentColumn(corrida -> {
            Span circulo = new Span();
            circulo.getStyle().set("width", "10px");
            circulo.getStyle().set("height", "10px");
            circulo.getStyle().set("border-radius", "50%");
            circulo.getStyle().set("display", "inline-block");

            boolean esAprobada = corrida.getEstado() == EstadoCorrida.APROBADA;

            circulo.getStyle().set("background-color", esAprobada ? "#2e7d32" : "#f59e0b");

            Span texto = new Span(corrida.getEstado().getDescripcion());
            texto.getStyle().set("color", esAprobada ? "#2e7d32" : "#f59e0b");
            texto.getStyle().set("font-weight", "500");

            HorizontalLayout layoutEstado = new HorizontalLayout(circulo, texto);
            layoutEstado.setAlignItems(Alignment.CENTER);
            layoutEstado.setSpacing(true);

            return layoutEstado;
        }).setHeader("Estado").setWidth("120px").setFlexGrow(0);

        gridCorridas.addComponentColumn(corrida -> {
            Button btnVer = new Button(new Icon(VaadinIcon.EYE));
            btnVer.addClassName("btn-accion-editar");
            btnVer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnVer.addClickListener(e -> dialogResultadoCorrida(corrida, paginator, false));

            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.setEnabled(corrida.getEstado() == EstadoCorrida.PENDIENTE);
            btnEditar.setVisible(corrida.getTipo() == TipoCorrida.ORDINARIA);
            btnEditar.addClickListener(e -> dialogResultadoCorrida(corrida, paginator, true));

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
        btnGenerar.addClickListener(e -> dialogGeneracion(paginator));

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

    private void dialogGeneracion(GridPaginator<CorridaNomina> paginator) {
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

        LocalDate hoy = LocalDate.now();
        LocalDate minFecha = hoy.minusMonths(3).withDayOfMonth(1);
        LocalDate maxFecha = hoy.withDayOfMonth(hoy.lengthOfMonth());

        fechaEmision.setMin(minFecha);
        fechaEmision.setMax(maxFecha);

        ComboBox<PeriodoFiscal> cmbPeriodoFiscal = new ComboBox<>("Período Fiscal");
        cmbPeriodoFiscal.setItems(periodoFiscalService.obtenerPeriodosDisponiblesParaBonificacion());
        cmbPeriodoFiscal.setItemLabelGenerator(p -> p.getAnio() + "");
        cmbPeriodoFiscal.setWidthFull();
        cmbPeriodoFiscal.setVisible(false);

        ComboBox<Empleado> cmbEmpleado = new ComboBox<>("Empleado (Solo Pago Individual)");
        cmbEmpleado.setItems(empleadoService.findByStatus(StatusEntidad.ACTIVO));
        cmbEmpleado.setItemLabelGenerator(e -> e.getPersona().getNombre());
        cmbEmpleado.setWidthFull();
        cmbEmpleado.setVisible(false);

        cmbTipo.addValueChangeListener(event -> {
            TipoCorrida tipo = event.getValue();
            cmbPeriodoFiscal.setVisible(tipo == TipoCorrida.BONIFICACION);
            cmbEmpleado.setVisible(tipo == TipoCorrida.VACACIONES_ANTICIPADAS);

            cmbPeriodo.setVisible(tipo == TipoCorrida.ORDINARIA);

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
                corridaNominaService.validarDisponibilidadDePeriodo(periodo, fecha, tipo);
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
                return;
            }

            List<NovedadNominaDTO> novedadesMemoria = new ArrayList<>();
            if (tipo == TipoCorrida.VACACIONES_ANTICIPADAS) {
                novedadesMemoria.add(new NovedadNominaDTO(cmbEmpleado.getValue()));
            } else {
                empleadoService.findByStatus(StatusEntidad.ACTIVO).forEach(emp -> novedadesMemoria.add(new NovedadNominaDTO(emp)));
            }

            dialog.close();
            if (tipo == TipoCorrida.ORDINARIA) {
                dialogPreparacionNovedades(periodo, fecha, tipo, cmbPeriodoFiscal.getValue(), cmbEmpleado.getValue(), novedadesMemoria, paginator);
            } else {
                try {
                    CorridaNomina corrida = corridaNominaService.generarCorrida(
                            periodo, fecha, tipo, cmbPeriodoFiscal.getValue(), cmbEmpleado.getValue(), novedadesMemoria
                    );
                    refrescarGrid(paginator);
                    dialogResultadoCorrida(corrida, paginator, true);
                } catch (Exception ex) {
                    mostrarError(ex.getMessage());
                }
            }
        });

        dialog.add(contenido);
        dialog.getFooter().add(btnGenerar, btnCancelar);
        dialog.open();
    }

    private void dialogPreparacionNovedades(
            PeriodoNomina periodo, LocalDate fecha, TipoCorrida tipo,
            PeriodoFiscal periodoFiscal, Empleado empleadoUnico,
            List<NovedadNominaDTO> novedades, GridPaginator<CorridaNomina> paginator) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Preparación de Novedades");
        dialog.setWidth("80vw");
        dialog.setMaxWidth("900px");

        Grid<NovedadNominaDTO> gridNovedades = new Grid<>(NovedadNominaDTO.class, false);
        gridNovedades.addColumn(dto -> dto.getEmpleado().getPersona().getNombre() + " " + dto.getEmpleado().getPersona().getApellido())
                .setHeader("Empleado").setFlexGrow(1);
        gridNovedades.addColumn(NovedadNominaDTO::getHorasExtras)
                .setHeader("Horas Ext.").setWidth("100px").setFlexGrow(0);
        gridNovedades.addColumn(dto -> "RD$ " + formatearMonto(dto.getTotalIngresosFijos()))
                .setHeader("Total Ing. Extra").setWidth("150px").setFlexGrow(0);
        gridNovedades.addColumn(NovedadNominaDTO::getAusenciasNoPagadasDias)
                .setHeader("Ausencias").setWidth("100px").setFlexGrow(0);

        gridNovedades.addComponentColumn(dto -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> dialogFormularioNovedadesDTO(dto, gridNovedades, periodo, fecha));
            return btnEditar;
        }).setHeader("Acciones").setWidth("100px").setFlexGrow(0);

        gridNovedades.setItems(novedades);
        gridNovedades.setHeight("400px");

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-borde");

        Button btnGenerar = new Button("Generar Corrida Definitiva", new Icon(VaadinIcon.PLAY));
        btnGenerar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnGenerar.addClickListener(e -> {
            try {
                CorridaNomina corrida = corridaNominaService.generarCorrida(
                        periodo, fecha, tipo, periodoFiscal, empleadoUnico, novedades
                );

                dialog.close();
                refrescarGrid(paginator);
                dialogResultadoCorrida(corrida, paginator, true);

            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });

        dialog.add(gridNovedades);
        dialog.getFooter().add(btnGenerar, btnCancelar);
        dialog.open();
    }

    private void dialogFormularioNovedadesDTO(NovedadNominaDTO dto, Grid<NovedadNominaDTO> grid, PeriodoNomina periodo, LocalDate fechaCorrida) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Novedades — " + dto.getEmpleado().getPersona().getNombre());
        dialog.setWidth("80vw");
        dialog.setMaxWidth("900px");

        NumberField horasExtras = new NumberField("Horas extras");
        horasExtras.setMin(0);
        horasExtras.setPlaceholder("0");
        horasExtras.setWidthFull();
        horasExtras.setValue(dto.getHorasExtras() != null ? dto.getHorasExtras().doubleValue() : 0d);

        BigDecimalField comisionesRegulares = crearCampoMoneda("Comisiones Regulares");
        comisionesRegulares.setValue(dto.getComisionesRegulares());

        BigDecimalField comisionesExtraordinarias = crearCampoMoneda("Comisiones Extraordinarias / Bonos");
        comisionesExtraordinarias.setValue(dto.getComisionesExtraordinarias());

        BigDecimalField dietasYViaticos = crearCampoMoneda("Dietas y viáticos");
        dietasYViaticos.setValue(dto.getDietasViaticos());

        NumberField ausencias = new NumberField("Ausencias no pagadas (días)");
        ausencias.setMin(0);

        int maxDias;
        if (periodo == PeriodoNomina.MES) {
            maxDias = fechaCorrida.lengthOfMonth();
        } else if (periodo == PeriodoNomina.SEMANAL) {
            maxDias = 7;
        } else {
            maxDias = (fechaCorrida.getDayOfMonth() <= 15) ? 15 : (fechaCorrida.lengthOfMonth() - 15);
        }
        ausencias.setMax(maxDias);
        ausencias.setStep(0.5);
        ausencias.setPlaceholder("0");
        ausencias.setWidthFull();
        ausencias.setValue(dto.getAusenciasNoPagadasDias() != null ? dto.getAusenciasNoPagadasDias().doubleValue() : 0d);

        FormLayout formIngresos = new FormLayout(horasExtras, comisionesRegulares, comisionesExtraordinarias, dietasYViaticos);
        formIngresos.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("300px", 2)
        );

        VerticalLayout seccionIngresos = new VerticalLayout(new Span("Ingresos Adicionales"), formIngresos);
        seccionIngresos.addClassNames("caja-novedades", "seccion-ingresos");
        seccionIngresos.setPadding(false);

        FormLayout formDeducciones = new FormLayout(ausencias);
        formDeducciones.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("300px", 2)
        );

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
            if (ausencias.getValue() != null && ausencias.getValue() > maxDias) {
                mostrarError("La cantidad de ausencias (" + ausencias.getValue()
                        + ") excede el límite máximo de " + maxDias + " días para este período.");
                return;
            }

            dto.setHorasExtras(horasExtras.getValue() != null ? horasExtras.getValue().intValue() : 0);
            dto.setComisionesRegulares(comisionesRegulares.getValue() != null ? comisionesRegulares.getValue() : BigDecimal.ZERO);
            dto.setComisionesExtraordinarias(comisionesExtraordinarias.getValue() != null ? comisionesExtraordinarias.getValue() : BigDecimal.ZERO);
            dto.setDietasViaticos(dietasYViaticos.getValue() != null ? dietasYViaticos.getValue() : BigDecimal.ZERO);
            dto.setAusenciasNoPagadasDias(ausencias.getValue() != null ? ausencias.getValue().intValue() : 0);

            grid.getDataProvider().refreshItem(dto);
            dialog.close();
            mostrarExito("Novedades registradas en memoria.");
        });

        dialog.add(layoutPrincipal);
        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private void dialogResultadoCorrida(
            CorridaNomina corrida,
            GridPaginator<CorridaNomina> paginator,
            boolean esModoEdicion
    ) {
        Dialog dialog = new Dialog();

        String tituloDialog;
        if (corrida.getTipo() == TipoCorrida.ORDINARIA) {
            tituloDialog = "Resultado — " + corrida.getTipo().getDescripcion() + " | " +
                    corrida.getPeriodo().getDescripcion() + " " + corrida.getFechaEmision();
        } else {
            tituloDialog = "Resultado — " + corrida.getTipo().getDescripcion() + " | " + corrida.getFechaEmision();
        }

        dialog.setHeaderTitle(tituloDialog);
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
            return btnVer;
        }).setWidth("90px").setFlexGrow(0).setHeader("Acciones");

        gridNominas.setItems(corrida.getNominas());
        gridNominas.setHeight("300px");

        Span totalGeneral = new Span("Total general: RD$ " + formatearMonto(corrida.getTotalGeneral()));
        totalGeneral.getStyle().set("font-weight", "bold").set("color", "#1a56db")
                .set("align-self", "flex-end")
                .set("margin-top", "10px");

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
