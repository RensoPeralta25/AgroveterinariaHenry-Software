package com.agroveterinaria.service;

import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.repository.UsuarioRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RolesAllowed("ADMINISTRADOR")
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario save(Usuario u) {
        Optional<Usuario> existente = usuarioRepository.findByUsername(u.getUsername());

        if(existente.isPresent() && !existente.get().getIdUsuario().equals(u.getIdUsuario())){
            throw new IllegalArgumentException("Error: El nombre de usuario '" + u.getUsername() + "' ya está en uso");
        }

        if (u.getIdUsuario() == null || u.getPassword().length() < 60) {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
        }

        return usuarioRepository.save(u);
    }

    public List<Usuario> findAll(){return usuarioRepository.findAll();}

    public void delete(Usuario u){
        usuarioRepository.delete(u);
    }
}
