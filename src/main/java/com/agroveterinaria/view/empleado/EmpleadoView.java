package com.agroveterinaria.view.empleado;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.EmpleadoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.util.HashSet;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class EmpleadoView extends VerticalLayout {

    private static final String CEDULA_PATTERN = "\\d{3}-\\d{7}-\\d{1}";
    private static final String TELEFONO_PATTERN = "\\d{3}-\\d{3}-\\d{4}";

    public EmpleadoView(EmpleadoService empleadoService) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);

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

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addClassName("btn-accion-eliminar");
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEliminar.addClickListener(e -> {
                crudEmpleado.getGrid().select(empleado);
                crudEmpleado.getDeleteButton().click();
            });

            HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEliminar);
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
        buscarEmpleado.addValueChangeListener(e -> {
            String filtro = e.getValue().toLowerCase().trim();
            crudEmpleado.setFindAllOperation(() ->
                    empleadoService.findAll().stream()
                            .filter(emp -> emp.getPersona() != null &&
                                    emp.getPersona().getNombre()
                                            .toLowerCase().contains(filtro))
                            .toList()
            );
            crudEmpleado.refreshGrid();
        });

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, buscarEmpleado);
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

        formFactory.setFieldCreationListener("salario", campo -> {
            campo.setRequiredIndicatorVisible(false);
        });

        formFactory.setFieldCreationListener("cargos", campo -> {
            campo.setRequiredIndicatorVisible(false);
        });

        formFactory.setFieldProvider("persona.cedula", empleado -> crearCampoCedula());
        formFactory.setFieldProvider("persona.telefono", empleado -> crearCampoTelefono());
        formFactory.setFieldProvider("cargos", empleado -> {
            com.vaadin.flow.component.combobox.MultiSelectComboBox<RolEmpleado> combo =
                    new com.vaadin.flow.component.combobox.MultiSelectComboBox<>("Roles");
            combo.setItems(RolEmpleado.values());
            combo.setItemLabelGenerator(rol ->
                    rol.name().charAt(0) + rol.name().substring(1).toLowerCase()
            );
            combo.setWidthFull();
            return combo;
        });
        formFactory.setErrorListener(error -> mostrarError(error));


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

        crudEmpleado.setFindAllOperation(empleadoService::findAll);
        crudEmpleado.setAddOperation(empleado -> {
            try {
                return empleadoService.save(empleado);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        });
        crudEmpleado.setUpdateOperation(empleadoService::update);
        crudEmpleado.setDeleteOperation(empleadoService::delete);

        add(toolbar, crudEmpleado);
    }

    private TextField crearCampoCedula() {
        TextField cedula = new TextField("Cédula");
        cedula.setPlaceholder("000-0000000-0");
        cedula.setPattern(CEDULA_PATTERN);
        cedula.setErrorMessage("Usa el formato 000-0000000-0");
        return cedula;
    }

    private TextField crearCampoTelefono() {
        TextField telefono = new TextField("Teléfono");
        telefono.setPlaceholder("000-000-0000");
        telefono.setPattern(TELEFONO_PATTERN);
        telefono.setErrorMessage("Usa el formato 000-000-0000");
        return telefono;
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
}
