package com.agroveterinaria.view;

import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.service.UsuarioService;
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
        crudUsuario.setAddOperation(usuarioService::add);
        crudUsuario.setUpdateOperation(usuarioService::add);

        add(crudUsuario);
    }
}
