
package com.perurecicla.service.impl;

import com.perurecicla.model.Usuario;
import com.perurecicla.repository.UsuarioRepository;
import com.perurecicla.service.UsuarioService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

/**
 * PERU RECICLA
 *
 * Implementacion del servicio de usuarios.
 *
 * Funcionalidades:
 *
 * 1. Registrar usuarios nuevos.
 * 2. Cifrar contrasenas mediante BCrypt.
 * 3. Buscar usuarios por correo.
 * 4. Listar usuarios registrados.
 * 5. Buscar usuarios por identificador.
 *
 * Arquitectura:
 *
 * Controller -> Service -> Repository -> MySQL
 */

@Service
public class UsuarioServiceImpl
        implements UsuarioService {

    // =====================================
    // REPOSITORIO DE USUARIOS
    // =====================================

    private final UsuarioRepository usuarioRepository;

    // =====================================
    // CODIFICADOR DE CONTRASENAS
    // =====================================

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository) {

        this.usuarioRepository = usuarioRepository;
    }

    // =====================================
    // REGISTRAR NUEVO USUARIO
    // =====================================

    @Override
    @Transactional
    public Usuario registrarUsuario(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                "Los datos del usuario son obligatorios"
            );
        }

        // Validar correo

        if (usuario.getCorreo() == null ||
            usuario.getCorreo().isBlank()) {

            throw new IllegalArgumentException(
                "El correo es obligatorio"
            );
        }

        // Validar DNI

        if (usuario.getDni() == null ||
            !usuario.getDni().matches("\\d{8}")) {

            throw new IllegalArgumentException(
                "El DNI debe contener 8 digitos"
            );
        }

        // Validar contrasena

        if (usuario.getContrasena() == null ||
            usuario.getContrasena().isBlank()) {

            throw new IllegalArgumentException(
                "La contrasena es obligatoria"
            );
        }

        // Verificar correo duplicado

        if (usuarioRepository
                .findByCorreo(usuario.getCorreo())
                .isPresent()) {

            throw new IllegalArgumentException(
                "El correo ya se encuentra registrado"
            );
        }

        // Evitar recibir un ID arbitrario
        // durante el registro de usuarios

        usuario.setId_usuario(null);

        // Los registros publicos corresponden
        // al perfil vecino

        usuario.setRol(
            Usuario.Rol.ROLE_vecino
        );

        // Estado inicial activo

        usuario.setEstado(
            Usuario.Estado.activo
        );

        // Puntos iniciales

        usuario.setPuntos_acumulados(
            BigDecimal.ZERO
        );

        // Cifrar contrasena con BCrypt

        String contrasenaCifrada =
            passwordEncoder.encode(
                usuario.getContrasena()
            );

        usuario.setContrasena(
            contrasenaCifrada
        );

        // Guardar usuario en MySQL

        return usuarioRepository.save(usuario);
    }

    // =====================================
    // BUSCAR USUARIO POR CORREO
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {

        return usuarioRepository
                .findByCorreo(correo)
                .orElse(null);
    }

    // =====================================
    // LISTAR TODOS LOS USUARIOS
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {

        return usuarioRepository.findAll();
    }

    // =====================================
    // BUSCAR USUARIO POR ID
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Integer id) {

        return usuarioRepository
                .findById(id)
                .orElse(null);
    }

}
