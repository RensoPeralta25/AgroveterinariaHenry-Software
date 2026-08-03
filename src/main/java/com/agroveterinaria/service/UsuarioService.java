package com.agroveterinaria.service;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.enums.RolEmpleado;
import com.agroveterinaria.repository.EmpleadoRepository;
import com.agroveterinaria.repository.UsuarioRepository;
import com.agroveterinaria.security.SecurityService;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@RolesAllowed({"ADMINISTRADOR", "RECURSOS_HUMANOS"})
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;

    public Usuario save(Usuario u) {
        Optional<Usuario> existente = usuarioRepository.findByUsername(u.getUsername());

        if(existente.isPresent() && !existente.get().getIdUsuario().equals(u.getIdUsuario())){
            throw new IllegalArgumentException("Error: El nombre de usuario '" + u.getUsername() + "' ya está en uso");
        }

        boolean currentUserIsAdmin = securityService.isCurrentUserAdmin();

        if (u.getIdUsuario() != null && !currentUserIsAdmin) {
            Optional<Empleado> empleadoVinculado = empleadoRepository.findByUsuario(u);
            if (empleadoVinculado.isPresent() && empleadoVinculado.get().getCargos().contains(RolEmpleado.ADMINISTRADOR)) {
                throw new IllegalStateException("Acceso denegado: No tiene permisos para modificar las credenciales de acceso de un Administrador.");
            }
        }

        if (u.getIdUsuario() == null || (u.getPassword() != null && !u.getPassword().startsWith("$2a$"))) {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
        }

        return usuarioRepository.save(u);
    }

    public List<Usuario> findAll(){return usuarioRepository.findAll();}

    public void delete(Usuario u){
        boolean currentUserIsAdmin = securityService.isCurrentUserAdmin();

        if (!currentUserIsAdmin) {
            Optional<Empleado> empleadoVinculado = empleadoRepository.findByUsuario(u);
            if (empleadoVinculado.isPresent() && empleadoVinculado.get().getCargos().contains(RolEmpleado.ADMINISTRADOR)) {
                throw new IllegalStateException("Acceso denegado: No tiene permisos para eliminar el usuario de un Administrador.");
            }
        }

        Usuario usuarioAutenticado = securityService.obtenerUsuarioAutenticado();
        if (usuarioAutenticado != null && usuarioAutenticado.getUsername().equals(u.getUsername())) {
            throw new IllegalStateException("Acción no permitida: No puede eliminar su propio usuario mientras está en sesión.");
        }

        usuarioRepository.delete(u);
    }
}
