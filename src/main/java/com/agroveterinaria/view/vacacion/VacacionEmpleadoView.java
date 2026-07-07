package com.agroveterinaria.view.vacacion;

import com.agroveterinaria.component.CrudGridPaginator;
import com.agroveterinaria.entity.DiaFeriado;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.VacacionEmpleado;
import com.agroveterinaria.service.ConfiguracionNominaService;
import com.agroveterinaria.service.DiaFeriadoService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.VacacionEmpleadoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class VacacionEmpleadoView extends VerticalLayout {

    private VacacionEmpleado vacacionActual;

    public VacacionEmpleadoView(VacacionEmpleadoService vacacionEmpleadoService, EmpleadoService empleadoService,
                                DiaFeriadoService diaFeriadoService, ConfiguracionNominaService configuracionNominaService) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);

        GridCrud<VacacionEmpleado> crudVacaciones = new GridCrud<>(VacacionEmpleado.class, new WindowBasedCrudLayout());
        crudVacaciones.getGrid().addClassName("vacacion-grid");
        crudVacaciones.getStyle().set("margin-top", "0");
        CrudGridPaginator<VacacionEmpleado> paginator = new CrudGridPaginator<>(10, "vacaciones");
        paginator.setRefreshOperation(crudVacaciones::refreshGrid);

        crudVacaciones.getGrid().removeAllColumns();

        crudVacaciones.getGrid().addColumn(v -> v.getEmpleado().getPersona().getNombre()).setHeader("Empleado").setSortable(true);
        crudVacaciones.getGrid().addColumn(VacacionEmpleado::getFechaInicio).setHeader("Fecha Inicio");
        crudVacaciones.getGrid().addColumn(VacacionEmpleado::getFechaFin).setHeader("Fecha Fin");
        crudVacaciones.getGrid().addColumn(VacacionEmpleado::getCantidadDiasDescanso).setHeader("Días");
        crudVacaciones.getGrid().addColumn(v -> v.isPagadoPorAdelantado() ? "Sí" : "No").setHeader("Pagado Adelantado");

        crudVacaciones.getGrid().addComponentColumn(vacacion -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.getElement().setProperty("title", "Editar vacación");
            btnEditar.addClickListener(e -> {
                crudVacaciones.getGrid().select(vacacion);
                crudVacaciones.getUpdateButton().click();
            });

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnEliminar.getElement().setProperty("title", "Eliminar registro");
            btnEliminar.addClickListener(e -> {
                crudVacaciones.getGrid().select(vacacion);
                crudVacaciones.getDeleteButton().click();
            });

            HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("100px").setFlexGrow(0);

        crudVacaciones.getGrid().addThemeNames("row-stripes");

        crudVacaciones.getAddButton().setVisible(false);
        crudVacaciones.getUpdateButton().setVisible(false);
        crudVacaciones.getDeleteButton().setVisible(false);
        crudVacaciones.getFindAllButton().setVisible(false);

        Button btnNuevo = new Button("Registrar vacación", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> {
            crudVacaciones.getAddButton().setVisible(true);
            crudVacaciones.getAddButton().click();
            crudVacaciones.getAddButton().setVisible(false);
        });

        TextField buscarVacacion = new TextField();
        buscarVacacion.setWidthFull();
        buscarVacacion.setPlaceholder("Buscar por empleado...");
        buscarVacacion.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarVacacion.setValueChangeMode(ValueChangeMode.LAZY);

        buscarVacacion.addValueChangeListener(
                e -> actualizarFiltroGrid(crudVacaciones, paginator, buscarVacacion.getValue(), vacacionEmpleadoService)
        );

        actualizarFiltroGrid(crudVacaciones, paginator, "", vacacionEmpleadoService);

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, buscarVacacion);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.expand(buscarVacacion);
        toolbar.getStyle().set("margin-bottom", "0");
        toolbar.getStyle().set("padding-top", "10px");

        DefaultCrudFormFactory<VacacionEmpleado> formFactory = (DefaultCrudFormFactory<VacacionEmpleado>) crudVacaciones.getCrudFormFactory();
        formFactory.setUseBeanValidation(true);

        formFactory.setDisabledProperties(CrudOperation.UPDATE, "empleado");

        formFactory.setVisibleProperties("empleado", "fechaInicio", "fechaFin", "cantidadDiasDescanso", "cantidadDiasAPagar");
        formFactory.setFieldCaptions("Empleado", "Fecha de Inicio", "Fecha de Fin", "Días de Descanso", "Días de Pago");

        ComboBox<Empleado> combo = new ComboBox<>("Empleado");

        formFactory.setFieldProvider("empleado", v -> {
            this.vacacionActual = (VacacionEmpleado) v;
            combo.setItems(empleadoService.findByActivoTrue());
            combo.setItemLabelGenerator(e -> e.getPersona().getNombre());
            combo.setWidthFull();
            return combo;
        });

        IntegerField numDiasDescanso = new IntegerField("Cantidad de Días");
        numDiasDescanso.setWidthFull();

        IntegerField numDiasAPagar = new IntegerField("Días a Pagar (Proporcional)");
        numDiasAPagar.setWidthFull();

        formFactory.setFieldCreationListener("cantidadDiasDescanso", field -> {
            field.setReadOnly(true);
        });

        formFactory.setFieldCreationListener("cantidadDiasAPagar", field -> {
            field.setReadOnly(true);
        });

        formFactory.setFieldCreationListener("empleado", field -> {
            field.setRequiredIndicatorVisible(false);
        });

        formFactory.setFieldCreationListener("fechaInicio", field -> {
            field.setRequiredIndicatorVisible(false);
        });

        formFactory.setFieldCreationListener("fechaFin", field -> {
            field.setRequiredIndicatorVisible(false);
        });

        DatePicker dpInicio = new DatePicker("Fecha de Inicio");
        dpInicio.setWidthFull();
        DatePicker dpFin = new DatePicker("Fecha de Fin");
        dpFin.setWidthFull();

        List<LocalDate> feriados = diaFeriadoService.findAll().stream()
                .map(DiaFeriado::getFecha)
                .toList();

        dpInicio.addValueChangeListener(e -> calcularDias(this.vacacionActual, dpInicio, dpFin, combo,
                numDiasDescanso, numDiasAPagar, feriados, configuracionNominaService, vacacionEmpleadoService));
        dpInicio.setMin(LocalDate.now());
        dpInicio.setMax(LocalDate.now().plusYears(1));

        dpFin.addValueChangeListener(e -> calcularDias(this.vacacionActual, dpInicio, dpFin, combo,
                numDiasDescanso, numDiasAPagar, feriados, configuracionNominaService, vacacionEmpleadoService));
        dpFin.setMin(LocalDate.now());
        dpFin.setMax(LocalDate.now().plusYears(1));

        formFactory.setFieldProvider("fechaInicio", v -> {
            this.vacacionActual = (VacacionEmpleado) v;
            return dpInicio;
        });
        formFactory.setFieldProvider("fechaFin", v -> {
            this.vacacionActual = (VacacionEmpleado) v;
            return dpFin;
        });
        formFactory.setFieldProvider("cantidadDiasDescanso", v -> numDiasDescanso);
        formFactory.setFieldProvider("cantidadDiasAPagar", v -> numDiasAPagar);

        formFactory.setErrorListener(this::mostrarError);

        formFactory.setButtonCaption(CrudOperation.ADD, "Registrar");
        formFactory.setButtonCaption(CrudOperation.UPDATE, "Guardar cambios");
        formFactory.setButtonCaption(CrudOperation.DELETE, "Sí, eliminar");
        formFactory.setCancelButtonCaption("Cancelar");

        formFactory.setCaption(CrudOperation.ADD, "Registrar Vacación");
        formFactory.setCaption(CrudOperation.UPDATE, "Editar Vacación");
        formFactory.setCaption(CrudOperation.DELETE, "¿Eliminar Registro?");

        crudVacaciones.setAddOperation(vacacion -> {
            vacacion.setPagadoPorAdelantado(false);
            registrarAprobador(vacacion, empleadoService);
            return vacacionEmpleadoService.save(vacacion);
        });

        crudVacaciones.setUpdateOperation(vacacion -> {
            registrarAprobador(vacacion, empleadoService);
            return vacacionEmpleadoService.update(vacacion);
        });

        crudVacaciones.setDeleteOperation(vacacionEmpleadoService::delete);

        add(toolbar, paginator, crudVacaciones);
    }

    private void calcularDias(VacacionEmpleado vacacionActual, DatePicker inicio, DatePicker fin, ComboBox<Empleado> comboEmpleado,
                              IntegerField numDiasDescanso, IntegerField numDiasAPagar,
                              List<LocalDate> fechasFeriadas, ConfiguracionNominaService configuracionNominaService,
                              VacacionEmpleadoService vacacionService) {

        if (inicio.getValue() != null && fin.getValue() != null && comboEmpleado.getValue() != null) {
            if (!fin.getValue().isBefore(inicio.getValue())) {

                Empleado empleado = comboEmpleado.getValue();
                LocalDate fechaIngreso = empleado.getFechaIngreso();
                LocalDate fechaInicioVac = inicio.getValue();

                int limiteDescanso = configuracionNominaService.getDiasDescansoVacaciones();
                int aniosSenior = configuracionNominaService.getAniosVacacionesSenior();
                int pagoBasico = configuracionNominaService.getDiasPagoVacacionesBasico();
                int pagoSenior = configuracionNominaService.getDiasPagoVacacionesSenior();

                int aniosAntiguedad = (int) ChronoUnit.YEARS.between(fechaIngreso, fechaInicioVac);
                LocalDate inicioAniversarioActual = fechaIngreso.plusYears(aniosAntiguedad);
                LocalDate finAniversarioActual = inicioAniversarioActual.plusYears(1);

                int diasYaTomados = vacacionService.obtenerDiasYaTomados(
                        empleado.getIdEmpleado(), inicioAniversarioActual, finAniversarioActual);

                if (vacacionActual != null && vacacionActual.getId() != null) {
                    VacacionEmpleado original = vacacionService.findById(vacacionActual.getId()).orElse(null);

                    if (original != null) {
                        diasYaTomados = diasYaTomados - original.getCantidadDiasDescanso();
                    }
                }
                int diasDisponibles = limiteDescanso - diasYaTomados;

                LocalDate fechaActual = inicio.getValue();
                LocalDate fechaFinal = fin.getValue();
                int diasLaborables = 0;

                while (!fechaActual.isAfter(fechaFinal)) {
                    boolean esDomingo = fechaActual.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
                    boolean esFeriado = fechasFeriadas.contains(fechaActual);

                    if (!esDomingo && !esFeriado) {
                        diasLaborables++;
                    }
                    fechaActual = fechaActual.plusDays(1);
                }

                if (diasLaborables > diasDisponibles) {
                    Notification.show("Saldo insuficiente. El empleado ya tomó " + diasYaTomados +
                                            " días este año. Solo le quedan " + diasDisponibles + " días disponibles.",
                                    5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    fin.clear();
                    numDiasDescanso.clear();
                    numDiasAPagar.clear();
                    return;
                }

                numDiasDescanso.setValue(diasLaborables);

                double proporcionDescanso = (double) diasLaborables / limiteDescanso;

                if (aniosAntiguedad > aniosSenior) {
                    int pagoProporcional = (int) Math.round(proporcionDescanso * pagoSenior);
                    numDiasAPagar.setValue(pagoProporcional);

                } else if (aniosAntiguedad >= 1) {
                    int pagoProporcional = (int) Math.round(proporcionDescanso * pagoBasico);
                    numDiasAPagar.setValue(pagoProporcional);

                } else {
                    numDiasAPagar.setValue(0);
                    Notification.show("El empleado tiene menos de 1 año.", 4000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                }

            } else {
                numDiasDescanso.clear();
                numDiasAPagar.clear();
            }
        }
    }


    private void registrarAprobador(VacacionEmpleado vacacion, EmpleadoService empleadoService) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            Empleado empleadoAprobador = empleadoService.findByUsuarioUsername(username);
            if (empleadoAprobador != null) {
                vacacion.setAprobadoPor(empleadoAprobador);
            }
        }
    }

    private void actualizarFiltroGrid(
            GridCrud<VacacionEmpleado> crud,
            CrudGridPaginator<VacacionEmpleado> paginator,
            String busqueda,
            VacacionEmpleadoService service
    ) {
        String filtroTexto = busqueda == null ? "" : busqueda.toLowerCase().trim();

        paginator.setSource(() ->
                service.findAll().stream()
                        .filter(v -> v.getEmpleado().getPersona().getNombre().toLowerCase().contains(filtroTexto))
                        .toList()
        );
        crud.setFindAllOperation(paginator::pageItems);
        paginator.reset();
    }

    private void mostrarError(Exception error) {
        Throwable causa = error;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }

        String mensaje = causa.getMessage() != null && !causa.getMessage().isBlank()
                ? causa.getMessage()
                : "Ocurrió un error al procesar la vacación.";

        Notification notification = Notification.show(mensaje, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
