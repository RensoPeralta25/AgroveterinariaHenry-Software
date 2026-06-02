package com.agroveterinaria.view.mascota;

import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.Mascota;
import com.agroveterinaria.enums.TipoAnimal;
import com.agroveterinaria.service.ClienteService;
import com.agroveterinaria.service.MascotaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
@Route("mascotas")
@PageTitle("Gestión de Mascotas")
public class MascotaView extends VerticalLayout {

    public MascotaView(MascotaService mascotaService, ClienteService clienteService) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("mascota-view");

        WindowBasedCrudLayout crudLayout = new WindowBasedCrudLayout();
        crudLayout.setFormWindowWidth("560px");
        GridCrud<Mascota> crud = new GridCrud<>(Mascota.class, crudLayout);
        crud.addClassName("mascota-crud");
        crud.getGrid().addClassName("usuario-grid");

        configurarGrid(crud);
        configurarFormulario(crud, clienteService);

        Button btnNueva = new Button("Nueva Mascota", new Icon(VaadinIcon.PLUS));
        btnNueva.addClassName("btn-nuevo");
        btnNueva.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNueva.addClickListener(event -> crud.getAddButton().click());

        TextField buscar = new TextField();
        buscar.setPlaceholder("Buscar mascota...");
        buscar.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscar.setClearButtonVisible(true);
        buscar.setValueChangeMode(ValueChangeMode.LAZY);
        buscar.addValueChangeListener(event -> crud.refreshGrid());

        ComboBox<TipoAnimal> tipoFilter = new ComboBox<>();
        tipoFilter.setPlaceholder("Todos los tipos");
        tipoFilter.setItems(TipoAnimal.values());
        tipoFilter.setItemLabelGenerator(this::labelTipoAnimal);
        tipoFilter.setClearButtonVisible(true);
        tipoFilter.addValueChangeListener(event -> crud.refreshGrid());

        HorizontalLayout toolbar = new HorizontalLayout(btnNueva, buscar, tipoFilter);
        toolbar.addClassName("usuario-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.expand(buscar);

        crud.setFindAllOperation(() -> {
            String termino = buscar.getValue() != null ? buscar.getValue().trim().toLowerCase() : "";
            TipoAnimal tipoSeleccionado = tipoFilter.getValue();

            return mascotaService.findAll().stream()
                    .filter(mascota -> tipoSeleccionado == null || mascota.getTipoAnimal() == tipoSeleccionado)
                    .filter(mascota -> termino.isBlank()
                            || contiene(mascota.getNombre(), termino)
                            || contiene(mascota.getRaza(), termino)
                            || contiene(mascota.getSexo(), termino)
                            || contiene(labelTipoAnimal(mascota.getTipoAnimal()), termino)
                            || contiene(nombreCliente(mascota.getCliente()), termino))
                    .toList();
        });
        crud.setAddOperation(mascotaService::save);
        crud.setUpdateOperation(mascotaService::save);
        crud.setDeleteOperation(mascotaService::delete);

        crud.setSizeFull();
        add(toolbar, crud);
        expand(crud);
    }

