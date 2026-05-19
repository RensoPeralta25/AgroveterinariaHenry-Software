package com.agroveterinaria.service;

import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario add(Usuario u) {
        return usuarioRepository.save(u);
    }

    public List<Usuario> findAll(){return usuarioRepository.findAll();}
}
