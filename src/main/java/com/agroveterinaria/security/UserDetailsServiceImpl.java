package com.agroveterinaria.security;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.repository.EmpleadoRepository;
import com.agroveterinaria.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository, EmpleadoRepository empleadoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        Optional<Empleado> emp = empleadoRepository.findByUsuario(usuario);

        if (emp.isEmpty() || emp.get().getCargos().isEmpty()) {
            throw new UsernameNotFoundException("Acceso denegado: El usuario no tiene un empleado o roles válidos asignados.");
        }

        String[] roles = emp.get().getCargos().stream()
                .map(Enum::name)
                .toArray(String[]::new);

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(roles)
                .disabled(!usuario.isActivo())
                .build();
    }
}
