package com.agroveterinaria.view.empleado;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.EmpleadoService;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.CrudFormFactory;

import java.util.HashSet;

@Route("empleados")
public class EmpleadoView extends VerticalLayout {

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
                "cargos"
        );

        formFactory.setFieldProvider("cargos", empleado -> {
            CheckboxGroup<RolEmpleado> chkRoles = new CheckboxGroup<>("Roles asignados");
            chkRoles.setItems(RolEmpleado.values());
            chkRoles.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
            return chkRoles;
        });


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
}
