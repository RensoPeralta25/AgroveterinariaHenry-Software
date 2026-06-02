package com.agroveterinaria.view.empleado;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.service.EmpleadoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.CrudFormFactory;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.util.HashSet;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@Route("empleados")
public class EmpleadoView extends VerticalLayout {

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
        crudEmpleado.setAddOperation(empleadoService::save);
        crudEmpleado.setUpdateOperation(empleadoService::update);
        crudEmpleado.setDeleteOperation(empleadoService::delete);

        add(toolbar, crudEmpleado);
    }
}
