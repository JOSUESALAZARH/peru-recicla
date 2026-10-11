
package com.perurecicla.repository;

import com.perurecicla.model.Recolecta;
import com.perurecicla.model.Usuario;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PERU RECICLA - PASO 66
 *
 * Repositorio de recolectas.
 *
 * CONSERVA:
 * - Historial por vecino
 * - Consultas por estado
 * - Bloqueo de validacion
 *
 * NUEVO:
 * - Historial paginado del recolector
 * - Busqueda por ID
 * - Filtros en MySQL
 * - Resumen estadistico
 */
@Repository
public interface RecolectaRepository
        extends JpaRepository<Recolecta, Integer> {

    // =====================================
    // HISTORIAL POR VECINO
    // EXISTENTE
    // =====================================

    List<Recolecta> findByVecino(Usuario vecino);

    // =====================================
    // RECOLECTAS POR ESTADO
    // EXISTENTE
    // =====================================

    @Query(
        "SELECT r FROM Recolecta r " +
        "WHERE r.estado_validacion = :estado"
    )
    List<Recolecta> buscarPorEstado(
        @Param("estado")
        Recolecta.EstadoValidacion estado
    );

    // =====================================
    // BLOQUEO PARA VALIDACION
    // EXISTENTE - PASO 61
    // NO ELIMINAR
    // =====================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT r FROM Recolecta r " +
        "WHERE r.id_recolecta = :id"
    )
    Optional<Recolecta> buscarPorIdParaValidacion(
        @Param("id") Integer id
    );

    // =====================================
    // PASO 66
    // CONTAR TODAS LAS RECOLECTAS
    // DE UN RECOLECTOR
    // =====================================

    @Query("""
        SELECT COUNT(r)
        FROM Recolecta r
        WHERE r.recolector.id_usuario = :idRecolector
        """)
    long contarHistorialPorRecolector(
        @Param("idRecolector")
        Integer idRecolector
    );

    // =====================================
    // PASO 66
    // BUSQUEDA PAGINADA CON FILTROS
    //
    // Los filtros se aplican en MySQL.
    //
    // Las relaciones con vecino y material
    // se recuperan en la misma consulta.
    //
    // El countQuery permite que Spring Data
    // calcule el numero total de paginas.
    // =====================================

    @Query(
        value = """
            SELECT r
            FROM Recolecta r
            LEFT JOIN FETCH r.vecino
            LEFT JOIN FETCH r.material
            WHERE r.recolector.id_usuario = :idRecolector
              AND (
                  :estado IS NULL
                  OR r.estado_validacion = :estado
              )
              AND (
                  :materialId IS NULL
                  OR r.material.id_material = :materialId
              )
              AND (
                  :idBusqueda IS NULL
                  OR r.id_recolecta = :idBusqueda
              )
              AND (
                  :usarDesde = false
                  OR r.fecha_recolecta >= :desde
              )
              AND (
                  :usarHasta = false
                  OR r.fecha_recolecta < :hastaExclusivo
              )
            ORDER BY
                r.fecha_recolecta DESC,
                r.id_recolecta DESC
            """,

        countQuery = """
            SELECT COUNT(r)
            FROM Recolecta r
            WHERE r.recolector.id_usuario = :idRecolector
              AND (
                  :estado IS NULL
                  OR r.estado_validacion = :estado
              )
              AND (
                  :materialId IS NULL
                  OR r.material.id_material = :materialId
              )
              AND (
                  :idBusqueda IS NULL
                  OR r.id_recolecta = :idBusqueda
              )
              AND (
                  :usarDesde = false
                  OR r.fecha_recolecta >= :desde
              )
              AND (
                  :usarHasta = false
                  OR r.fecha_recolecta < :hastaExclusivo
              )
            """
    )
    Page<Recolecta> buscarHistorialPaginado(

        @Param("idRecolector")
        Integer idRecolector,

        @Param("estado")
        Recolecta.EstadoValidacion estado,

        @Param("materialId")
        Integer materialId,

        @Param("idBusqueda")
        Integer idBusqueda,

        @Param("usarDesde")
        boolean usarDesde,

        @Param("desde")
        LocalDateTime desde,

        @Param("usarHasta")
        boolean usarHasta,

        @Param("hastaExclusivo")
        LocalDateTime hastaExclusivo,

        Pageable pageable
    );

    // =====================================
    // PASO 66
    // ESTADISTICAS DE TODOS LOS RESULTADOS
    //
    // No se limita a los 10 o 20 registros
    // visibles en la pagina.
    //
    // Resultado:
    // [0] pendientes
    // [1] validadas
    // [2] rechazadas
    // [3] peso total
    // [4] puntos acreditados
    // =====================================

    @Query("""
        SELECT
            COUNT(
                CASE
                    WHEN r.estado_validacion = :pendiente
                    THEN r.id_recolecta
                    ELSE NULL
                END
            ),

            COUNT(
                CASE
                    WHEN r.estado_validacion = :validado
                    THEN r.id_recolecta
                    ELSE NULL
                END
            ),

            COUNT(
                CASE
                    WHEN r.estado_validacion = :rechazado
                    THEN r.id_recolecta
                    ELSE NULL
                END
            ),

            COALESCE(
                SUM(r.peso),
                0
            ),

            COALESCE(
                SUM(
                    CASE
                        WHEN r.estado_validacion = :validado
                        THEN r.puntos_generados
                        ELSE NULL
                    END
                ),
                0
            )

        FROM Recolecta r

        WHERE r.recolector.id_usuario = :idRecolector

          AND (
              :estado IS NULL
              OR r.estado_validacion = :estado
          )

          AND (
              :materialId IS NULL
              OR r.material.id_material = :materialId
          )

          AND (
              :idBusqueda IS NULL
              OR r.id_recolecta = :idBusqueda
          )

          AND (
              :usarDesde = false
              OR r.fecha_recolecta >= :desde
          )

          AND (
              :usarHasta = false
              OR r.fecha_recolecta < :hastaExclusivo
          )
        """)
    List<Object[]> resumirHistorialRecolector(

        @Param("idRecolector")
        Integer idRecolector,

        @Param("estado")
        Recolecta.EstadoValidacion estado,

        @Param("materialId")
        Integer materialId,

        @Param("idBusqueda")
        Integer idBusqueda,

        @Param("usarDesde")
        boolean usarDesde,

        @Param("desde")
        LocalDateTime desde,

        @Param("usarHasta")
        boolean usarHasta,

        @Param("hastaExclusivo")
        LocalDateTime hastaExclusivo,

        @Param("pendiente")
        Recolecta.EstadoValidacion pendiente,

        @Param("validado")
        Recolecta.EstadoValidacion validado,

        @Param("rechazado")
        Recolecta.EstadoValidacion rechazado
    );
}
