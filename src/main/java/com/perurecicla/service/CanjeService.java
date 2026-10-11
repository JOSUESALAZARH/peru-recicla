
package com.perurecicla.service;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Recompensa;

import java.util.List;

/**
 * PERU RECICLA
 *
 * Servicio de Canjes
 *
 * Define las operaciones de negocio
 * relacionadas con los canjes de
 * recompensas realizados por los vecinos.
 *
 * Funcionalidades:
 *
 * 1. Realizar canjes de recompensas.
 * 2. Verificar puntos acumulados.
 * 3. Verificar stock disponible.
 * 4. Descontar puntos del vecino.
 * 5. Actualizar stock de recompensas.
 * 6. Registrar el canje en MySQL.
 * 7. Consultar recompensas disponibles.
 * 8. Consultar historial de canjes.
 *
 * Arquitectura:
 * Controller -> Service -> Repository
 */

public interface CanjeService {

    // =====================================
    // REALIZAR CANJE DE RECOMPENSA
    // =====================================

    Canje realizarCanje(
        Integer idUsuario,
        Integer idRecompensa
    );

    // =====================================
    // LISTAR RECOMPENSAS DISPONIBLES
    // =====================================

    List<Recompensa> listarPremiosDisponibles();

    // =====================================
    // CONSULTAR HISTORIAL DE CANJES
    // =====================================

    List<Canje> listarCanjesPorUsuario(
        Integer idUsuario
    );

}
