package com.agroveterinaria.view.proveedor;

import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.ProveedorService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@CssImport(value = "./sorter-styles.css", themeFor = "vaadin-grid-sorter")
@Route("proveedores")
@PageTitle("Gestión de Proveedores | Agroveterinaria")
public class ProveedorView extends VerticalLayout {

    public ProveedorView(ProveedorService proveedorService) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);

        WindowBasedCrudLayout crudLayout = new WindowBasedCrudLayout();
        crudLayout.setFormWindowWidth("560px");
        GridCrud<Proveedor> crud = new GridCrud<>(Proveedor.class, crudLayout);
        crud.addClassName("proveedor-crud");
        crud.getGrid().addClassName("usuario-grid");

        configurarGrid(crud);
        configurarFormulario(crud);

        Button btnNuevo = new Button("Nuevo Proveedor", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(event -> crud.getAddButton().click());

        TextField searchField = new TextField();
        searchField.setWidthFull();
        searchField.setPlaceholder("Buscar proveedor...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(event -> crud.refreshGrid());

        ComboBox<StatusEntidad> statusFilter = new ComboBox<>();
        statusFilter.setPlaceholder("Todos los estados");
        statusFilter.setItems(StatusEntidad.values());
        statusFilter.setItemLabelGenerator(StatusEntidad::getEtiqueta);
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(event -> crud.refreshGrid());

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, searchField, statusFilter);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.addClassName("producto-toolbar");
        toolbar.expand(searchField);

        crud.setFindAllOperation(() -> {
            String termino = searchField.getValue() != null ? searchField.getValue().toLowerCase().trim() : "";
            StatusEntidad estadoSeleccionado = statusFilter.getValue();

            return proveedorService.listarTodos().stream()
                    .filter(proveedor -> estadoSeleccionado == null || proveedor.getStatus() == estadoSeleccionado)
                    .filter(proveedor -> termino.isBlank()
                            || contiene(proveedor.getNombre(), termino)
                            || contiene(proveedor.getRnc(), termino)
                            || contiene(proveedor.getTelefono(), termino)
                            || contiene(proveedor.getDireccion(), termino)
                            || contiene(proveedor.getNumPersonaContacto(), termino)
                            || contiene(etiquetaStatus(proveedor.getStatus()), termino))
                    .toList();
        });

        crud.setAddOperation(proveedorService::guardar);
        crud.setUpdateOperation(proveedorService::guardar);
        crud.setDeleteOperation(proveedor -> proveedorService.eliminarPorId(proveedor.getIdProveedor()));
        crud.setSizeFull();
        add(toolbar, crud);
    }

    private void configurarGrid(GridCrud<Proveedor> crud) {
        crud.getGrid().removeAllColumns();

        crud.getGrid().addColumn(Proveedor::getRnc)
                .setHeader("RNC")
                .setKey("rnc")
                .setWidth("140px")
                .setFlexGrow(0)
                .setComparator(proveedor -> valor(proveedor.getRnc()));

        crud.getGrid().addColumn(Proveedor::getNombre)
                .setHeader("Proveedor")
                .setKey("nombre")
                .setFlexGrow(2)
                .setComparator(proveedor -> valor(proveedor.getNombre()));

        crud.getGrid().addColumn(Proveedor::getTelefono)
                .setHeader("Teléfono")
                .setKey("telefono")
                .setWidth("150px")
                .setFlexGrow(0)
                .setComparator(proveedor -> valor(proveedor.getTelefono()));

        crud.getGrid().addColumn(Proveedor::getDireccion)
                .setHeader("Dirección")
                .setKey("direccion")
                .setFlexGrow(2)
                .setComparator(proveedor -> valor(proveedor.getDireccion()));

        crud.getGrid().addColumn(Proveedor::getNumPersonaContacto)
                .setHeader("Contacto")
                .setKey("numPersonaContacto")
                .setWidth("150px")
                .setFlexGrow(0)
                .setComparator(proveedor -> valor(proveedor.getNumPersonaContacto()));

        crud.getGrid().addComponentColumn(proveedor -> {
            boolean activo = proveedor.getStatus() == StatusEntidad.ACTIVO;
            Span badge = new Span(etiquetaStatus(proveedor.getStatus()));
            badge.getElement().getThemeList().add("badge " + (activo ? "success" : "error"));
            return badge;
        }).setHeader("Estado").setKey("status");

        crud.getGrid().addComponentColumn(proveedor -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.setAriaLabel("Editar proveedor");
            btnEditar.addClickListener(event -> {
                crud.getGrid().select(proveedor);
                crud.getUpdateButton().click();
            });

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addClassName("btn-accion-eliminar");
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEliminar.setAriaLabel("Eliminar proveedor");
            btnEliminar.addClickListener(event -> {
                crud.getGrid().select(proveedor);
                crud.getDeleteButton().click();
            });

            HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);

        crud.getGrid().addThemeNames("row-stripes");
        crud.getAddButton().setVisible(false);
        crud.getUpdateButton().setVisible(false);
        crud.getDeleteButton().setVisible(false);
        crud.getFindAllButton().setVisible(false);
    }

    private void configurarFormulario(GridCrud<Proveedor> crud) {
        DefaultCrudFormFactory<Proveedor> formFactory = new DefaultCrudFormFactory<>(Proveedor.class);
        formFactory.setUseBeanValidation(true);
        formFactory.setVisibleProperties("rnc", "nombre", "direccion", "telefono", "numPersonaContacto", "status");
        formFactory.setFieldCaptions("RNC", "Nombre del proveedor", "Dirección", "Teléfono", "Número de contacto", "Estado");

        formFactory.setFieldCreationListener("rnc", field -> {
            TextField rncField = (TextField) field;
            rncField.setPlaceholder("Ej: 101234567");
            rncField.setClearButtonVisible(true);
        });

        formFactory.setFieldCreationListener("nombre", field -> {
            TextField nombreField = (TextField) field;
            nombreField.setPlaceholder("Ej: Distribuidora Agrovet");
            nombreField.setClearButtonVisible(true);
            nombreField.getElement().setAttribute("colspan", "2");
        });

        formFactory.setFieldCreationListener("direccion", field -> {
            TextField direccionField = (TextField) field;
            direccionField.setPlaceholder("Dirección comercial");
            direccionField.setClearButtonVisible(true);
            direccionField.getElement().setAttribute("colspan", "2");
        });

        formFactory.setFieldCreationListener("telefono", field -> {
            TextField telefonoField = (TextField) field;
            telefonoField.setPlaceholder("000-000-0000");
            telefonoField.setClearButtonVisible(true);
        });

        formFactory.setFieldCreationListener("numPersonaContacto", field -> {
            TextField contactoField = (TextField) field;
            contactoField.setPlaceholder("000-000-0000");
            contactoField.setClearButtonVisible(true);
        });

        formFactory.setFieldProvider("status", proveedor -> {
            ComboBox<StatusEntidad> statusComboBox = new ComboBox<>();
            statusComboBox.setItems(StatusEntidad.values());
            statusComboBox.setItemLabelGenerator(StatusEntidad::getEtiqueta);
            Proveedor proveedorActual = (Proveedor) proveedor;
            statusComboBox.setValue(proveedorActual.getStatus() != null ? proveedorActual.getStatus() : StatusEntidad.ACTIVO);
            statusComboBox.getElement().setAttribute("colspan", "2");
            return statusComboBox;
        });

        formFactory.setErrorListener(error -> {
            Throwable causa = error;
            while (causa.getCause() != null) {
                causa = causa.getCause();
            }

            String mensaje = causa.getMessage() != null && !causa.getMessage().isBlank()
                    ? causa.getMessage()
                    : "No se pudo guardar el proveedor.";

            Notification notification = Notification.show(mensaje, 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        formFactory.setCaption(CrudOperation.ADD, "Registrar nuevo proveedor");
        formFactory.setCaption(CrudOperation.UPDATE, "Editar proveedor");
        formFactory.setCaption(CrudOperation.DELETE, "¿Eliminar Proveedor?");
        formFactory.setButtonCaption(CrudOperation.DELETE, "Sí, eliminar");
        formFactory.setCancelButtonCaption("Cancelar");

        crud.setCrudFormFactory(formFactory);
    }

    private String etiquetaStatus(StatusEntidad status) {
        return status != null ? status.getEtiqueta() : "";
    }

    private boolean contiene(String valor, String termino) {
        return valor != null && valor.toLowerCase().contains(termino);
    }

    private String valor(String valor) {
        return valor != null ? valor : "";
    }
}
