
package com.perurecicla.repository;

import com.perurecicla.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PERU RECICLA
 *
 * Repositorio de Usuarios
 *
 * Permite realizar operaciones
 * de persistencia con Spring Data JPA
 * e Hibernate.
 *
 * Tabla MySQL: Usuarios
 */

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    // =====================================
    // BUSCAR USUARIO POR CORREO
    // =====================================

    Optional<Usuario> findByCorreo(String correo);

}
