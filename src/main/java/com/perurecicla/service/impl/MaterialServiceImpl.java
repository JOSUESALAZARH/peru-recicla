
package com.perurecicla.service.impl;

import com.perurecicla.model.Material;
import com.perurecicla.repository.MaterialRepository;
import com.perurecicla.service.MaterialService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * PERU RECICLA
 *
 * Implementacion del mantenimiento
 * completo de materiales reciclables.
 *
 * CREATE - Registrar materiales
 * READ   - Consultar materiales
 * UPDATE - Editar materiales
 * DELETE - Eliminar materiales
 *
 * Arquitectura:
 * Controller -> Service -> Repository -> MySQL
 */

@Service
public class MaterialServiceImpl
        implements MaterialService {

    // =====================================
    // REPOSITORIO DE MATERIALES
    // =====================================

    private final MaterialRepository materialRepository;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public MaterialServiceImpl(
            MaterialRepository materialRepository) {

        this.materialRepository = materialRepository;
    }

    // =====================================
    // READ - LISTAR TODOS LOS MATERIALES
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Material> listarTodos() {

        return materialRepository.findAll();
    }

    // =====================================
    // READ - BUSCAR MATERIAL POR ID
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public Material buscarPorId(Integer id) {

        if (id == null || id <= 0) {
            return null;
        }

        return materialRepository
                .findById(id)
                .orElse(null);
    }

    // =====================================
    // CREATE - REGISTRAR NUEVO MATERIAL
    // =====================================

    @Override
    @Transactional
    public void guardar(Material material) {

        // Validar los datos recibidos

        validarMaterial(material);

        // El registro nuevo no debe tener ID

        if (material.getId_material() != null) {

            throw new IllegalArgumentException(
                "Para editar un material utilice actualizar()"
            );
        }

        // Limpiar el nombre del material

        material.setTipo_material(
            material.getTipo_material().trim()
        );

        // Registrar material en MySQL

        materialRepository.save(material);
    }

    // =====================================
    // UPDATE - ACTUALIZAR MATERIAL
    // =====================================

    @Override
    @Transactional
    public Material actualizar(
            Integer id,
            Material datosActualizados) {

        // Validar el identificador

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                "El ID del material no es valido"
            );
        }

        // Validar nuevos datos

        validarMaterial(datosActualizados);

        // Buscar el registro original

        Material materialExistente =
            materialRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "No se encontro el material con ID: "
                        + id
                    )
                );

        // Actualizar nombre

        materialExistente.setTipo_material(
            datosActualizados
                .getTipo_material()
                .trim()
        );

        // Actualizar puntos por gramo

        materialExistente.setPuntos_por_gramo(
            datosActualizados.getPuntos_por_gramo()
        );

        // Actualizar descripcion

        materialExistente.setDescripcion(
            datosActualizados.getDescripcion()
        );

        // Actualizar imagen

        materialExistente.setUrl_imagen(
            datosActualizados.getUrl_imagen()
        );

        // Guardar cambios sin alterar el ID

        return materialRepository.save(
            materialExistente
        );
    }

    // =====================================
    // DELETE - ELIMINAR MATERIAL
    // =====================================

    @Override
    @Transactional
    public void eliminar(Integer id) {

        // Validar identificador

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                "El ID del material no es valido"
            );
        }

        // Verificar que exista

        Material materialExistente =
            materialRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "No se encontro el material con ID: "
                        + id
                    )
                );

        // Eliminar registro
        // MySQL protege los materiales
        // relacionados con recolectas.

        materialRepository.delete(
            materialExistente
        );
    }

    // =====================================
    // VALIDAR DATOS DEL MATERIAL
    // =====================================

    private void validarMaterial(Material material) {

        if (material == null) {

            throw new IllegalArgumentException(
                "Los datos del material son obligatorios"
            );
        }

        // Validar tipo de material

        if (material.getTipo_material() == null ||
            material.getTipo_material().isBlank()) {

            throw new IllegalArgumentException(
                "El nombre del material es obligatorio"
            );
        }

        if (material.getTipo_material().trim().length() > 50) {

            throw new IllegalArgumentException(
                "El nombre no puede superar 50 caracteres"
            );
        }

        // Validar puntos por gramo

        BigDecimal puntos =
            material.getPuntos_por_gramo();

        if (puntos == null ||
            puntos.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "Los puntos por gramo deben ser mayores a cero"
            );
        }

        if (puntos.scale() > 4 ||
            puntos.precision() - puntos.scale() > 6) {

            throw new IllegalArgumentException(
                "Los puntos deben ajustarse al formato DECIMAL(10,4)"
            );
        }

        // Validar longitud de imagen

        if (material.getUrl_imagen() != null &&
            material.getUrl_imagen().length() > 255) {

            throw new IllegalArgumentException(
                "La URL de imagen es demasiado larga"
            );
        }
    }
}
