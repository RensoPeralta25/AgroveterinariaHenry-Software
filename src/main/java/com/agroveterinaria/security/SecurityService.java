package com.agroveterinaria.security;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.repository.UsuarioRepository;
import com.agroveterinaria.service.EmpleadoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoService empleadoService;

    public SecurityService(UsuarioRepository usuarioRepository, EmpleadoService empleadoService) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoService = empleadoService;
    }

    public Empleado obtenerEmpleadoAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            Usuario usuarioActual = usuarioRepository.findByUsername(username).orElse(null);

            if (usuarioActual != null) {
                return empleadoService.findByUsuario(usuarioActual);
            }
        }

        return null;
    }
}