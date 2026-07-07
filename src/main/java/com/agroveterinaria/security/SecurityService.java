package com.agroveterinaria.security;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.repository.EmpleadoRepository;
import com.agroveterinaria.repository.PersonaRepository;
import com.agroveterinaria.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class SecurityService {
    private static final Pattern NOMBRE_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");
    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^\\d{3}-\\d{3}-\\d{4}$");

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityService(
            UsuarioRepository usuarioRepository,
            EmpleadoRepository empleadoRepository,
            PersonaRepository personaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Empleado obtenerEmpleadoAutenticado() {
        Usuario usuarioActual = obtenerUsuarioAutenticado();
        if (usuarioActual == null) {
            return null;
        }

        return empleadoRepository.findByUsuario(usuarioActual).orElse(null);
    }

    public Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return usuarioRepository.findByUsername(username).orElse(null);
        }

        return null;
    }

    @Transactional
    public Empleado actualizarPerfilAutenticado(String nombre, String apellido, String telefono, byte[] fotoPerfil) {
        Usuario usuarioActual = obtenerUsuarioAutenticado();
        Empleado empleadoActual = obtenerEmpleadoAutenticado();

        if (usuarioActual == null || empleadoActual == null || empleadoActual.getPersona() == null) {
            throw new IllegalStateException("No se pudo encontrar el perfil del usuario autenticado.");
        }

        String nombreNormalizado = normalizar(nombre);
        String apellidoNormalizado = normalizar(apellido);
        String telefonoNormalizado = normalizar(telefono);

        validarNombre("nombre", nombreNormalizado);
        validarNombre("apellido", apellidoNormalizado);

        if (telefonoNormalizado.isBlank()) {
            throw new IllegalArgumentException("El teléfono es obligatorio.");
        }

        if (!TELEFONO_PATTERN.matcher(telefonoNormalizado).matches()) {
            throw new IllegalArgumentException("El teléfono debe tener el formato 000-000-0000.");
        }

        Persona persona = empleadoActual.getPersona();
        persona.setNombre(nombreNormalizado);
        persona.setApellido(apellidoNormalizado);
        persona.setTelefono(telefonoNormalizado);
        personaRepository.save(persona);

        usuarioActual.setFotoPerfil(fotoPerfil);
        usuarioRepository.save(usuarioActual);

        return empleadoActual;
    }

    @Transactional
    public void cambiarPasswordAutenticado(String passwordActual, String passwordNueva, String confirmacionPassword) {
        Usuario usuarioActual = obtenerUsuarioAutenticado();

        if (usuarioActual == null) {
            throw new IllegalStateException("No se pudo encontrar el usuario autenticado.");
        }

        String actual = passwordActual == null ? "" : passwordActual;
        String nueva = passwordNueva == null ? "" : passwordNueva;
        String confirmacion = confirmacionPassword == null ? "" : confirmacionPassword;

        if (actual.isBlank()) {
            throw new IllegalArgumentException("La contraseña actual es obligatoria.");
        }

        if (!passwordEncoder.matches(actual, usuarioActual.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        if (nueva.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        if (!nueva.equals(confirmacion)) {
            throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña.");
        }

        usuarioActual.setPassword(passwordEncoder.encode(nueva));
        usuarioRepository.save(usuarioActual);
    }

    private void validarNombre(String campo, String valor) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El " + campo + " es obligatorio.");
        }

        if (!NOMBRE_PATTERN.matcher(valor).matches()) {
            throw new IllegalArgumentException("El " + campo + " solo debe contener letras.");
        }
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
