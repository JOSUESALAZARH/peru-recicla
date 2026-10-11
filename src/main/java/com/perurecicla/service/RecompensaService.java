
package com.perurecicla.service;

import com.perurecicla.model.Recompensa;

import java.util.List;

/**
 * PERU RECICLA
 *
 * Servicio de Recompensas
 *
 * Define las operaciones de negocio
 * relacionadas con el mantenimiento
 * de recompensas disponibles para
 * los vecinos del sistema.
 *
 * Funcionalidades:
 *
 * 1. Listar todas las recompensas.
 * 2. Registrar nuevas recompensas.
 * 3. Buscar recompensas por ID.
 * 4. Actualizar recompensas existentes.
 * 5. Eliminar recompensas.
 * 6. Consultar recompensas activas.
 *
 * Arquitectura:
 * Controller -> Service -> Repository
 */

public interface RecompensaService {

    // =====================================
    // READ - LISTAR TODAS LAS RECOMPENSAS
    // =====================================

    List<Recompensa> listarTodas();

    // =====================================
    // CREATE Y UPDATE - GUARDAR RECOMPENSA
    // =====================================

    void guardar(Recompensa recompensa);

    // =====================================
    // READ - BUSCAR RECOMPENSA POR ID
    // =====================================

    Recompensa buscarPorId(Integer id);

    // =====================================
    // DELETE - ELIMINAR RECOMPENSA
    // =====================================

    void eliminar(Integer id);

    // =====================================
    // CONSULTAR RECOMPENSAS ACTIVAS
    // =====================================

    List<Recompensa> listarActivos();

}
