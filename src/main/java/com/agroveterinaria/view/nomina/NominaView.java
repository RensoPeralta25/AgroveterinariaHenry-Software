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
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


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

        gridCorridas.addColumn(corrida -> {
            if (corrida.getFechaInicio() != null && corrida.getFechaFin() != null) {
                return corrida.getFechaInicio() + " - " + corrida.getFechaFin();
            }
            return "N/A";
        }).setHeader("Período Cubierto").setSortable(true).setAutoWidth(true);

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
            btnEditar.addClickListener(e -> {
                List<NovedadNominaDTO> novedadesPrevias = extraerNovedadesDeCorrida(corrida);

                dialogPreparacionNovedades(
                        corrida.getPeriodo(), corrida.getFechaInicio(), corrida.getFechaFin(), corrida.getFechaEmision(), corrida.getTipo(),
                        corrida.getPeriodoFiscal(), null, novedadesPrevias, paginator, corrida
                );
            });

            Button btnAprobar = new Button(new Icon(VaadinIcon.CHECK));
            btnAprobar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnAprobar.setEnabled(corrida.getEstado() == EstadoCorrida.PENDIENTE);
            btnAprobar.addClickListener(e -> confirmarAprobacion(corrida, paginator));

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
        dialog.setWidth("600px");

        ComboBox<TipoCorrida> cmbTipo = new ComboBox<>("Tipo de Corrida");
        cmbTipo.setItems(TipoCorrida.values());
        cmbTipo.setValue(TipoCorrida.ORDINARIA);
        cmbTipo.setItemLabelGenerator(TipoCorrida::getDescripcion);
        cmbTipo.setWidthFull();

        ComboBox<PeriodoNomina> cmbPeriodo = new ComboBox<>("Período");
        cmbPeriodo.setItems(PeriodoNomina.values());
        cmbPeriodo.setItemLabelGenerator(PeriodoNomina::getDescripcion);
        cmbPeriodo.setWidthFull();

        DatePicker fechaInicio = new DatePicker("Inicio del período");
        DatePicker fechaFin = new DatePicker("Fin del período");
        DatePicker fechaEmision = new DatePicker("Fecha de emisión");

        LocalDate hoy = LocalDate.now();
        LocalDate minFecha = hoy.minusMonths(3).withDayOfMonth(1);

        fechaInicio.setMin(minFecha);
        fechaInicio.setMax(hoy);

        fechaFin.setReadOnly(true);

        fechaEmision.setMin(minFecha);
        fechaEmision.setMax(hoy);
        fechaEmision.setValue(LocalDate.now());

        fechaInicio.setWidthFull();
        fechaFin.setWidthFull();
        fechaEmision.setWidthFull();

        HorizontalLayout layoutFechas = new HorizontalLayout(fechaInicio, fechaFin, fechaEmision);
        layoutFechas.setWidthFull();
        layoutFechas.setSpacing(true);

        cmbPeriodo.addValueChangeListener(e -> {
            PeriodoNomina periodoSeleccionado = e.getValue();

            if (periodoSeleccionado != null) {
                LocalDate proximoInicio = corridaNominaService.obtenerProximaFechaInicioOrdinaria(periodoSeleccionado);

                fechaInicio.setReadOnly(false);

                if (proximoInicio != null) {
                    fechaInicio.setValue(proximoInicio);
                    fechaInicio.setReadOnly(true);
                } else {
                    if (e.isFromClient() || e.getOldValue() != null) {
                        fechaInicio.clear();
                        fechaFin.clear();
                    }
                }
            }
        });

        cmbTipo.addValueChangeListener(e -> {
            if (e.isFromClient() || e.getOldValue() != null) {
                fechaInicio.setReadOnly(false);
                fechaInicio.clear();
                fechaFin.clear();
            }
        });

        fechaInicio.addValueChangeListener(e -> {
            LocalDate inicio = e.getValue();
            if (inicio == null) {
                fechaFin.clear();
                return;
            }

            PeriodoNomina periodo = cmbPeriodo.getValue();
            if (periodo == null) return;

            if (periodo == PeriodoNomina.SEMANAL) {
                LocalDate proximoSabado = inicio;
                while (proximoSabado.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
                    proximoSabado = proximoSabado.plusDays(1);
                }
                fechaFin.setValue(proximoSabado);

            } else if (periodo == PeriodoNomina.QUINCENA) {
                if (inicio.getDayOfMonth() <= 15) {
                    fechaFin.setValue(inicio.withDayOfMonth(15));
                } else {
                    fechaFin.setValue(inicio.withDayOfMonth(inicio.lengthOfMonth()));
                }

            } else if (periodo == PeriodoNomina.MES) {
                fechaFin.setValue(inicio.withDayOfMonth(inicio.lengthOfMonth()));
            }
        });

        ComboBox<PeriodoFiscal> cmbPeriodoFiscal = new ComboBox<>("Período Fiscal");
        cmbPeriodoFiscal.setItems(periodoFiscalService.obtenerPeriodosDisponiblesParaBonificacion());
        cmbPeriodoFiscal.setItemLabelGenerator(p -> p.getAnio() + "");
        cmbPeriodoFiscal.setWidthFull();
        cmbPeriodoFiscal.setVisible(false);

        ComboBox<Empleado> cmbEmpleado = new ComboBox<>("Empleado (Solo Pago Individual)");
        cmbEmpleado.setItems(empleadoService.findByStatus(StatusEntidad.ACTIVO));
        cmbEmpleado.setItemLabelGenerator(e -> e.getPersona().getNombre() + " " + e.getPersona().getApellido());
        cmbEmpleado.setWidthFull();
        cmbEmpleado.setVisible(false);

        cmbTipo.addValueChangeListener(event -> {
            TipoCorrida tipo = event.getValue();
            cmbPeriodoFiscal.setVisible(tipo == TipoCorrida.BONIFICACION);
            cmbEmpleado.setVisible(tipo == TipoCorrida.VACACIONES_ANTICIPADAS);

            boolean esOrdinaria = (tipo == TipoCorrida.ORDINARIA);
            cmbPeriodo.setVisible(esOrdinaria);
            fechaInicio.setVisible(esOrdinaria);
            fechaFin.setVisible(esOrdinaria);

            if (tipo == TipoCorrida.BONIFICACION || tipo == TipoCorrida.REGALIA_PASCUAL) {
                cmbPeriodo.setValue(PeriodoNomina.MES);
            }

            if (tipo == TipoCorrida.VACACIONES_ANTICIPADAS) {
                cmbPeriodo.setValue(PeriodoNomina.QUINCENA);
            }
        });

        VerticalLayout contenido = new VerticalLayout(cmbTipo, cmbPeriodo, layoutFechas, cmbPeriodoFiscal, cmbEmpleado);
        contenido.setPadding(false);
        contenido.setSpacing(true);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnCancelar.addClassName("btn-borde");

        Button btnGenerar = new Button("Generar", new Icon(VaadinIcon.PLAY));
        btnGenerar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnGenerar.addClickListener(e -> {
            if (cmbTipo.isEmpty() || (cmbPeriodo.isVisible() && cmbPeriodo.isEmpty()) || fechaEmision.isEmpty()) {
                mostrarError("Debes completar los campos principales.");
                return;
            }

            if (fechaInicio.isVisible() && (fechaInicio.isEmpty() || fechaFin.isEmpty())) {
                mostrarError("Debes especificar el rango de fechas para la nómina ordinaria.");
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
            LocalDate vInicio = fechaInicio.getValue();
            LocalDate vFin = fechaFin.getValue();
            LocalDate vEmision = fechaEmision.getValue();
            TipoCorrida tipo = cmbTipo.getValue();

            try {
                corridaNominaService.validarDisponibilidadDePeriodo(periodo, vInicio, vFin, vEmision, tipo);
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
                dialogPreparacionNovedades(periodo, vInicio, vFin, vEmision, tipo, cmbPeriodoFiscal.getValue(), cmbEmpleado.getValue(), novedadesMemoria, paginator, null);
            } else {
                try {
                    CorridaNomina corrida = corridaNominaService.generarCorrida(
                            periodo, vInicio, vFin, vEmision, tipo, cmbPeriodoFiscal.getValue(), cmbEmpleado.getValue(), novedadesMemoria
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

    private void confirmarAprobacion(CorridaNomina corrida, GridPaginator<CorridaNomina> paginator) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Aprobar Nómina");

        confirm.add(new Span("¿Está seguro que desea aprobar la nómina " + corrida.getPeriodo().name() +
                " de tipo " + corrida.getTipo().name() + "? Una vez aprobada, los pagos y descuentos serán aplicados definitivamente."));

        Button btnSi = new Button("Sí, Aprobar", e -> {
            try {
                corridaNominaService.aprobarCorrida(corrida);
                mostrarExito("Corrida aprobada correctamente.");
                confirm.close();
                refrescarGrid(paginator);
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });
        btnSi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnNo = new Button("Cancelar", e -> confirm.close());
        btnNo.addClassName("btn-borde");

        confirm.getFooter().add(btnSi, btnNo);
        confirm.open();
    }

    private void dialogPreparacionNovedades(
            PeriodoNomina periodo, LocalDate fechaInicio, LocalDate fechaFin, LocalDate fechaEmision, TipoCorrida tipo,
            PeriodoFiscal periodoFiscal, Empleado empleadoUnico,
            List<NovedadNominaDTO> novedades, GridPaginator<CorridaNomina> paginator,
            CorridaNomina corridaOriginal) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(corridaOriginal == null ? "Preparación de Novedades" : "Edición de Novedades");
        dialog.setWidth("80vw");
        dialog.setMaxWidth("900px");

        Grid<NovedadNominaDTO> gridNovedades = new Grid<>(NovedadNominaDTO.class, false);
        gridNovedades.addColumn(dto -> dto.getEmpleado().getPersona().getNombre() + " " + dto.getEmpleado().getPersona().getApellido())
                .setHeader("Empleado").setFlexGrow(1);
        gridNovedades.addColumn(NovedadNominaDTO::getHorasExtras)
                .setHeader("Horas Ext.").setWidth("100px").setFlexGrow(0);
        gridNovedades.addColumn(dto -> "RD$ " + formatearMonto(dto.getTotalIngresosExtra()))
                .setHeader("Total Ingresos Extra").setWidth("150px").setFlexGrow(0);

        gridNovedades.addComponentColumn(dto -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> dialogFormularioNovedadesDTO(dto, gridNovedades, periodo));
            return btnEditar;
        }).setHeader("Acciones").setWidth("100px").setFlexGrow(0);

        List<NovedadNominaDTO> novedadesOrdenadas = novedades.stream()
                .sorted(java.util.Comparator.comparing(dto -> {
                    String nombre = dto.getEmpleado().getPersona().getNombre() != null ? dto.getEmpleado().getPersona().getNombre() : "";
                    String apellido = dto.getEmpleado().getPersona().getApellido() != null ? dto.getEmpleado().getPersona().getApellido() : "";
                    return (nombre + " " + apellido).trim().toLowerCase();
                }))
                .toList();

        TextField txtBuscar = new TextField();
        txtBuscar.setPlaceholder("Buscar empleado para agregar novedad...");
        txtBuscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtBuscar.setWidthFull();
        txtBuscar.getStyle().set("margin-bottom", "8px");
        txtBuscar.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);

        txtBuscar.addValueChangeListener(e -> {
            String termino = e.getValue() != null ? e.getValue().toLowerCase().trim() : "";
            if (termino.isEmpty()) {
                gridNovedades.setItems(novedadesOrdenadas);
            } else {
                List<NovedadNominaDTO> filtrados = novedadesOrdenadas.stream()
                        .filter(dto -> {
                            String nombreCompleto = (dto.getEmpleado().getPersona().getNombre() + " " +
                                    dto.getEmpleado().getPersona().getApellido()).toLowerCase();
                            return nombreCompleto.contains(termino);
                        })
                        .toList();
                gridNovedades.setItems(filtrados);
            }
        });

        gridNovedades.setItems(novedadesOrdenadas);
        gridNovedades.setHeight("400px");

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-borde");

        Button btnGenerar = new Button("Generar Corrida Definitiva", new Icon(VaadinIcon.PLAY));
        btnGenerar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnGenerar.addClickListener(e -> {
            try {
                if (corridaOriginal != null) {
                    corridaNominaService.delete(corridaOriginal);
                }

                CorridaNomina corrida = corridaNominaService.generarCorrida(
                        periodo, fechaInicio, fechaFin, fechaEmision, tipo, periodoFiscal, empleadoUnico, novedades
                );

                dialog.close();
                refrescarGrid(paginator);
                dialogResultadoCorrida(corrida, paginator, true);

            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });

        dialog.add(txtBuscar, gridNovedades);
        dialog.getFooter().add(btnGenerar, btnCancelar);
        dialog.open();
    }

    private void dialogFormularioNovedadesDTO(NovedadNominaDTO dto, Grid<NovedadNominaDTO> grid, PeriodoNomina periodo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Novedades — " + dto.getEmpleado().getPersona().getNombre());
        dialog.setWidth("80vw");
        dialog.setMaxWidth("900px");

        int maxHorasExtras = switch (periodo) {
            case SEMANAL -> configuracionNominaService.getMaxHorasExtrasSemanal();
            case QUINCENA -> configuracionNominaService.getMaxHorasExtrasQuincenal();
            case MES -> configuracionNominaService.getMaxHorasExtrasMensual();
        };

        NumberField horasExtras = new NumberField("Horas extras");
        horasExtras.setMin(0);
        horasExtras.setPlaceholder("0");
        horasExtras.setWidthFull();
        horasExtras.setValue((dto.getHorasExtras() != null && dto.getHorasExtras() > 0) ? dto.getHorasExtras().doubleValue() : null);

        BigDecimalField comisionesRegulares = crearCampoMoneda("Comisiones Regulares");
        comisionesRegulares.setValue((dto.getComisionesRegulares() != null && dto.getComisionesRegulares().compareTo(BigDecimal.ZERO) > 0) ? dto.getComisionesRegulares() : null);

        BigDecimalField comisionesExtraordinarias = crearCampoMoneda("Comisiones Extraordinarias / Bonos");
        comisionesExtraordinarias.setValue((dto.getComisionesExtraordinarias() != null && dto.getComisionesExtraordinarias().compareTo(BigDecimal.ZERO) > 0) ? dto.getComisionesExtraordinarias() : null);

        BigDecimalField dietasYViaticos = crearCampoMoneda("Dietas y viáticos");
        dietasYViaticos.setValue((dto.getDietasViaticos() != null && dto.getDietasViaticos().compareTo(BigDecimal.ZERO) > 0) ? dto.getDietasViaticos() : null);

        BigDecimalField reembolsoLicencias = crearCampoMoneda("Reembolso Licencias Médicas");
        reembolsoLicencias.setValue((dto.getReembolsoLicencias() != null && dto.getReembolsoLicencias().compareTo(BigDecimal.ZERO) > 0) ? dto.getReembolsoLicencias() : null);

        FormLayout formIngresos = new FormLayout(horasExtras, comisionesRegulares, comisionesExtraordinarias, dietasYViaticos, reembolsoLicencias);
        formIngresos.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("300px", 2)
        );

        VerticalLayout seccionIngresos = new VerticalLayout(new Span("Ingresos Adicionales"), formIngresos);
        seccionIngresos.addClassNames("caja-novedades", "seccion-ingresos");
        seccionIngresos.setPadding(false);

        VerticalLayout layoutPrincipal = new VerticalLayout(seccionIngresos);
        layoutPrincipal.setPadding(false);
        layoutPrincipal.setSpacing(false);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        Button btnGuardar = new Button("Guardar", new Icon(VaadinIcon.CHECK));
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnGuardar.addClickListener(e -> {
            if (horasExtras.getValue() != null && (horasExtras.getValue() < 0 || horasExtras.getValue() > maxHorasExtras)) {
                mostrarError("Las horas extras deben estar entre 0 y " + maxHorasExtras + " para este período.");
                return;
            }

            if (comisionesRegulares.getValue() != null && comisionesRegulares.getValue().compareTo(BigDecimal.ZERO) < 0) {
                mostrarError("Las comisiones regulares no pueden ser negativas.");
                return;
            }

            if (comisionesExtraordinarias.getValue() != null && comisionesExtraordinarias.getValue().compareTo(BigDecimal.ZERO) < 0) {
                mostrarError("Las comisiones extraordinarias no pueden ser negativas.");
                return;
            }

            if (dietasYViaticos.getValue() != null && dietasYViaticos.getValue().compareTo(BigDecimal.ZERO) < 0) {
                mostrarError("Las dietas y viáticos no pueden ser negativos.");
                return;
            }

            dto.setHorasExtras(horasExtras.getValue() != null ? horasExtras.getValue().intValue() : 0);
            dto.setComisionesRegulares(comisionesRegulares.getValue() != null ? comisionesRegulares.getValue() : BigDecimal.ZERO);
            dto.setComisionesExtraordinarias(comisionesExtraordinarias.getValue() != null ? comisionesExtraordinarias.getValue() : BigDecimal.ZERO);
            dto.setDietasViaticos(dietasYViaticos.getValue() != null ? dietasYViaticos.getValue() : BigDecimal.ZERO);
            dto.setReembolsoLicencias((reembolsoLicencias.getValue() != null ? reembolsoLicencias.getValue() : BigDecimal.ZERO));

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
        List<Nomina> nominasOrdenadas = corrida.getNominas().stream()
                .sorted(Comparator.comparing(n -> n.getEmpleado().getPersona().getNombre() + " " + n.getEmpleado().getPersona().getApellido()))
                .collect(Collectors.toList());

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
        gridNominas.addColumn(n -> n.getEmpleado().getPersona().getNombre() + " " + n.getEmpleado().getPersona().getApellido())
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

        TextField txtBuscar = new TextField();
        txtBuscar.setPlaceholder("Buscar empleado...");
        txtBuscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtBuscar.setWidthFull();
        txtBuscar.getStyle().set("margin-top", "8px");
        txtBuscar.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);

        txtBuscar.addValueChangeListener(e -> {
            String termino = e.getValue() != null ? e.getValue().toLowerCase().trim() : "";

            if (termino.isEmpty()) {
                gridNominas.setItems(nominasOrdenadas);
            } else {
                List<Nomina> filtrados = nominasOrdenadas.stream()
                        .filter(n -> {
                            String nombreCompleto = (n.getEmpleado().getPersona().getNombre() + " " +
                                    n.getEmpleado().getPersona().getApellido()).toLowerCase();
                            return nombreCompleto.contains(termino);
                        })
                        .collect(Collectors.toList());
                gridNominas.setItems(filtrados);
            }
        });

        gridNominas.setItems(nominasOrdenadas);
        gridNominas.setHeight("300px");

        Span totalGeneral = new Span("Total general: RD$ " + formatearMonto(corrida.getTotalGeneral()));
        totalGeneral.getStyle().set("font-weight", "bold").set("color", "#1a56db")
                .set("align-self", "flex-end")
                .set("margin-top", "10px");

        VerticalLayout contenido = new VerticalLayout(txtBuscar, gridNominas, totalGeneral);
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
        dialog.setWidth("80vw");
        dialog.setMaxWidth("750px");

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

    private List<NovedadNominaDTO> extraerNovedadesDeCorrida(CorridaNomina corrida) {
        List<NovedadNominaDTO> listaNovedades = new ArrayList<>();

        for (Nomina nomina : corrida.getNominas()) {
            NovedadNominaDTO dto = new NovedadNominaDTO(nomina.getEmpleado());

            for (DetalleNomina detalle : nomina.getDetalles()) {
                switch (detalle.getTipo()) {
                    case HORAS_EXTRAS -> dto.setHorasExtras(detalle.getCantidad().intValue());
                    case COMISIONES_REGULARES -> dto.setComisionesRegulares(detalle.getMonto());
                    case COMISIONES_EXTRAORDINARIAS -> dto.setComisionesExtraordinarias(detalle.getMonto());
                }
            }

            if (dto.getHorasExtras() != null || dto.getComisionesRegulares() != null ||
                    dto.getComisionesExtraordinarias() != null) {
                listaNovedades.add(dto);
            }
        }
        return listaNovedades;
    }
}
