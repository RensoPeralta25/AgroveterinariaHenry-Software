package com.agroveterinaria.view.configuracion;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadEvent;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ConfiguracionUsuarioView extends VerticalLayout {
    private byte[] fotoPerfil;
    private final Image previewFoto = new Image();
    private final Span iniciales = new Span();

    public ConfiguracionUsuarioView(SecurityService securityService, Runnable onPerfilActualizado) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("configuracion-view");

        Usuario usuario = securityService.obtenerUsuarioAutenticado();
        Empleado empleado = securityService.obtenerEmpleadoAutenticado();

        if (usuario == null || empleado == null || empleado.getPersona() == null) {
            add(crearEstadoVacio());
            return;
        }

        Persona persona = empleado.getPersona();
        fotoPerfil = usuario.getFotoPerfil();

        TextField nombreField = crearCampoTexto("Nombre");
        nombreField.setValue(valor(persona.getNombre()));
        nombreField.setAllowedCharPattern("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]");

        TextField apellidoField = crearCampoTexto("Apellido");
        apellidoField.setValue(valor(persona.getApellido()));
        apellidoField.setAllowedCharPattern("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]");

        TextField telefonoField = crearCampoTexto("Teléfono");
        telefonoField.setValue(valor(persona.getTelefono()));
        telefonoField.setPlaceholder("000-000-0000");
        telefonoField.setAllowedCharPattern("[0-9-]");
        telefonoField.setMaxLength(12);

        TextField rolField = crearCampoTexto("Rol");
        rolField.setValue(formatearRoles(empleado));
        rolField.setReadOnly(true);

        Upload uploadFoto = crearUploadFoto();
        Button eliminarFotoButton = new Button("Eliminar foto", VaadinIcon.TRASH.create());
        eliminarFotoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        eliminarFotoButton.addClickListener(event -> {
            fotoPerfil = null;
            uploadFoto.clearFileList();
            actualizarPreviewFoto(persona);
        });

        Div fotoPreview = new Div(previewFoto, iniciales);
        fotoPreview.addClassName("configuracion-photo-preview");
        actualizarPreviewFoto(persona);

        VerticalLayout fotoActions = new VerticalLayout(uploadFoto, eliminarFotoButton);
        fotoActions.setPadding(false);
        fotoActions.setSpacing(false);
        fotoActions.addClassName("configuracion-photo-actions");

        HorizontalLayout fotoPerfilLayout = new HorizontalLayout(fotoPreview, fotoActions);
        fotoPerfilLayout.addClassName("configuracion-photo-row");
        fotoPerfilLayout.setAlignItems(Alignment.CENTER);

        Button guardarPerfilButton = new Button("Guardar perfil", VaadinIcon.CHECK.create(), event -> {
            try {
                Empleado actualizado = securityService.actualizarPerfilAutenticado(
                        nombreField.getValue(),
                        apellidoField.getValue(),
                        telefonoField.getValue(),
                        fotoPerfil
                );
                actualizarPreviewFoto(actualizado.getPersona());
                onPerfilActualizado.run();
                mostrarExito("Perfil actualizado correctamente");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                mostrarError(ex.getMessage());
            }
        });
        guardarPerfilButton.addClassName("btn-nuevo");
        guardarPerfilButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div perfilSection = crearSeccion(
                "Perfil",
                fotoPerfilLayout,
                crearGridCampos(nombreField, apellidoField, telefonoField, rolField),
                crearAcciones(guardarPerfilButton)
        );

        PasswordField passwordActualField = crearCampoPassword("Contraseña actual");
        PasswordField passwordNuevaField = crearCampoPassword("Nueva contraseña");
        PasswordField confirmacionPasswordField = crearCampoPassword("Confirmar nueva contraseña");

        Button cambiarPasswordButton = new Button("Cambiar contraseña", VaadinIcon.LOCK.create(), event -> {
            try {
                securityService.cambiarPasswordAutenticado(
                        passwordActualField.getValue(),
                        passwordNuevaField.getValue(),
                        confirmacionPasswordField.getValue()
                );
                passwordActualField.clear();
                passwordNuevaField.clear();
                confirmacionPasswordField.clear();
                mostrarExito("Contraseña actualizada correctamente");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                mostrarError(ex.getMessage());
            }
        });
        cambiarPasswordButton.addClassName("btn-nuevo");
        cambiarPasswordButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div cuentaSection = crearSeccion(
                "Cuenta",
                crearGridCampos(passwordActualField, passwordNuevaField, confirmacionPasswordField),
                crearAcciones(cambiarPasswordButton)
        );

        add(perfilSection, cuentaSection);
    }

    private TextField crearCampoTexto(String label) {
        TextField field = new TextField(label);
        field.setWidthFull();
        field.setClearButtonVisible(true);
        return field;
    }

    private PasswordField crearCampoPassword(String label) {
        PasswordField field = new PasswordField(label);
        field.setWidthFull();
        field.setClearButtonVisible(true);
        return field;
    }

    private Upload crearUploadFoto() {
        Upload upload = new Upload();
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("Arrastra una imagen aquí"));
        upload.setUploadButton(new Button("Subir foto", VaadinIcon.UPLOAD.create()));
        upload.getElement().addEventListener("file-remove", event -> {
            fotoPerfil = null;
            previewFoto.getElement().removeAttribute("src");
        });
        upload.setUploadHandler(this::procesarFoto);
        return upload;
    }

    private void procesarFoto(UploadEvent event) {
        try (java.io.InputStream stream = event.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesLeidos;

            while ((bytesLeidos = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesLeidos);
            }

            byte[] bytes = baos.toByteArray();
            getUI().ifPresent(ui -> ui.access(() -> {
                fotoPerfil = bytes;
                actualizarPreviewFoto(null);
            }));
        } catch (Exception ex) {
            getUI().ifPresent(ui -> ui.access(() -> mostrarError("Error al procesar la imagen")));
        }
    }

    private Div crearSeccion(String titulo, com.vaadin.flow.component.Component... componentes) {
        H3 heading = new H3(titulo);
        heading.addClassName("configuracion-section-title");

        Div section = new Div(heading);
        section.addClassName("configuracion-section");
        section.add(componentes);
        return section;
    }

    private Div crearGridCampos(com.vaadin.flow.component.Component... campos) {
        Div grid = new Div(campos);
        grid.addClassName("configuracion-form-grid");
        return grid;
    }

    private HorizontalLayout crearAcciones(Button button) {
        HorizontalLayout actions = new HorizontalLayout(button);
        actions.addClassName("configuracion-actions");
        actions.setJustifyContentMode(JustifyContentMode.END);
        return actions;
    }

    private Div crearEstadoVacio() {
        Div empty = new Div();
        empty.addClassName("configuracion-empty");
        empty.setText("No se encontró un perfil asociado al usuario actual.");
        return empty;
    }

    private void actualizarPreviewFoto(Persona persona) {
        if (fotoPerfil != null && fotoPerfil.length > 0) {
            String base64 = Base64.getEncoder().encodeToString(fotoPerfil);
            previewFoto.setSrc("data:image/jpeg;base64," + base64);
            previewFoto.setVisible(true);
            iniciales.setVisible(false);
            return;
        }

        previewFoto.setVisible(false);
        iniciales.setVisible(true);
        iniciales.setText(persona == null ? "" : obtenerIniciales(persona));
    }

    private String obtenerIniciales(Persona persona) {
        String nombre = valor(persona.getNombre());
        String apellido = valor(persona.getApellido());
        String primera = nombre.isBlank() ? "" : nombre.substring(0, 1);
        String segunda = apellido.isBlank() ? "" : apellido.substring(0, 1);
        String resultado = (primera + segunda).toUpperCase();
        return resultado.isBlank() ? "U" : resultado;
    }

    private String formatearRoles(Empleado empleado) {
        if (empleado.getCargos() == null || empleado.getCargos().isEmpty()) {
            return "Sin rol asignado";
        }

        return empleado.getCargos().stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(this::formatearRol)
                .collect(Collectors.joining(", "));
    }

    private String formatearRol(RolEmpleado rol) {
        String texto = rol.name().toLowerCase().replace("_", " ");
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    private String valor(String value) {
        return value == null ? "" : value;
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 3500, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4500, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
