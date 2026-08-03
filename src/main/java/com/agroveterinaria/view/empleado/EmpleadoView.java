package com.agroveterinaria.view.empleado;

import com.agroveterinaria.component.CrudGridPaginator;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.security.SecurityService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.NominaService;
import com.agroveterinaria.service.PersonaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class EmpleadoView extends VerticalLayout {
    private static final String CEDULA_PATTERN = "\\d{3}-\\d{7}-\\d{1}";

    public EmpleadoView(EmpleadoService empleadoService, PersonaService personaService, NominaService nominaService, SecurityService securityService) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);

        Set<Long> empleadosConHistorial = nominaService.getIdsEmpleadosConHistorial();

        GridCrud<Empleado> crudEmpleado = new GridCrud<>(Empleado.class, new WindowBasedCrudLayout());
        crudEmpleado.getGrid().addClassName("usuario-grid");
        crudEmpleado.getStyle().set("margin-top", "0");
        CrudGridPaginator<Empleado> paginator = new CrudGridPaginator<>(10, "empleados");
        paginator.setRefreshOperation(crudEmpleado::refreshGrid);

        crudEmpleado.getGrid().removeAllColumns();

        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getCedula() : "").setHeader("Cédula");
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getNombre() + " " + e.getPersona().getApellido() : "").setHeader("Nombre").setSortable(true);
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getTelefono() : "").setHeader("Teléfono");
        crudEmpleado.getGrid().addColumn(Empleado::getFechaIngreso).setHeader("Fecha de Ingreso").setSortable(true);
        crudEmpleado.getGrid().addColumn(e -> formatearSueldo(e.getSalario())).setHeader("Salario");

        crudEmpleado.getGrid().addComponentColumn(empleado -> {
            if (empleado.getCargos() == null || empleado.getCargos().isEmpty()) {
                return new Span("Sin roles");
            }

            HorizontalLayout layoutBadges = new HorizontalLayout();
            layoutBadges.addClassName("roles-container");
            layoutBadges.setPadding(false);
            layoutBadges.setMargin(false);

            for (RolEmpleado rol : empleado.getCargos()) {
                Span badge = new Span(rol.getDescripcion());

                badge.getElement().getThemeList().add("badge");
                badge.addClassName("rol-badge");

                layoutBadges.add(badge);
            }

            return layoutBadges;
        }).setHeader("Roles").setFlexGrow(2);

        crudEmpleado.getGrid().addComponentColumn(empleado -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> {
                crudEmpleado.getGrid().select(empleado);
                crudEmpleado.getUpdateButton().click();
            });

            Button btnEstado = new Button();
            btnEstado.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            if (empleado.getStatus() == StatusEntidad.ACTIVO) {
                btnEstado.setIcon(new Icon(VaadinIcon.POWER_OFF));
                btnEstado.addThemeVariants(ButtonVariant.LUMO_ERROR);
                btnEstado.getElement().setProperty("title", "Dar de baja (Desactivar)");
                btnEstado.addClickListener(e -> dialogBaja(empleado, crudEmpleado,empleadoService));
            } else {
                btnEstado.setIcon(new Icon(VaadinIcon.ARROW_CIRCLE_UP));
                btnEstado.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                btnEstado.getElement().setProperty("title", "Reactivar empleado");
                btnEstado.addClickListener(e -> {
                    empleadoService.reactivarEmpleado(empleado);
                    Notification.show("Empleado reactivado exitosamente", 4000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    crudEmpleado.refreshGrid();
                });
            }

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addClassName("btn-accion-eliminar");
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            boolean tieneHistorial = empleadosConHistorial.contains(empleado.getIdEmpleado());

            if (tieneHistorial) {
                btnEliminar.setEnabled(false);
                btnEliminar.getElement().setProperty("title", "No se puede eliminar: ya tiene nóminas registradas. Use 'Dar de baja'.");
            } else {
                btnEliminar.setEnabled(true);
                btnEliminar.getElement().setProperty("title", "Eliminar permanentemente");
            }

            btnEliminar.addClickListener(e -> {
                crudEmpleado.getGrid().select(empleado);
                crudEmpleado.getDeleteButton().click();
            });

            Empleado actual = securityService.obtenerEmpleadoAutenticado();
            boolean esUsuarioActual = actual != null &&
                    actual.getUsuario() != null &&
                    empleado.getUsuario() != null &&
                    empleado.getUsuario().getUsername().equals(actual.getUsuario().getUsername());

            if (esUsuarioActual) {
                btnEditar.setEnabled(false);
                btnEstado.setEnabled(false);
                btnEliminar.setEnabled(false);
            }

            HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEstado, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);

        crudEmpleado.getGrid().addThemeNames("row-stripes");

        crudEmpleado.getAddButton().setVisible(false);
        crudEmpleado.getUpdateButton().setVisible(false);
        crudEmpleado.getDeleteButton().setVisible(false);
        crudEmpleado.getFindAllButton().setVisible(false);

        Button btnNuevo = new Button("Nuevo empleado", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> crudEmpleado.getAddButton().setVisible(true));

        btnNuevo.addClickListener(e -> {
            crudEmpleado.getAddButton().setVisible(true);
            crudEmpleado.getAddButton().click();
            crudEmpleado.getAddButton().setVisible(false);
        });

        TextField buscarEmpleado = new TextField();
        buscarEmpleado.setWidthFull();
        buscarEmpleado.setPlaceholder("Buscar empleado...");
        buscarEmpleado.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarEmpleado.setValueChangeMode(ValueChangeMode.LAZY);

        Checkbox chkMostrarInactivos = new Checkbox("Mostrar inactivos");

        buscarEmpleado.addValueChangeListener(
                e -> actualizarFiltroGrid(crudEmpleado, paginator, buscarEmpleado.getValue(), chkMostrarInactivos.getValue(), empleadoService)
        );
        chkMostrarInactivos.addValueChangeListener(e -> actualizarFiltroGrid(crudEmpleado, paginator, buscarEmpleado.getValue(), e.getValue(), empleadoService));
        chkMostrarInactivos.getStyle().set("white-space", "nowrap");
        chkMostrarInactivos.getStyle().set("flex-shrink", "0");

        actualizarFiltroGrid(crudEmpleado, paginator, "", false, empleadoService);

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, buscarEmpleado, chkMostrarInactivos);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.addClassName("empleado-toolbar");
        toolbar.getStyle().set("margin-bottom", "0");

        DefaultCrudFormFactory<Empleado> formFactory = (DefaultCrudFormFactory<Empleado>) crudEmpleado.getCrudFormFactory();
        formFactory.setUseBeanValidation(true);

        formFactory.setVisibleProperties(
                "persona.cedula",
                "persona.nombre",
                "persona.apellido",
                "persona.telefono",
                "persona.direccion",
                "fechaIngreso",
                "salario",
                "cargos"
        );

        formFactory.setFieldCaptions(
                "Cédula",
                "Nombre",
                "Apellidos",
                "Teléfono",
                "Dirección",
                "Fecha de ingreso",
                "Salario",
                "Cargos"
        );

        formFactory.setFieldProvider("persona.telefono", empleado -> crearCampoTelefono());
        formFactory.setFieldProvider("persona.direccion", empleado -> {
            TextField direccionField = new TextField("Dirección");
            direccionField.setClearButtonVisible(true);
            return direccionField;
        });
        formFactory.setFieldProvider("salario", empleado -> crearCampoSalario());

        formFactory.setFieldProvider("persona.nombre", empleado -> {
            TextField nombreField = new TextField("Nombre");
            nombreField.setAllowedCharPattern("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]");
            nombreField.setClearButtonVisible(true);
            return nombreField;
        });

        formFactory.setFieldProvider("persona.apellido", empleado -> {
            TextField apellidoField = new TextField("Apellidos");
            apellidoField.setAllowedCharPattern("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]");
            apellidoField.setClearButtonVisible(true);
            return apellidoField;
        });

        formFactory.setFieldProvider("persona.cedula", obj -> {
            Empleado empleado = (Empleado) obj;

            TextField cedulaField = crearCampoCedula();

            if (empleado.getIdEmpleado() == null) {
                cedulaField.addValueChangeListener(event -> {
                    if (event.isFromClient()) {
                        String cedula = event.getValue();

                        if (cedula != null && cedula.matches(CEDULA_PATTERN)) {
                            Optional<Persona> persona = personaService.findByCedula(cedula);

                            cedulaField.getParent().ifPresent(parent -> {
                                parent.getChildren().forEach(component -> {
                                    if (component instanceof TextField txt) {
                                        if ("Nombre".equals(txt.getLabel())) {
                                            txt.setValue(persona.isPresent() ? persona.get().getNombre() : "");
                                            txt.setReadOnly(persona.isPresent());
                                        } else if ("Apellidos".equals(txt.getLabel())) {
                                            txt.setValue(persona.isPresent() && persona.get().getApellido() != null ? persona.get().getApellido() : "");
                                            txt.setReadOnly(persona.isPresent());
                                        } else if ("Teléfono".equals(txt.getLabel())) {
                                            txt.setValue(persona.isPresent() ? persona.get().getTelefono() : "");
                                            txt.setReadOnly(persona.isPresent());
                                        } else if ("Dirección".equals(txt.getLabel())) {
                                            txt.setValue(persona.isPresent() ? persona.get().getDireccion() : "");
                                            txt.setReadOnly(persona.isPresent());
                                        }
                                    }
                                });
                            });
                        } else {
                            cedulaField.getParent().ifPresent(parent -> {
                                parent.getChildren().forEach(component -> {
                                    if (component instanceof TextField txt) {
                                        if ("Nombre".equals(txt.getLabel()) || "Apellidos".equals(txt.getLabel()) ||
                                                "Teléfono".equals(txt.getLabel()) || "Dirección".equals(txt.getLabel())) {
                                            txt.clear();
                                            txt.setReadOnly(false);
                                        }
                                    }
                                });
                            });
                        }
                    }
                });
            } else {
                cedulaField.setReadOnly(true);
            }

            return cedulaField;
        });

        formFactory.setFieldProvider("fechaIngreso", empleado -> {
            DatePicker fechaIngresoField = new DatePicker("Fecha de ingreso");
            fechaIngresoField.setClearButtonVisible(true);
            fechaIngresoField.setWidthFull();
            return fechaIngresoField;
        });

        formFactory.setFieldProvider("cargos", empleado -> {
            MultiSelectComboBox<RolEmpleado> combo = new MultiSelectComboBox<>("Roles");
            combo.setItems(RolEmpleado.values());
            combo.setItemLabelGenerator(RolEmpleado::getDescripcion);
            combo.setWidthFull();
            combo.setClearButtonVisible(true);

            combo.addValueChangeListener(event -> {
                Set<RolEmpleado> seleccionados = event.getValue();
                if (seleccionados != null && seleccionados.contains(RolEmpleado.ADMINISTRADOR) && seleccionados.size() > 1) {
                    combo.setValue(Set.of(RolEmpleado.ADMINISTRADOR));

                    Notification.show("El rol de Administrador tiene acceso total y no requiere roles adicionales.",
                                    3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
            });

            return combo;
        });

        formFactory.setFieldCreationListener("salario", campo -> {
            campo.setRequiredIndicatorVisible(false);
        });

        formFactory.setFieldCreationListener("cargos", campo -> {
            campo.setRequiredIndicatorVisible(false);
        });
        formFactory.setErrorListener(this::mostrarError);


        formFactory.setNewInstanceSupplier(() -> {
            Empleado nuevoEmpleado = new Empleado();
            nuevoEmpleado.setPersona(new Persona());
            nuevoEmpleado.setCargos(new HashSet<>());

            return nuevoEmpleado;
        });

        formFactory.setButtonCaption(CrudOperation.ADD,    "Crear");
        formFactory.setButtonCaption(CrudOperation.UPDATE, "Guardar cambios");
        formFactory.setButtonCaption(CrudOperation.DELETE, "Sí, eliminar");
        formFactory.setCancelButtonCaption("Cancelar");

        formFactory.setCaption(CrudOperation.ADD,    "Crear Empleado");
        formFactory.setCaption(CrudOperation.UPDATE, "Editar Empleado");
        formFactory.setCaption(CrudOperation.DELETE, "¿Eliminar Empleado?");

        crudEmpleado.setAddOperation(empleado -> {
            try {
                return empleadoService.save(empleado);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        });
        crudEmpleado.setUpdateOperation(empleadoService::update);

        crudEmpleado.setDeleteOperation(empleado -> {
            try {
                empleadoService.delete(empleado);
            } catch (IllegalStateException ex) {
                throw new RuntimeException(ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                throw new RuntimeException("El empleado tiene registros asociados (Préstamos, Vacaciones, etc.). Utilice el botón 'Dar de Baja'.");
            } catch (Exception ex) {
                throw new RuntimeException("Ocurrió un error inesperado al intentar eliminar el empleado.");
            }
        });

        add(toolbar, paginator, crudEmpleado);
    }

    private void dialogBaja(Empleado empleado, GridCrud<Empleado> crudEmpleado, EmpleadoService empleadoService) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.addClassName("dialog-baja-empleado");
        dialog.setHeader("Dar de baja al empleado");
        dialog.setText("¿Está seguro que desea dar de baja a " + empleado.getPersona().getNombre() + " " + empleado.getPersona().getApellido() + "? Se le cortará el acceso al sistema inmediatamente.");
        dialog.setConfirmText("Sí, dar de baja");
        dialog.setConfirmButtonTheme("error tonal");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setCancelButtonTheme("outlined");
        dialog.addConfirmListener(event -> {

            try {
                empleadoService.darDeBaja(empleado);
                Notification.show("Empleado inhabilitado correctamente", 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                crudEmpleado.refreshGrid();

            } catch (IllegalStateException | IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                Notification.show("Ocurrió un error inesperado al intentar dar de baja al empleado.", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private TextField crearCampoCedula() {
        TextField cedula = new TextField("Cédula");
        cedula.setPlaceholder("000-0000000-0");
        cedula.setAllowedCharPattern("[0-9-]");
        cedula.setMaxLength(13);
        cedula.setClearButtonVisible(true);
        cedula.setValueChangeMode(ValueChangeMode.ON_BLUR);

        return cedula;
    }

    private TextField crearCampoTelefono() {
        TextField telefono = new TextField("Teléfono");
        telefono.setPlaceholder("000-000-0000");
        telefono.setMaxLength(12);
        telefono.setAllowedCharPattern("[0-9-]");
        telefono.setRequiredIndicatorVisible(false);
        telefono.setClearButtonVisible(true);
        telefono.setValueChangeMode(ValueChangeMode.ON_BLUR);

        return telefono;
    }

    private BigDecimalField crearCampoSalario() {
        BigDecimalField salario = new com.vaadin.flow.component.textfield.BigDecimalField("Salario");
        salario.setPlaceholder("0.00");
        salario.setPrefixComponent(new Span("RD$"));
        salario.setClearButtonVisible(true);

        return salario;
    }

    private void mostrarError(Exception error) {
        Throwable causa = error;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }

        String mensaje = causa.getMessage() != null && !causa.getMessage().isBlank()
                ? causa.getMessage()
                : "No se pudo guardar el empleado.";

        Notification notification = Notification.show(mensaje, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void actualizarFiltroGrid(
            GridCrud<Empleado> crud,
            CrudGridPaginator<Empleado> paginator,
            String busqueda,
            boolean mostrarInactivos,
            EmpleadoService empleadoService
    ) {
        String filtroTexto = busqueda == null ? "" : busqueda.toLowerCase().trim();

        paginator.setSource(() ->
                empleadoService.findAll().stream()
                        .filter(emp -> mostrarInactivos || emp.getStatus() == StatusEntidad.ACTIVO)
                        .filter(emp -> {
                            if (emp.getPersona() == null) return false;

                            String nombre = emp.getPersona().getNombre() != null ? emp.getPersona().getNombre().toLowerCase() : "";
                            String apellido = emp.getPersona().getApellido() != null ? emp.getPersona().getApellido().toLowerCase() : "";

                            return nombre.contains(filtroTexto) || apellido.contains(filtroTexto);
                        })
                        .toList()
        );
        crud.setFindAllOperation(paginator::pageItems);
        paginator.reset();
    }

    private String formatearSueldo(BigDecimal sueldo) {
        if (sueldo == null) return "RD$ 0.00";
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "DO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "RD$ " + formato.format(sueldo);
    }
}
