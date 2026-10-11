
package com.perurecicla.security;

import com.perurecicla.model.Usuario;
import com.perurecicla.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * PERU RECICLA
 *
 * Servicio de autenticacion de usuarios.
 *
 * Permite que Spring Security consulte
 * las cuentas almacenadas en MySQL.
 *
 * Funcionalidades:
 *
 * 1. Buscar usuario por correo.
 * 2. Obtener su contrasena cifrada.
 * 3. Obtener su rol de seguridad.
 * 4. Verificar si su cuenta esta activa.
 *
 * Arquitectura:
 *
 * Spring Security
 *       |
 * UsuarioDetailsService
 *       |
 * UsuarioRepository
 *       |
 * MySQL
 */

@Service
public class UsuarioDetailsService
        implements UserDetailsService {

    // =====================================
    // REPOSITORIO DE USUARIOS
    // =====================================

    private final UsuarioRepository usuarioRepository;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public UsuarioDetailsService(
            UsuarioRepository usuarioRepository) {

        this.usuarioRepository = usuarioRepository;
    }

    // =====================================
    // CARGAR USUARIO DESDE MYSQL
    // =====================================

    @Override
    public UserDetails loadUserByUsername(
            String correo)
            throws UsernameNotFoundException {

        // =================================
        // VALIDAR CORREO
        // =================================

        if (correo == null || correo.isBlank()) {

            throw new UsernameNotFoundException(
                "Credenciales invalidas"
            );
        }

        // =================================
        // NORMALIZAR CORREO
        // =================================

        String correoNormalizado = correo
            .trim()
            .toLowerCase(Locale.ROOT);

        // =================================
        // BUSCAR USUARIO EN MYSQL
        // =================================

        Usuario usuario = usuarioRepository
            .findByCorreo(correoNormalizado)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "Credenciales invalidas"
                )
            );

        // =================================
        // VERIFICAR DATOS DE SEGURIDAD
        // =================================

        if (usuario.getContrasena() == null ||
            usuario.getContrasena().isBlank() ||
            usuario.getRol() == null ||
            usuario.getEstado() == null) {

            throw new UsernameNotFoundException(
                "La cuenta no tiene una configuracion valida"
            );
        }

        // =================================
        // VERIFICAR ESTADO DE LA CUENTA
        // =================================

        boolean cuentaInactiva =
            usuario.getEstado() !=
            Usuario.Estado.activo;

        // =================================
        // CONSTRUIR USUARIO DE SEGURIDAD
        // =================================

        return User.withUsername(
                    usuario.getCorreo()
                )
                .password(
                    usuario.getContrasena()
                )
                .authorities(
                    usuario.getRol().name()
                )
                .disabled(cuentaInactiva)
                .build();
    }
}
