package com.agroveterinaria.view.proveedor;

import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.repository.ProveedorRepository;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.impl.GridCrud;

@Route("proveedores")
@PageTitle("Gestión de Proveedores | Agroveterinaria")
public class ProveedorView extends VerticalLayout {

    public ProveedorView(ProveedorRepository repository) {

        GridCrud<Proveedor> crud = new GridCrud<>(Proveedor.class);


        crud.setFindAllOperation(repository::findAll);
        crud.setAddOperation(repository::save);
        crud.setUpdateOperation(repository::save);
        crud.setDeleteOperation(repository::save);

        crud.getGrid().setColumns("rnc", "nombre", "telefono", "direccion", "numPersonaContacto", "status");

        crud.getGrid().getColumnByKey("rnc").setHeader("RNC");
        crud.getGrid().getColumnByKey("nombre").setHeader("Nombre Completo");
        crud.getGrid().getColumnByKey("telefono").setHeader("Teléfono");
        crud.getGrid().getColumnByKey("direccion").setHeader("Dirección");
        crud.getGrid().getColumnByKey("numPersonaContacto").setHeader("Contacto");
        crud.getGrid().getColumnByKey("status").setHeader("Estado");

        crud.getCrudFormFactory().setVisibleProperties(
                "rnc", "nombre", "direccion", "telefono", "numPersonaContacto", "status"
        );

        crud.getCrudFormFactory().setFieldCaptions(
                "RNC", "Nombre del Proveedor", "Dirección", "Teléfono", "Número de Contacto", "Estado"
        );

        crud.getCrudFormFactory().setFieldProvider("status", e -> {
            ComboBox<StatusEntidad> statusComboBox = new ComboBox<>();
            statusComboBox.setItems(StatusEntidad.values());
            return statusComboBox;
        });

        setSizeFull();
        crud.setSizeFull();

        add(crud);
    }
}