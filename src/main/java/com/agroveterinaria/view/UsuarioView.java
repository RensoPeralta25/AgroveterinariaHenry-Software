package com.agroveterinaria.view;

import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.service.UsuarioService;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.impl.GridCrud;


@Route("usuarios")
public class UsuarioView extends VerticalLayout {

    public UsuarioView(UsuarioService usuarioService) {
        GridCrud<Usuario> crudUsuario = new GridCrud<>(Usuario.class);
        crudUsuario.getGrid().setColumns("idUsuario", "username");
        crudUsuario.getGrid().getColumnByKey("idUsuario").setHeader("ID");
        crudUsuario.getGrid().getColumnByKey("username").setHeader("Usuario");

        crudUsuario.getCrudFormFactory().setVisibleProperties("username", "password");
        crudUsuario.getCrudFormFactory().setFieldType("password", PasswordField.class);

        crudUsuario.setFindAllOperation(usuarioService::findAll);
        crudUsuario.setAddOperation(usuario -> {
            try{
                return usuarioService.add(usuario);
            } catch (IllegalArgumentException e){
                Notification notificacion = Notification.show(e.getMessage(), 4000, Notification.Position.MIDDLE);
                notificacion.addThemeVariants(NotificationVariant.LUMO_ERROR);

                throw new RuntimeException("Validación fallida");
            }
        });

        crudUsuario.setUpdateOperation(usuario -> {
            try{
                return usuarioService.add(usuario);
            } catch (IllegalArgumentException e) {
                Notification notificacion = Notification.show(e.getMessage(), 4000, Notification.Position.MIDDLE);
                notificacion.addThemeVariants(NotificationVariant.LUMO_ERROR);

                throw new RuntimeException("Validación fallida");
            }
        });

        add(crudUsuario);
    }
}
