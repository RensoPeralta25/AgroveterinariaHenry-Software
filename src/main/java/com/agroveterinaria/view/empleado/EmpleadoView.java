package com.agroveterinaria.view.empleado;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.NominaService;
import com.agroveterinaria.service.PersonaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class EmpleadoView extends VerticalLayout {
    private static final String CEDULA_PATTERN = "\\d{3}-\\d{7}-\\d{1}";

    public EmpleadoView(EmpleadoService empleadoService, PersonaService personaService, NominaService nominaService) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);

        Set<Long> empleadosConHistorial = nominaService.getIdsEmpleadosConHistorial();

        GridCrud<Empleado> crudEmpleado = new GridCrud<>(Empleado.class, new WindowBasedCrudLayout());
        crudEmpleado.getGrid().addClassName("usuario-grid");
        crudEmpleado.getStyle().set("margin-top", "0");

        crudEmpleado.getGrid().removeAllColumns();

        crudEmpleado.getGrid().addColumn(Empleado::getIdEmpleado).setHeader("ID").setSortable(true);
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getCedula() : "").setHeader("Cédula");
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getNombre() : "").setHeader("Nombre");
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getTelefono() : "").setHeader("Teléfono");
        crudEmpleado.getGrid().addColumn(Empleado::getSalario).setHeader("Salario");

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

            if (empleado.isActivo()) {
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
        buscarEmpleado.setPlaceholder("Buscar empleado...");
        buscarEmpleado.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarEmpleado.setValueChangeMode(ValueChangeMode.LAZY);

        Checkbox chkMostrarInactivos = new Checkbox("Mostrar inactivos");

        buscarEmpleado.addValueChangeListener(
                e -> actualizarFiltroGrid(crudEmpleado, buscarEmpleado.getValue(), chkMostrarInactivos.getValue(), empleadoService)
        );
        chkMostrarInactivos.addValueChangeListener(e -> actualizarFiltroGrid(crudEmpleado, buscarEmpleado.getValue(), e.getValue(),empleadoService));

        actualizarFiltroGrid(crudEmpleado, "", false, empleadoService);

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
                "persona.telefono",
                "persona.direccion",
                "salario",
                "cargos"
        );

        formFactory.setFieldCaptions(
                "Cédula",
                "Nombre",
                "Teléfono",
                "Dirección",
                "Salario",
                "Cargos"
        );

        TextField cedulaField = crearCampoCedula();
        TextField telefonoField = crearCampoTelefono();

        TextField nombreField = new TextField("Nombre");
        nombreField.setAllowedCharPattern("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]");
        nombreField.setClearButtonVisible(true);

        TextField direccionField = new TextField("Dirección");
        direccionField.setClearButtonVisible(true);

        cedulaField.setValueChangeMode(ValueChangeMode.ON_BLUR);
        cedulaField.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                String cedula = event.getValue();

                if (cedula != null && cedula.matches(CEDULA_PATTERN)) {
                    Optional<Persona> persona = personaService.findByCedula(cedula);

                    if (persona.isPresent()) {
                        Persona p = persona.get();

                        nombreField.setValue(p.getNombre());
                        telefonoField.setValue(p.getTelefono());
                        direccionField.setValue(p.getDireccion());

                        nombreField.setReadOnly(true);
                        telefonoField.setReadOnly(true);
                        direccionField.setReadOnly(true);

                    } else {
                        reiniciarCampos(nombreField, telefonoField, direccionField);
                    }
                } else {
                    reiniciarCampos(nombreField, telefonoField, direccionField);
                }
            }
        });

        formFactory.setFieldProvider("persona.cedula", empleado -> cedulaField);
        formFactory.setFieldProvider("persona.telefono", empleado -> telefonoField);
        formFactory.setFieldProvider("persona.nombre", empleado -> nombreField);
        formFactory.setFieldProvider("persona.direccion", empleado -> direccionField);
        formFactory.setFieldProvider("salario", empleado -> crearCampoSalario());


        formFactory.setFieldProvider("cargos", empleado -> {
            MultiSelectComboBox<RolEmpleado> combo = new MultiSelectComboBox<>("Roles");
            combo.setItems(RolEmpleado.values());
            combo.setItemLabelGenerator(rol ->
                    rol.name().charAt(0) + rol.name().substring(1).toLowerCase()
            );
            combo.setWidthFull();
            combo.setClearButtonVisible(true);
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

        crudEmpleado.setFindAllOperation(() ->
                empleadoService.findAll().stream()
                        .filter(Empleado::isActivo)
                        .toList()
        );

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

        add(toolbar, crudEmpleado);
    }

    private void dialogBaja(Empleado empleado, GridCrud<Empleado> crudEmpleado, EmpleadoService empleadoService) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.addClassName("dialog-baja-empleado");
        dialog.setHeader("Dar de baja al empleado");
        dialog.setText("¿Está seguro que desea dar de baja a " + empleado.getPersona().getNombre() + "? Se le cortará el acceso al sistema inmediatamente.");
        dialog.setConfirmText("Sí, dar de baja");
        dialog.setConfirmButtonTheme("error tonal");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setCancelButtonTheme("outlined");
        dialog.addConfirmListener(event -> {

            empleadoService.darDeBaja(empleado);
            Notification.show("Empleado inhabilitado correctamente", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            crudEmpleado.refreshGrid();
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

    private void reiniciarCampos(TextField nombre, TextField telefono, TextField direccion) {
        if (nombre.isReadOnly() || telefono.isReadOnly() || direccion.isReadOnly()) {
            nombre.clear();
            nombre.setReadOnly(false);

            telefono.clear();
            telefono.setReadOnly(false);

            direccion.clear();
            direccion.setReadOnly(false);
        }
    }

    private void actualizarFiltroGrid(GridCrud<Empleado> crud, String busqueda, boolean mostrarInactivos, EmpleadoService empleadoService) {
        String filtroTexto = busqueda == null ? "" : busqueda.toLowerCase().trim();

        crud.setFindAllOperation(() ->
                empleadoService.findAll().stream()
                        .filter(emp -> mostrarInactivos || emp.isActivo())
                        .filter(emp -> emp.getPersona() != null &&
                                emp.getPersona().getNombre().toLowerCase().contains(filtroTexto))
                        .toList()
        );
        crud.refreshGrid();
    }
}
