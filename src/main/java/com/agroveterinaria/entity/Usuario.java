package com.agroveterinaria.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Usuario {
    Long idUsuario;
    String username;
    String Password;

    public Usuario(){}

    public Usuario(Long idUsuario, String username, String password) {
        this.idUsuario = idUsuario;
        this.username = username;
        Password = password;
    }
}

