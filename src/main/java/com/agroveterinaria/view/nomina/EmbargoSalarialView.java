package com.agroveterinaria.view.nomina;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.CuotaExtraEmbargo;
import com.agroveterinaria.entity.EmbargoSalarial;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.enums.TipoEmbargo;
import com.agroveterinaria.service.AnticipoSalarioService;
import com.agroveterinaria.service.ConfiguracionNominaService;
import com.agroveterinaria.service.EmbargoSalarialService;
import com.agroveterinaria.service.EmpleadoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class EmbargoSalarialView extends VerticalLayout {

    private final EmbargoSalarialService embargoService;
    private final EmpleadoService empleadoService;
    private final ConfiguracionNominaService configuracionNominaService;
    private final AnticipoSalarioService anticipoSalarioService;

    public EmbargoSalarialView(EmbargoSalarialService embargoService, EmpleadoService empleadoService, ConfiguracionNominaService configuracionNominaService, AnticipoSalarioService anticipoSalarioService) {
        this.embargoService = embargoService;
        this.empleadoService = empleadoService;
        this.configuracionNominaService = configuracionNominaService;
        this.anticipoSalarioService = anticipoSalarioService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);
        getStyle().set("margin-top", "12px");

        crearContenido();
    }

    private void crearContenido() {
        Grid<EmbargoSalarial> gridEmbargos = new Grid<>(EmbargoSalarial.class, false);
        GridPaginator<EmbargoSalarial> paginator = new GridPaginator<>(gridEmbargos, 10, "embargos");
        gridEmbargos.addClassName("embargo-grid");
        gridEmbargos.addThemeNames("row-stripes");
        gridEmbargos.setWidthFull();
        gridEmbargos.setHeight("390px");

        gridEmbargos.addColumn(e -> e.getEmpleado().getPersona().getNombre() + " " + e.getEmpleado().getPersona().getApellido())
                .setHeader("Empleado").setFlexGrow(1).setSortable(true);
        gridEmbargos.addColumn(EmbargoSalarial::getEntidadDemandante)
                .setHeader("Demandante").setWidth("150px").setFlexGrow(0);
        gridEmbargos.addColumn(e -> "RD$ " + formatearMonto(e.getMontoCuotaOrdinaria()))
                .setHeader("Cuota Ord.").setWidth("130px").setFlexGrow(0);
        gridEmbargos.addColumn(e -> "RD$ " + formatearMonto(e.getSaldoPendienteMora()))
                .setHeader("Mora").setWidth("130px").setFlexGrow(0);

        gridEmbargos.addComponentColumn(this::crearBadgeEstadoEntidad)
                .setHeader("Estado").setWidth("120px").setFlexGrow(0);

        gridEmbargos.addComponentColumn(embargo -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> dialogFormularioEmbargo(embargo, paginator));

            HorizontalLayout acciones = new HorizontalLayout(btnEditar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("100px").setFlexGrow(0);

        Button btnNuevo = new Button("Nuevo Embargo", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.addClickListener(e -> dialogFormularioEmbargo(new EmbargoSalarial(), paginator));

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin-bottom", "12px");

        refrescarGrid(paginator);
        add(toolbar, paginator, gridEmbargos);
    }

    private void dialogFormularioEmbargo(EmbargoSalarial embargo, GridPaginator<EmbargoSalarial> paginator) {
        Dialog dialog = new Dialog();
        boolean isEdit = embargo.getIdEmbargo() != null;
        dialog.setHeaderTitle(isEdit ? "Editar Embargo" : "Registrar Embargo");
        dialog.setWidth("80vw");
        dialog.setMaxWidth("800px");

        ComboBox<Empleado> cmbEmpleado = new ComboBox<>("Empleado");
        cmbEmpleado.setItems(empleadoService.findByStatus(StatusEntidad.ACTIVO));
        cmbEmpleado.setItemLabelGenerator(e -> e.getPersona().getNombre() + " " + e.getPersona().getApellido());
        cmbEmpleado.setValue(embargo.getEmpleado());
        cmbEmpleado.setReadOnly(isEdit);
        cmbEmpleado.setWidthFull();

        TextField txtDemandante = new TextField("Entidad Demandante");
        txtDemandante.setValue(embargo.getEntidadDemandante() != null ? embargo.getEntidadDemandante() : "");
        txtDemandante.setWidthFull();

        ComboBox<TipoEmbargo> cmbTipo = new ComboBox<>("Tipo");
        cmbTipo.setItems(TipoEmbargo.values());
        cmbTipo.setValue(embargo.getTipoEmbargo());
        cmbTipo.setWidthFull();

        BigDecimalField numCuota = crearCampoMoneda("Cuota Ordinaria");
        numCuota.setValue(embargo.getMontoCuotaOrdinaria());

        DatePicker fechaNotificacion = new DatePicker("Fecha Notificación");
        fechaNotificacion.setValue(embargo.getFechaNotificacion() != null ? embargo.getFechaNotificacion() : LocalDate.now());
        fechaNotificacion.setWidthFull();

        ComboBox<StatusEntidad> cmbEstado = new ComboBox<>("Estado");
        cmbEstado.setItems(StatusEntidad.values());
        cmbEstado.setValue(embargo.getEstado() != null ? embargo.getEstado() : StatusEntidad.ACTIVO);
        cmbEstado.setWidthFull();

        FormLayout formLayout = new FormLayout(cmbEmpleado, txtDemandante, cmbTipo, numCuota, fechaNotificacion, cmbEstado);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("400px", 2));

        Binder<EmbargoSalarial> binder = new Binder<>(EmbargoSalarial.class);

        binder.forField(cmbEmpleado).asRequired("Seleccione un empleado").bind(EmbargoSalarial::getEmpleado, EmbargoSalarial::setEmpleado);
        binder.forField(txtDemandante).asRequired("La entidad es obligatoria").bind(EmbargoSalarial::getEntidadDemandante, EmbargoSalarial::setEntidadDemandante);
        binder.forField(cmbTipo).asRequired("Seleccione un tipo").bind(EmbargoSalarial::getTipoEmbargo, EmbargoSalarial::setTipoEmbargo);
        binder.forField(fechaNotificacion).asRequired("Seleccione una fecha").bind(EmbargoSalarial::getFechaNotificacion, EmbargoSalarial::setFechaNotificacion);
        binder.forField(cmbEstado).asRequired("Seleccione un estado").bind(EmbargoSalarial::getEstado, EmbargoSalarial::setEstado);

        binder.forField(numCuota)
                .asRequired("La cuota es obligatoria")
                .withValidator(monto -> monto.compareTo(BigDecimal.ZERO) > 0, "El monto debe ser mayor a RD$ 0.00")
                .bind(EmbargoSalarial::getMontoCuotaOrdinaria, EmbargoSalarial::setMontoCuotaOrdinaria);

        if (!isEdit) {
            embargo.setEstado(StatusEntidad.ACTIVO);
            embargo.setFechaNotificacion(LocalDate.now());
        }

        binder.readBean(embargo);

        List<CuotaExtraEmbargo> listaCuotas = new ArrayList<>();
        if (embargo.getCuotasExtras() != null) {
            listaCuotas.addAll(embargo.getCuotasExtras());
        }

        Grid<CuotaExtraEmbargo> gridCuotas = new Grid<>(CuotaExtraEmbargo.class, false);
        gridCuotas.setHeight("200px");
        gridCuotas.addColumn(CuotaExtraEmbargo::getMesAplicacion).setHeader("Mes").setAutoWidth(true);
        gridCuotas.addColumn(c -> "RD$ " + formatearMonto(c.getMontoExtra())).setHeader("Monto").setAutoWidth(true);
        gridCuotas.addColumn(CuotaExtraEmbargo::getConcepto).setHeader("Concepto").setFlexGrow(1);
        gridCuotas.addComponentColumn(cuota -> {
            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            btnEliminar.addClickListener(e -> {
                listaCuotas.remove(cuota);
                gridCuotas.setItems(listaCuotas);
            });
            return btnEliminar;
        }).setHeader("").setWidth("70px");
        gridCuotas.setItems(listaCuotas);

        ComboBox<Integer> cmbMes = new ComboBox<>();
        cmbMes.setItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        cmbMes.setPlaceholder("Mes");
        cmbMes.setWidth("100px");

        BigDecimalField txtMontoExtra = crearCampoMoneda("");
        txtMontoExtra.setPlaceholder("Monto");
        txtMontoExtra.setWidth("150px");

        TextField txtConceptoExtra = new TextField();
        txtConceptoExtra.setPlaceholder("Concepto");
        txtConceptoExtra.setWidth("200px");

        Button btnAgregarCuota = new Button(new Icon(VaadinIcon.PLUS), e -> {
            if (cmbMes.isEmpty() || txtMontoExtra.isEmpty() || txtConceptoExtra.isEmpty()) {
                mostrarError("Complete los datos de la cuota extra.");
                return;
            }

            if (txtMontoExtra.getValue().compareTo(BigDecimal.ZERO) < 0) {
                mostrarError("El monto de la cuota extra no puede ser negativo.");
                return;
            }
            CuotaExtraEmbargo nuevaCuota = new CuotaExtraEmbargo();
            nuevaCuota.setMesAplicacion(cmbMes.getValue());
            nuevaCuota.setMontoExtra(txtMontoExtra.getValue());
            nuevaCuota.setConcepto(txtConceptoExtra.getValue());
            nuevaCuota.setEmbargoSalarial(embargo);

            listaCuotas.add(nuevaCuota);
            gridCuotas.setItems(listaCuotas);

            cmbMes.clear(); txtMontoExtra.clear(); txtConceptoExtra.clear();
        });

        HorizontalLayout layoutAddCuota = new HorizontalLayout(cmbMes, txtMontoExtra, txtConceptoExtra, btnAgregarCuota);
        VerticalLayout seccionCuotas = new VerticalLayout(new H3("Cuotas Extraordinarias"), layoutAddCuota, gridCuotas);
        seccionCuotas.setPadding(false);

        VerticalLayout contenido = new VerticalLayout(formLayout, seccionCuotas);
        contenido.setPadding(false);
        dialog.add(contenido);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-borde");

        Button btnGuardar = new Button("Guardar", new Icon(VaadinIcon.CHECK));
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnGuardar.addClickListener(e -> {
            if (binder.writeBeanIfValid(embargo)) {
                embargo.getCuotasExtras().clear();
                listaCuotas.forEach(embargo::addCuotaExtra);

                try {
                    embargoService.save(embargo);
                    dialog.close();
                    refrescarGrid(paginator);
                    mostrarExito("Embargo guardado correctamente.");
                } catch (Exception ex) {
                    mostrarError(ex.getMessage());
                }
            } else {
                mostrarError("Por favor, corrija los errores en el formulario.");
            }
        });

        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private HorizontalLayout crearBadgeEstadoEntidad(EmbargoSalarial embargo) {
        Span circulo = new Span();
        circulo.getStyle().set("width", "10px").set("height", "10px")
                .set("border-radius", "50%").set("display", "inline-block");

        boolean esActivo = embargo.getEstado() == StatusEntidad.ACTIVO;
        circulo.getStyle().set("background-color", esActivo ? "#2e7d32" : "#9e9e9e");

        Span texto = new Span(embargo.getEstado().name());
        texto.getStyle().set("color", esActivo ? "#2e7d32" : "#9e9e9e").set("font-weight", "500");

        HorizontalLayout layoutEstado = new HorizontalLayout(circulo, texto);
        layoutEstado.setAlignItems(Alignment.CENTER);
        layoutEstado.setSpacing(true);
        return layoutEstado;
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

    private void refrescarGrid(GridPaginator<EmbargoSalarial> paginator) {
        paginator.setItems(embargoService.findAllParaVista());
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}