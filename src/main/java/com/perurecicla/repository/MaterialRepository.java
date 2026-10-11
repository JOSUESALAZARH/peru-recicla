
package com.perurecicla.repository;

import com.perurecicla.model.Material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * PERU RECICLA
 *
 * Repositorio de Materiales
 *
 * Permite realizar operaciones CRUD
 * sobre la tabla Materiales mediante
 * Spring Data JPA e Hibernate.
 *
 * Operaciones:
 *
 * CREATE - Registrar materiales
 * READ   - Consultar materiales
 * UPDATE - Actualizar materiales
 * DELETE - Eliminar materiales
 *
 * Tabla MySQL: Materiales
 */

@Repository
public interface MaterialRepository
        extends JpaRepository<Material, Integer> {

}
