
package com.perurecicla.service;

import com.perurecicla.model.Usuario;
import java.util.List;

/**
 * PERU RECICLA
 *
 * Servicio de Usuarios
 *
 * Define las operaciones de negocio
 * relacionadas con el registro y
 * administracion de usuarios.
 *
 * Arquitectura:
 * Controller -> Service -> Repository
 */

public interface UsuarioService {

    // =====================================
    // REGISTRAR UN NUEVO USUARIO
    // =====================================

    Usuario registrarUsuario(Usuario usuario);

    // =====================================
    // BUSCAR USUARIO POR CORREO
    // =====================================

    Usuario buscarPorCorreo(String correo);

    // =====================================
    // LISTAR TODOS LOS USUARIOS
    // =====================================

    List<Usuario> listarTodos();

    // =====================================
    // BUSCAR USUARIO POR ID
    // =====================================

    Usuario buscarPorId(Integer id);

}
