package com.perurecicla.service.impl;

import com.perurecicla.model.Material;
import com.perurecicla.repository.MaterialRepository;
import com.perurecicla.service.MaterialService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * PERU RECICLA - PASO 82
 *
 * Mantenimiento de materiales reciclables.
 *
 * CREATE - Registrar materiales
 * READ   - Consultar materiales
 * UPDATE - Editar materiales
 * DELETE - Desactivar sin borrar historial
 *
 * Los cambios de puntos por gramo solo
 * afectan los pesajes posteriores.
 */
@Service
public class MaterialServiceImpl
        implements MaterialService {

    private final MaterialRepository materialRepository;

    private final EntityManager entityManager;

    public MaterialServiceImpl(
            MaterialRepository materialRepository,
            EntityManager entityManager) {

        this.materialRepository = materialRepository;
        this.entityManager = entityManager;
    }

    // =====================================
    // LISTAR TODOS LOS MATERIALES
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Material> listarTodos() {

        return materialRepository.findAll();
    }

    // =====================================
    // BUSCAR POR ID
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
    // REGISTRAR MATERIAL NUEVO
    // =====================================

    @Override
    @Transactional
    public void guardar(Material material) {

        validarMaterial(material);

        if (material.getId_material() != null) {

            throw new IllegalArgumentException(
                "Para editar un material utilice actualizar()"
            );
        }

        material.setTipo_material(
            material.getTipo_material().trim()
        );

        materialRepository.save(material);
    }

    // =====================================
    // ACTUALIZAR MATERIAL
    // =====================================

    @Override
    @Transactional
    public Material actualizar(
            Integer id,
            Material datosActualizados) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                "El ID del material no es valido"
            );
        }

        validarMaterial(datosActualizados);

        Material materialExistente =
            materialRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "No se encontro el material con ID: "
                        + id
                    )
                );

        // Bloquear el registro mientras
        // se modifican sus datos y tarifa.

        entityManager.refresh(
            materialExistente,
            LockModeType.PESSIMISTIC_WRITE
        );

        materialExistente.setTipo_material(
            datosActualizados
                .getTipo_material()
                .trim()
        );

        materialExistente.setPuntos_por_gramo(
            datosActualizados.getPuntos_por_gramo()
        );

        materialExistente.setDescripcion(
            datosActualizados.getDescripcion()
        );

        materialExistente.setUrl_imagen(
            datosActualizados.getUrl_imagen()
        );

        materialExistente.setEstado(
            datosActualizados.getEstado()
        );

        return materialRepository.save(
            materialExistente
        );
    }

    // =====================================
    // BAJA LOGICA, SIN BORRAR HISTORIAL
    // =====================================

    @Override
    @Transactional
    public void eliminar(Integer id) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                "El ID del material no es valido"
            );
        }

        Material materialExistente =
            materialRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "No se encontro el material con ID: "
                        + id
                    )
                );

        // Nunca eliminar fisicamente
        // un material historico.

        entityManager.refresh(
            materialExistente,
            LockModeType.PESSIMISTIC_WRITE
        );

        materialExistente.setEstado(
            Material.EstadoMaterial.inactivo
        );

        materialRepository.save(
            materialExistente
        );
    }

    // =====================================
    // VALIDACIONES DE LOS DATOS
    // =====================================

    private void validarMaterial(Material material) {

        if (material == null) {

            throw new IllegalArgumentException(
                "Los datos del material son obligatorios"
            );
        }

        if (material.getTipo_material() == null
                || material.getTipo_material().isBlank()) {

            throw new IllegalArgumentException(
                "El nombre del material es obligatorio"
            );
        }

        if (material.getTipo_material().trim().length() > 50) {

            throw new IllegalArgumentException(
                "El nombre no puede superar 50 caracteres"
            );
        }

        BigDecimal puntos =
            material.getPuntos_por_gramo();

        if (puntos == null
                || puntos.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "Los puntos por gramo deben ser mayores a cero"
            );
        }

        if (puntos.scale() > 4
                || puntos.precision() - puntos.scale() > 6) {

            throw new IllegalArgumentException(
                "Los puntos deben ajustarse al formato DECIMAL(10,4)"
            );
        }

        if (material.getEstado() == null) {

            throw new IllegalArgumentException(
                "Selecciona un estado valido"
            );
        }

        if (material.getUrl_imagen() != null
                && material.getUrl_imagen().length() > 255) {

            throw new IllegalArgumentException(
                "La URL de imagen es demasiado larga"
            );
        }
    }
}