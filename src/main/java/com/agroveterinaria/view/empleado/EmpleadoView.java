package com.agroveterinaria.view.empleado;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.EmpleadoService;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.CrudFormFactory;

import java.util.HashSet;

@Route("empleados")
public class EmpleadoView extends VerticalLayout {

    private static final String CEDULA_PATTERN = "\\d{3}-\\d{7}-\\d{1}";
    private static final String TELEFONO_PATTERN = "\\d{3}-\\d{3}-\\d{4}";

    public EmpleadoView(EmpleadoService empleadoService) {
        GridCrud<Empleado> crudEmpleado = new GridCrud<>(Empleado.class);

        crudEmpleado.getGrid().removeAllColumns();

        crudEmpleado.getGrid().addColumn(Empleado::getIdEmpleado).setHeader("ID").setSortable(true);
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getCedula() : "").setHeader("Cédula");
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getNombre() : "").setHeader("Nombre");
        crudEmpleado.getGrid().addColumn(e -> e.getPersona() != null ? e.getPersona().getTelefono() : "").setHeader("Teléfono");
        crudEmpleado.getGrid().addColumn(Empleado::getSalario).setHeader("Salario");

        CrudFormFactory<Empleado> formFactory = crudEmpleado.getCrudFormFactory();

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

        formFactory.setFieldProvider("persona.cedula", empleado -> crearCampoCedula());
        formFactory.setFieldProvider("persona.telefono", empleado -> crearCampoTelefono());
        formFactory.setFieldProvider("cargos", empleado -> {
            CheckboxGroup<RolEmpleado> chkRoles = new CheckboxGroup<>("Roles asignados");
            chkRoles.setItems(RolEmpleado.values());
            chkRoles.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
            return chkRoles;
        });
        formFactory.setErrorListener(error -> mostrarError(error));


        formFactory.setNewInstanceSupplier(() -> {
            Empleado nuevoEmpleado = new Empleado();
            nuevoEmpleado.setPersona(new Persona());
            nuevoEmpleado.setCargos(new HashSet<>());

            return nuevoEmpleado;
        });

        crudEmpleado.setFindAllOperation(empleadoService::findAll);
        crudEmpleado.setAddOperation(empleadoService::save);
        crudEmpleado.setUpdateOperation(empleadoService::update);
        crudEmpleado.setDeleteOperation(empleadoService::delete);

        add(crudEmpleado);
    }

    private TextField crearCampoCedula() {
        TextField cedula = new TextField("Cédula");
        cedula.setPlaceholder("000-0000000-0");
        cedula.setHelperText("Formato: 000-0000000-0");
        cedula.setPattern(CEDULA_PATTERN);
        cedula.setErrorMessage("Usa el formato 000-0000000-0");
        cedula.setRequiredIndicatorVisible(true);
        cedula.setClearButtonVisible(true);
        return cedula;
    }

    private TextField crearCampoTelefono() {
        TextField telefono = new TextField("Teléfono");
        telefono.setPlaceholder("000-000-0000");
        telefono.setHelperText("Formato: 000-000-0000");
        telefono.setPattern(TELEFONO_PATTERN);
        telefono.setErrorMessage("Usa el formato 000-000-0000");
        telefono.setRequiredIndicatorVisible(true);
        telefono.setClearButtonVisible(true);
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
