package com.perurecicla.repository;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PERU RECICLA - PASO 78
 *
 * Repositorio de canjes.
 *
 * CONSERVA:
 * - Historial por usuario.
 * - Busqueda por codigo de ticket.
 * - Actualizacion atomica del estado.
 *
 * NUEVO:
 * - Listado paginado por estado.
 * - Orden por fecha y codigo interno.
 * - Consulta de usuario y recompensa
 *   asociados al ticket.
 */
@Repository
public interface CanjeRepository
        extends JpaRepository<Canje, Integer> {

    // =====================================
    // HISTORIAL POR USUARIO
    // =====================================

    List<Canje> findByUsuario(
            Usuario usuario
    );

    // =====================================
    // BUSCAR POR CODIGO DE TICKET
    // =====================================

    @EntityGraph(
        attributePaths = {
            "usuario",
            "recompensa"
        }
    )
    @Query("""
        SELECT c
        FROM Canje c
        WHERE c.codigo_ticket = :codigo
        """)
    Optional<Canje> buscarPorCodigoTicket(
            @Param("codigo") String codigo
    );

    // =====================================
    // NUEVO - LISTAR CANJES POR ESTADO
    //
    // Permite obtener paginas de tickets
    // pendientes de entrega.
    //
    // Incluye los datos del vecino
    // y de la recompensa.
    // =====================================

    @EntityGraph(
        attributePaths = {
            "usuario",
            "recompensa"
        }
    )
    @Query(
        value = """
            SELECT c
            FROM Canje c
            WHERE c.estado = :estado
            ORDER BY
                c.fecha_canje DESC,
                c.id_canje DESC
            """,
        countQuery = """
            SELECT COUNT(c)
            FROM Canje c
            WHERE c.estado = :estado
            """
    )
    Page<Canje> listarPorEstadoOrdenado(
            @Param("estado")
            Canje.EstadoCanje estado,
            Pageable pageable
    );

    // =====================================
    // ACTUALIZACION ATOMICA DEL ESTADO
    //
    // SE CONSERVA SIN MODIFICACIONES.
    //
    // Impide confirmar dos veces una
    // misma entrega de recompensa.
    // =====================================

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
        UPDATE Canje c
        SET c.estado = :nuevoEstado
        WHERE c.id_canje = :id
          AND c.estado = :estadoActual
        """)
    int actualizarEstadoSiCoincide(
            @Param("id")
            Integer id,

            @Param("estadoActual")
            Canje.EstadoCanje estadoActual,

            @Param("nuevoEstado")
            Canje.EstadoCanje nuevoEstado
    );
}