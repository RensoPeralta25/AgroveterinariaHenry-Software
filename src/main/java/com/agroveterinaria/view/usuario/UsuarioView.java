package com.agroveterinaria.view.usuario;

import com.agroveterinaria.component.CrudGridPaginator;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.security.SecurityService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class UsuarioView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final EmpleadoService empleadoService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioView(UsuarioService usuarioService, EmpleadoService empleadoService, PasswordEncoder passwordEncoder, SecurityService securityService) {
        this.usuarioService = usuarioService;
        this.empleadoService = empleadoService;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        GridCrud<Usuario> crudUsuario = new GridCrud<>(Usuario.class, new WindowBasedCrudLayout());
        crudUsuario.getGrid().addClassName("usuario-grid");
        CrudGridPaginator<Usuario> paginator = new CrudGridPaginator<>(10, "usuarios");
        paginator.setRefreshOperation(crudUsuario::refreshGrid);

        DefaultCrudFormFactory<Usuario> formFactory = (DefaultCrudFormFactory<Usuario>) crudUsuario.getCrudFormFactory();

        crudUsuario.getGrid().setColumns("username");
        crudUsuario.getGrid().getColumnByKey("username").setHeader("Usuario").setSortable(true);

        crudUsuario.getGrid().addColumn(usuario -> {
            Empleado emp = empleadoService.findByUsuario(usuario);
            return emp != null && emp.getPersona() != null ? emp.getPersona().getNombre() + " " + emp.getPersona().getApellido() : "Sin asignar";
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

            Usuario actual = securityService.obtenerUsuarioAutenticado();
            boolean esUsuarioActual = actual != null &&
                    actual.getUsername() != null &&
                    usuario.getUsername() != null &&
                    usuario.getUsername().equals(actual.getUsername());

            if (esUsuarioActual) {
                btnEditar.setEnabled(false);
                btnEliminar.setEnabled(false);
            }

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
            paginator.setSource(() ->
                    usuarioService.findAll().stream()
                            .filter(u -> u.getUsername() != null &&
                                    u.getUsername().toLowerCase().contains(filtro))
                            .toList()
            );
            paginator.reset();
        });

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, buscarUsuario);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.expand(buscarUsuario);
        toolbar.addClassName("usuario-toolbar");

        formFactory.setVisibleProperties(CrudOperation.DELETE, "username");
        formFactory.setFieldCaptions(CrudOperation.DELETE, "Usuario a eliminar");

        formFactory.setButtonCaption(CrudOperation.DELETE, "Sí, eliminar");
        formFactory.setCancelButtonCaption("Cancelar");

        formFactory.setCaption(CrudOperation.DELETE, "¿Eliminar Usuario?");

        paginator.setSource(usuarioService::findAll);
        crudUsuario.setFindAllOperation(paginator::pageItems);
        crudUsuario.setDeleteOperation(usuario -> {
            Empleado emp = empleadoService.findByUsuario(usuario);
            if (emp != null) {
                emp.setUsuario(null);
                empleadoService.save(emp);
            }
            usuarioService.delete(usuario);
        });

        add(toolbar, paginator, crudUsuario);
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
                emp.getPersona() != null ? emp.getPersona().getNombre() + " " + emp.getPersona().getApellido() : ""
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

        usernameField.addValueChangeListener(e -> usernameField.setInvalid(false));
        passwordField.addValueChangeListener(e -> passwordField.setInvalid(false));

        Button btnGuardar = new Button("Crear",new Icon(VaadinIcon.CHECK), e -> {
            Empleado empleadoSeleccionado = gridEmpleados.asSingleSelect().getValue();

            if (usernameField.isEmpty()) {
                usernameField.setInvalid(true);
                usernameField.setErrorMessage("El nombre de usuario es obligatorio");
                return;
            }

            if (passwordField.isEmpty() || passwordField.getValue().length() < 6) {
                passwordField.setInvalid(true);
                passwordField.setErrorMessage("La contraseña debe tener al menos 6 caracteres");
                return;
            }

            if (empleadoSeleccionado == null) {
                Notification notif = Notification.show(
                        "Debes seleccionar un empleado", 3500, Notification.Position.MIDDLE);
                notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                Usuario nuevoUsuario = new Usuario();
                nuevoUsuario.setUsername(usernameField.getValue().trim());

                String claveEncriptada = passwordEncoder.encode(passwordField.getValue());
                nuevoUsuario.setPassword(claveEncriptada);

                empleadoSeleccionado.setUsuario(nuevoUsuario);
                empleadoService.save(empleadoSeleccionado);

                dialog.close();
                crudUsuario.refreshGrid();

                Notification notif = Notification.show(
                        "Usuario creado y asignado a " + empleadoSeleccionado.getPersona().getNombre(),
                        3500, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (IllegalArgumentException ex) {
                Notification notif = Notification.show(
                        ex.getMessage(), 4000, Notification.Position.MIDDLE);
                notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnGuardar.addClassName("btn-nuevo");
        btnGuardar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-cancelar");
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.END);
        botones.getStyle().set("margin-top", "16px");

        VerticalLayout contenido = new VerticalLayout(
                titulo, campos, tituloTabla, gridEmpleados, botones);
        contenido.setPadding(true);
        contenido.setSpacing(false);

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
        passwordField.setPlaceholder("Dejar en blanco para conservar la actual");
        passwordField.setClearButtonVisible(true);

        usernameField.addValueChangeListener(e -> usernameField.setInvalid(false));
        passwordField.addValueChangeListener(e -> passwordField.setInvalid(false));

        Button btnGuardar = new Button("Guardar cambios", new Icon(VaadinIcon.CHECK), e -> {
            if (usernameField.isEmpty()) {
                usernameField.setInvalid(true);
                usernameField.setErrorMessage("El nombre de usuario es obligatorio");
                return;
            }

            try {
                usuario.setUsername(usernameField.getValue().trim());

                if (!passwordField.isEmpty()) {
                    usuario.setPassword(passwordField.getValue());
                }

                usuarioService.save(usuario);

                dialog.close();
                crudUsuario.refreshGrid();

                Notification notif = Notification.show(
                        "Usuario actualizado correctamente",
                        3500, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification notif = Notification.show(
                        ex.getMessage(), 4000, Notification.Position.MIDDLE);
                notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnGuardar.addClassName("btn-nuevo");
        btnGuardar.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-cancelar");
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setWidthFull();
        botones.getStyle().set("margin-top", "16px");

        VerticalLayout contenido = new VerticalLayout(titulo, usernameField, passwordField, botones);
        contenido.setPadding(true);
        contenido.setSpacing(true);

        dialog.add(contenido);
        dialog.open();
    }

    private String getUsuarioLogueado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            return authentication.getName();
        }
        return null;
    }

}
