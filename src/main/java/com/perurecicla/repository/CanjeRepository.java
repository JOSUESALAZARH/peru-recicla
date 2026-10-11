
package com.perurecicla.repository;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PERU RECICLA - PASO 50
 *
 * Repositorio de canjes.
 *
 * Conserva los metodos existentes e incorpora
 * una actualizacion condicional y atomica.
 */
@Repository
public interface CanjeRepository
        extends JpaRepository<Canje, Integer> {

    // HISTORIAL POR USUARIO
    List<Canje> findByUsuario(Usuario usuario);

    // BUSCAR POR CODIGO DE TICKET
    @Query("""
        SELECT c FROM Canje c
        WHERE c.codigo_ticket = :codigo
        """)
    Optional<Canje> buscarPorCodigoTicket(
            @Param("codigo") String codigo
    );

    // ACTUALIZAR SOLAMENTE SI SIGUE PENDIENTE
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
            @Param("id") Integer id,
            @Param("estadoActual")
            Canje.EstadoCanje estadoActual,
            @Param("nuevoEstado")
            Canje.EstadoCanje nuevoEstado
    );
}
