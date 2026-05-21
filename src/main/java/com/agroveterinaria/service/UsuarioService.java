package com.agroveterinaria.service;

import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario add(Usuario u) {
        Optional<Usuario> existente = usuarioRepository.findByUsername(u.getUsername());

        if(existente.isPresent() && !existente.get().getIdUsuario().equals(u.getIdUsuario())){
            throw new IllegalArgumentException("Error: El nombre de usuario '" + u.getUsername() + "' ya está en uso");
        }

        return usuarioRepository.save(u);
    }

    public List<Usuario> findAll(){return usuarioRepository.findAll();}
}
