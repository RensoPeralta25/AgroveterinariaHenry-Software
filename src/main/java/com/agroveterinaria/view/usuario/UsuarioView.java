package com.agroveterinaria.view.usuario;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@Route("usuarios")
public class UsuarioView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final EmpleadoService empleadoService;

    public UsuarioView(UsuarioService usuarioService, EmpleadoService empleadoService) {
        this.usuarioService = usuarioService;
        this.empleadoService = empleadoService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        GridCrud<Usuario> crudUsuario = new GridCrud<>(Usuario.class, new WindowBasedCrudLayout());
        crudUsuario.addClassName("usuario-crud");
        crudUsuario.getGrid().addClassName("usuario-grid");

        DefaultCrudFormFactory<Usuario> formFactory = (DefaultCrudFormFactory<Usuario>) crudUsuario.getCrudFormFactory();

        crudUsuario.getGrid().setColumns("idUsuario", "username");
        crudUsuario.getGrid().getColumnByKey("idUsuario").setHeader("ID");
        crudUsuario.getGrid().getColumnByKey("username").setHeader("Usuario");

        crudUsuario.getGrid().addColumn(usuario -> {
            Empleado emp = empleadoService.findByUsuario(usuario);
            return emp != null && emp.getPersona() != null ? emp.getPersona().getNombre() : "Sin asignar";
        }).setHeader("Empleado").setFlexGrow(1);

        crudUsuario.getGrid().addComponentColumn(usuario -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addClassName("btn-accion-editar");
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> dialogEditarUsuario(usuario, crudUsuario));

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addClassName("btn-accion-eliminar");
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEliminar.addClickListener(e -> {
                crudUsuario.getGrid().select(usuario);
                crudUsuario.getDeleteButton().click();
            });

            HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);

        crudUsuario.getGrid().addThemeNames("row-stripes");

        crudUsuario.getAddButton().setVisible(false);
        crudUsuario.getUpdateButton().setVisible(false);
        crudUsuario.getDeleteButton().setVisible(false);
        crudUsuario.getFindAllButton().setVisible(false);

        Button btnNuevo = new Button("Nuevo usuario", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> dialogNuevoUsuario(crudUsuario));

        TextField buscarUsuario = new TextField();
        buscarUsuario.setPlaceholder("Buscar usuario...");
        buscarUsuario.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarUsuario.setValueChangeMode(ValueChangeMode.LAZY);
        buscarUsuario.addValueChangeListener(e -> {
            String filtro = e.getValue().toLowerCase().trim();
            crudUsuario.setFindAllOperation(() ->
                    usuarioService.findAll().stream()
                            .filter(u -> u.getUsername() != null &&
                                    u.getUsername().toLowerCase().contains(filtro))
                            .toList()
            );
            crudUsuario.refreshGrid();
        });

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, buscarUsuario);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.addClassName("usuario-toolbar");

        formFactory.setVisibleProperties("username", "password");
        formFactory.setFieldCaptions("Nombre de Usuario", "Contraseña");
        formFactory.setFieldType("password", PasswordField.class);

        formFactory.setButtonCaption(CrudOperation.DELETE, "Sí, eliminar");
        formFactory.setCancelButtonCaption("Cancelar");

        formFactory.setCaption(CrudOperation.DELETE, "¿Eliminar Usuario?");

        crudUsuario.setFindAllOperation(usuarioService::findAll);
        crudUsuario.setDeleteOperation(usuario -> {
            Empleado emp = empleadoService.findByUsuario(usuario);
            if (emp != null) {
                emp.setUsuario(null);
                empleadoService.save(emp);
            }
            usuarioService.delete(usuario);
        });

        add(toolbar, crudUsuario);
    }

    private void dialogNuevoUsuario(GridCrud<Usuario> crudUsuario) {
        Dialog dialog = new Dialog();
        dialog.setWidth("700px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3("Crear Usuario");
        titulo.getStyle().set("margin", "0 0 16px 0");

        TextField usernameField = new TextField("Nombre de Usuario");
        usernameField.setWidthFull();

        PasswordField passwordField = new PasswordField("Contraseña");
        passwordField.setWidthFull();

        HorizontalLayout campos = new HorizontalLayout(usernameField, passwordField);
        campos.setWidthFull();
        campos.setSpacing(true);

        H3 tituloTabla = new H3("Seleccionar Empleado");
        tituloTabla.getStyle().set("margin", "16px 0 8px 0").set("font-size", "15px");

        TextField buscarEmpleado = new TextField();
        buscarEmpleado.setPlaceholder("Buscar empleado...");
        buscarEmpleado.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarEmpleado.setWidthFull();

        Grid<Empleado> gridEmpleados = new Grid<>(Empleado.class, false);
        gridEmpleados.addClassName("usuario-grid");
        gridEmpleados.addThemeNames("row-stripes");
        gridEmpleados.setHeight("250px");

        gridEmpleados.addColumn(emp ->
                emp.getPersona() != null ? emp.getPersona().getNombre() : ""
        ).setHeader("Nombre").setFlexGrow(1);

        gridEmpleados.addColumn(emp ->
                emp.getPersona() != null ? emp.getPersona().getCedula() : ""
        ).setHeader("Cédula").setFlexGrow(1);

        gridEmpleados.addColumn(emp ->
                emp.getPersona() != null ? emp.getPersona().getTelefono() : ""
        ).setHeader("Teléfono").setFlexGrow(1);

        List<Empleado> sinUsuario = empleadoService.findAll()
                .stream()
                .filter(emp -> emp.getUsuario() == null)
                .toList();
        gridEmpleados.setItems(sinUsuario);

        Button btnGuardar = new Button("Crear", e -> {
            Empleado empleadoSeleccionado = gridEmpleados.asSingleSelect().getValue();

            if (usernameField.isEmpty()) {
                mostrarError("El nombre de usuario es obligatorio");
                return;
            }
            if (passwordField.isEmpty()) {
                mostrarError("La contraseña es obligatoria");
                return;
            }
            if (empleadoSeleccionado == null) {
                mostrarError("Debes seleccionar un empleado");
                return;
            }

            try {
                Usuario nuevoUsuario = new Usuario();
                nuevoUsuario.setUsername(usernameField.getValue().trim());
                nuevoUsuario.setPassword(passwordField.getValue());
                Usuario usuarioGuardado = usuarioService.save(nuevoUsuario);

                empleadoSeleccionado.setUsuario(usuarioGuardado);
                empleadoService.save(empleadoSeleccionado);

                dialog.close();
                crudUsuario.refreshGrid();

                Notification notif = Notification.show(
                        "Usuario creado y asignado a " + empleadoSeleccionado.getPersona().getNombre(),
                        3500, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
            }
        });
        btnGuardar.addClassName("btn-nuevo");
        btnGuardar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.END);
        botones.getStyle().set("margin-top", "16px");

        VerticalLayout contenido = new VerticalLayout(
                titulo, campos, tituloTabla, gridEmpleados, botones);
        contenido.setPadding(true);
        contenido.setSpacing(false);
        contenido.addClassName("usuario-form");

        dialog.add(contenido);
        dialog.open();
    }

    private void dialogEditarUsuario(Usuario usuario, GridCrud<Usuario> crudUsuario) {
        Dialog dialog = new Dialog();
        dialog.setWidth("420px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3("Editar Usuario");
        titulo.getStyle().set("margin", "0 0 16px 0");

        TextField usernameField = new TextField("Nombre de Usuario");
        usernameField.setWidthFull();
        usernameField.setValue(usuario.getUsername() != null ? usuario.getUsername() : "");

        PasswordField passwordField = new PasswordField("Contraseña");
        passwordField.setWidthFull();
        passwordField.setValue(usuario.getPassword() != null ? usuario.getPassword() : "");

        Button btnGuardar = new Button("Guardar cambios", e -> {
            if (usernameField.isEmpty()) {
                mostrarError("El nombre de usuario es obligatorio");
                return;
            }
            if (passwordField.isEmpty()) {
                mostrarError("La contraseña es obligatoria");
                return;
            }

            try {
                usuario.setUsername(usernameField.getValue().trim());
                usuario.setPassword(passwordField.getValue());
                usuarioService.save(usuario);

                dialog.close();
                crudUsuario.refreshGrid();

                Notification notif = Notification.show(
                        "Usuario actualizado correctamente",
                        3500, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
            }
        });
        btnGuardar.addClassName("btn-nuevo");
        btnGuardar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "16px");

        VerticalLayout contenido = new VerticalLayout(titulo, usernameField, passwordField, botones);
        contenido.setPadding(true);
        contenido.setSpacing(true);
        contenido.addClassName("usuario-form");

        dialog.add(contenido);
        dialog.open();
    }

    private void mostrarError(String mensaje) {
        Notification notif = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}