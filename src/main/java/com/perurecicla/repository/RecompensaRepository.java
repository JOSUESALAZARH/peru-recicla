
package com.perurecicla.repository;

import com.perurecicla.model.Recompensa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PERU RECICLA
 *
 * Repositorio de Recompensas
 *
 * Permite administrar las recompensas
 * disponibles para los vecinos mediante
 * Spring Data JPA e Hibernate.
 *
 * Funcionalidades:
 *
 * 1. Registrar nuevas recompensas.
 * 2. Consultar todas las recompensas.
 * 3. Buscar recompensas por ID.
 * 4. Actualizar recompensas existentes.
 * 5. Eliminar recompensas.
 * 6. Consultar recompensas por estado.
 * 7. Consultar recompensas activas
 *    que tengan stock disponible.
 *
 * Tabla MySQL: Recompensas
 */

@Repository
public interface RecompensaRepository
        extends JpaRepository<Recompensa, Integer> {

    // =====================================
    // CONSULTAR RECOMPENSAS POR ESTADO
    // =====================================

    List<Recompensa> findByEstado(
        Recompensa.EstadoRecompensa estado
    );

    // =====================================
    // CONSULTAR POR ESTADO Y STOCK
    // =====================================

    List<Recompensa> findByEstadoAndStockGreaterThan(
        Recompensa.EstadoRecompensa estado,
        Integer stock
    );

    // =====================================
    // RECOMPENSAS DISPONIBLES PARA CANJE
    // =====================================

    default List<Recompensa> listarDisponiblesParaCanje() {

        return findByEstadoAndStockGreaterThan(
            Recompensa.EstadoRecompensa.activo,
            0
        );

    }

}
