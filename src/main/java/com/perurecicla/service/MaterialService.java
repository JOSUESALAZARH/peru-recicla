
package com.perurecicla.service;

import com.perurecicla.model.Material;
import java.util.List;

/**
 * PERU RECICLA
 *
 * Servicio de Materiales
 *
 * Define las operaciones de negocio
 * para el mantenimiento de los
 * materiales reciclables.
 *
 * Operaciones CRUD:
 *
 * CREATE - Registrar materiales
 * READ   - Consultar materiales
 * UPDATE - Editar materiales existentes
 * DELETE - Eliminar materiales
 *
 * Arquitectura:
 * Controller -> Service -> Repository
 */

public interface MaterialService {

    // =====================================
    // READ - LISTAR TODOS LOS MATERIALES
    // =====================================

    List<Material> listarTodos();

    // =====================================
    // READ - BUSCAR MATERIAL POR ID
    // =====================================

    Material buscarPorId(Integer id);

    // =====================================
    // CREATE - REGISTRAR MATERIAL
    // =====================================

    void guardar(Material material);

    // =====================================
    // UPDATE - ACTUALIZAR MATERIAL
    // =====================================

    Material actualizar(
        Integer id,
        Material datosActualizados
    );

    // =====================================
    // DELETE - ELIMINAR MATERIAL
    // =====================================

    void eliminar(Integer id);

}
