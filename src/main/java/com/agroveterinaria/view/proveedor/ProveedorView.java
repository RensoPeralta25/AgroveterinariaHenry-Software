package com.agroveterinaria.view.proveedor;

import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.repository.ProveedorRepository;
import com.agroveterinaria.service.ProveedorService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.impl.GridCrud;
import tools.jackson.databind.type.IterationType;

@Route("proveedores")
@PageTitle("Gestión de Proveedores | Agroveterinaria")
public class ProveedorView extends VerticalLayout {

    public ProveedorView(ProveedorService proveedorService) {

        GridCrud<Proveedor> crud = new GridCrud<>(Proveedor.class);

        crud.setFindAllOperation(proveedorService::listarTodos);
        crud.setAddOperation(proveedorService::guardar);
        crud.setUpdateOperation(proveedorService::guardar);
        crud.setDeleteOperation(proveedorService::guardar);

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

        TextField searchField = new TextField();
        searchField.setPlaceholder("Buscar proveedor...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> crud.refreshGrid());
        ComboBox<StatusEntidad> isActivoInactivo = new ComboBox<>();
        isActivoInactivo.setPlaceholder("Filtrar por Estado");
        isActivoInactivo.setItems(StatusEntidad.ACTIVO, StatusEntidad.INACTIVO);
        isActivoInactivo.setAllowCustomValue(false);
        isActivoInactivo.setAutoOpen(true);
        isActivoInactivo.setClearButtonVisible(true);
        isActivoInactivo.addValueChangeListener(event -> crud.refreshGrid());
        searchField.setWidth("250px");
        isActivoInactivo.setWidth("220px");
        crud.getCrudLayout().addFilterComponents(searchField, isActivoInactivo);

        crud.setFindAllOperation(() -> {
            String termino = searchField.getValue().toLowerCase().trim();
            StatusEntidad estadoSeleccionado = isActivoInactivo.getValue();

            return proveedorService.listarTodos().stream()
                    .filter(proveedor -> {
                        boolean coincideNombre = proveedor.getNombre() != null &&
                                proveedor.getNombre().toLowerCase().contains(termino);

                        boolean coincideRnc = proveedor.getRnc() != null &&
                                proveedor.getRnc().toLowerCase().contains(termino);

                        boolean coincideTelefono = proveedor.getTelefono() != null &&
                                proveedor.getTelefono().toLowerCase().contains(termino);

                        boolean coincideNumContacto = proveedor.getNumPersonaContacto() != null &&
                                proveedor.getNumPersonaContacto().toLowerCase().contains(termino);

                        boolean coincideTexto = termino.isEmpty()
                                || coincideNombre
                                || coincideRnc
                                || coincideTelefono
                                || coincideNumContacto;

                        boolean coincideEstado = estadoSeleccionado == null
                                || proveedor.getStatus() == estadoSeleccionado;

                        return coincideTexto && coincideEstado;
                    }).toList();
        });


        setSizeFull();
        crud.setSizeFull();

        add(crud);
    }
}