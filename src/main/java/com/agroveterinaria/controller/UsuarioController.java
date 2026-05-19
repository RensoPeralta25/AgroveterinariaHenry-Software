package com.agroveterinaria.controller;

import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Usuario> create(@RequestBody Usuario u){
        Usuario nuevoUsuario = usuarioService.add(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }

}