    private void configurarGrid(GridCrud<Mascota> crud) {
        crud.getGrid().removeAllColumns();
        crud.getGrid().addColumn(Mascota::getIdMascota)
                .setHeader("ID")
                .setKey("idMascota")
                .setWidth("90px")
                .setFlexGrow(0)
                .setSortable(true);
        crud.getGrid().addColumn(Mascota::getNombre)
                .setHeader("Mascota")
                .setKey("nombre")
                .setFlexGrow(1)
                .setComparator(mascota -> valor(mascota.getNombre()));
        crud.getGrid().addColumn(mascota -> nombreCliente(mascota.getCliente()))
                .setHeader("Cliente")
                .setKey("cliente")
                .setFlexGrow(1)
                .setComparator(mascota -> nombreCliente(mascota.getCliente()));
        crud.getGrid().addComponentColumn(mascota -> {
            Span badge = new Span(labelTipoAnimal(mascota.getTipoAnimal()));
            badge.getElement().getThemeList().add("badge contrast");
            return badge;
        }).setHeader("Tipo").setKey("tipoAnimal");
        crud.getGrid().addColumn(Mascota::getRaza)
                .setHeader("Raza")
                .setKey("raza")
                .setFlexGrow(1)
                .setComparator(mascota -> valor(mascota.getRaza()));
        crud.getGrid().addColumn(Mascota::getSexo)
                .setHeader("Sexo")
                .setKey("sexo")
                .setWidth("120px")
                .setFlexGrow(0)
                .setComparator(mascota -> valor(mascota.getSexo()));
        crud.getGrid().addColumn(mascota -> mascota.getFechaNacimiento() != null ? mascota.getFechaNacimiento().toString() : "")
                .setHeader("Nacimiento")
                .setKey("fechaNacimiento")
                .setWidth("150px")
                .setFlexGrow(0);

        crud.getGrid().addComponentColumn(mascota -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.setAriaLabel("Editar mascota");
            btnEditar.addClickListener(event -> {
                crud.getGrid().select(mascota);
                crud.getUpdateButton().click();
            });

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addClassName("btn-accion-eliminar");
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEliminar.setAriaLabel("Eliminar mascota");
            btnEliminar.addClickListener(event -> {
                crud.getGrid().select(mascota);
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

    private void configurarFormulario(GridCrud<Mascota> crud, ClienteService clienteService) {
        DefaultCrudFormFactory<Mascota> formFactory = new DefaultCrudFormFactory<>(Mascota.class);
        formFactory.setUseBeanValidation(true);
        formFactory.setVisibleProperties("nombre", "cliente", "tipoAnimal", "raza", "sexo", "fechaNacimiento");
        formFactory.setFieldCaptions("Nombre", "Cliente", "Tipo de animal", "Raza", "Sexo", "Fecha de nacimiento");

        formFactory.setFieldProvider("nombre", mascota -> {
            TextField nombre = new TextField();
            nombre.setPlaceholder("Ej: Max");
            nombre.setClearButtonVisible(true);
            nombre.setRequiredIndicatorVisible(true);
            return nombre;
        });

        formFactory.setFieldProvider("cliente", mascota -> {
            ComboBox<Cliente> cliente = new ComboBox<>();
            cliente.setItems(clienteService.findAll());
            cliente.setItemLabelGenerator(this::nombreCliente);
            cliente.setRequiredIndicatorVisible(true);
            return cliente;
        });

        formFactory.setFieldProvider("tipoAnimal", mascota -> {
            ComboBox<TipoAnimal> tipoAnimal = new ComboBox<>();
            tipoAnimal.setItems(TipoAnimal.values());
            tipoAnimal.setItemLabelGenerator(this::labelTipoAnimal);
            tipoAnimal.setRequiredIndicatorVisible(true);
            return tipoAnimal;
        });

        formFactory.setFieldProvider("raza", mascota -> {
            TextField raza = new TextField();
            raza.setPlaceholder("Ej: Labrador");
            raza.setClearButtonVisible(true);
            raza.setRequiredIndicatorVisible(true);
            return raza;
        });

        formFactory.setFieldProvider("sexo", mascota -> {
            ComboBox<String> sexo = new ComboBox<>();
            sexo.setItems("Macho", "Hembra");
            sexo.setRequiredIndicatorVisible(true);
            return sexo;
        });

        formFactory.setFieldProvider("fechaNacimiento", mascota -> {
            DatePicker fechaNacimiento = new DatePicker();
            fechaNacimiento.setRequiredIndicatorVisible(true);
            return fechaNacimiento;
        });

        formFactory.setErrorListener(error -> {
            Throwable causa = error;
            while (causa.getCause() != null) {
                causa = causa.getCause();
            }

            String mensaje = causa.getMessage() != null && !causa.getMessage().isBlank()
                    ? causa.getMessage()
                    : "No se pudo guardar la mascota.";

            Notification notification = Notification.show(mensaje, 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        formFactory.setCaption(CrudOperation.ADD, "Registrar nueva mascota");
        formFactory.setCaption(CrudOperation.UPDATE, "Editar mascota");
        formFactory.setCaption(CrudOperation.DELETE, "¿Eliminar Mascota?");
        formFactory.setButtonCaption(CrudOperation.DELETE, "Sí, eliminar");
        formFactory.setCancelButtonCaption("Cancelar");

        crud.setCrudFormFactory(formFactory);
    }

    private String nombreCliente(Cliente cliente) {
        return cliente != null && cliente.getPersona() != null ? cliente.getPersona().getNombre() : "";
    }

    private String labelTipoAnimal(TipoAnimal tipoAnimal) {
        if (tipoAnimal == null) {
            return "";
        }
        String nombre = tipoAnimal.name().toLowerCase();
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
    }

    private boolean contiene(String valor, String termino) {
        return valor != null && valor.toLowerCase().contains(termino);
    }

    private String valor(String valor) {
        return valor != null ? valor : "";
    }
}
