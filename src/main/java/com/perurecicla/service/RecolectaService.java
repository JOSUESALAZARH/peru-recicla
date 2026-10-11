
package com.perurecicla.service;

import com.perurecicla.model.Recolecta;

import java.util.List;

/**
 * PERU RECICLA
 *
 * Servicio de Recolectas
 *
 * Define las operaciones de negocio
 * relacionadas con el registro,
 * validacion y consulta de materiales
 * reciclados por los vecinos.
 *
 * Funcionalidades:
 *
 * 1. Registrar una recolecta.
 * 2. Calcular los puntos generados.
 * 3. Validar o rechazar recolectas.
 * 4. Listar todas las recolectas.
 * 5. Consultar recolectas pendientes.
 * 6. Consultar historial por vecino.
 *
 * Arquitectura:
 * Controller -> Service -> Repository
 */

public interface RecolectaService {

    // =====================================
    // REGISTRAR PESO DE MATERIAL RECICLADO
    // =====================================

    Recolecta registrarPeso(
        Recolecta recolecta
    );

    // =====================================
    // VALIDAR O RECHAZAR RECOLECTA
    // =====================================

    void validarRecolecta(
        Integer idRecolecta,
        String estado
    );

    // =====================================
    // LISTAR TODAS LAS RECOLECTAS
    // =====================================

    List<Recolecta> listarTodas();

    // =====================================
    // LISTAR RECOLECTAS PENDIENTES
    // =====================================

    List<Recolecta> listarPendientes();

    // =====================================
    // CONSULTAR HISTORIAL POR VECINO
    // =====================================

    List<Recolecta> listarHistorialPorVecino(
        Integer idUsuario
    );

}
