package com.perurecicla.repository;

import com.perurecicla.model.Material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PERU RECICLA - PASO 82
 *
 * Consultas de materiales reciclables.
 *
 * Permite consultar todos, filtrar
 * por estado y contar materiales activos.
 */
@Repository
public interface MaterialRepository
        extends JpaRepository<Material, Integer> {

    List<Material> findByEstado(
        Material.EstadoMaterial estado
    );

    long countByEstado(
        Material.EstadoMaterial estado
    );
}