
package com.perurecicla.service.impl;

import com.perurecicla.model.Recompensa;
import com.perurecicla.repository.RecompensaRepository;
import com.perurecicla.service.RecompensaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PERU RECICLA
 *
 * Implementacion del servicio de recompensas.
 *
 * Funcionalidades:
 *
 * 1. Registrar recompensas.
 * 2. Consultar todas las recompensas.
 * 3. Buscar recompensas por ID.
 * 4. Actualizar recompensas.
 * 5. Eliminar recompensas.
 * 6. Consultar recompensas activas.
 *
 * Arquitectura:
 * Controller -> Service -> Repository -> MySQL
 */

@Service
public class RecompensaServiceImpl
        implements RecompensaService {

    // =====================================
    // REPOSITORIO DE RECOMPENSAS
    // =====================================

    private final RecompensaRepository recompensaRepository;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public RecompensaServiceImpl(
            RecompensaRepository recompensaRepository) {

        this.recompensaRepository = recompensaRepository;
    }

    // =====================================
    // READ - LISTAR TODAS LAS RECOMPENSAS
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Recompensa> listarTodas() {

        return recompensaRepository.findAll();
    }

    // =====================================
    // CREATE Y UPDATE - GUARDAR RECOMPENSA
    // =====================================

    @Override
    @Transactional
    public void guardar(Recompensa recompensa) {

        // Validar datos recibidos

        validarRecompensa(recompensa);

        // =================================
        // CREATE - NUEVA RECOMPENSA
        // =================================

        if (recompensa.getId_recompensa() == null) {

            // Establecer estado inicial

            if (recompensa.getEstado() == null) {

                recompensa.setEstado(
                    Recompensa.EstadoRecompensa.activo
                );
            }

            // Limpiar nombre

            recompensa.setNombre(
                recompensa.getNombre().trim()
            );

            // Guardar en MySQL

            recompensaRepository.save(recompensa);

            return;
        }

        // =================================
        // UPDATE - EDITAR RECOMPENSA
        // =================================

        Integer id = recompensa.getId_recompensa();

        if (id <= 0) {

            throw new IllegalArgumentException(
                "El ID de recompensa no es valido"
            );
        }

        // Buscar registro existente

        Recompensa recompensaExistente =
            recompensaRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "No se encontro la recompensa con ID: "
                        + id
                    )
                );

        // Actualizar nombre

        recompensaExistente.setNombre(
            recompensa.getNombre().trim()
        );

        // Actualizar categoria

        recompensaExistente.setCategoria(
            recompensa.getCategoria()
        );

        // Actualizar descripcion

        recompensaExistente.setDescripcion(
            recompensa.getDescripcion()
        );

        // Actualizar costo en puntos

        recompensaExistente.setCosto_puntos(
            recompensa.getCosto_puntos()
        );

        // Actualizar stock

        recompensaExistente.setStock(
            recompensa.getStock()
        );

        // Actualizar imagen

        recompensaExistente.setUrl_imagen(
            recompensa.getUrl_imagen()
        );

        // Actualizar estado, si fue enviado

        if (recompensa.getEstado() != null) {

            recompensaExistente.setEstado(
                recompensa.getEstado()
            );
        }

        // Guardar cambios manteniendo el ID

        recompensaRepository.save(
            recompensaExistente
        );
    }

    // =====================================
    // READ - BUSCAR RECOMPENSA POR ID
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public Recompensa buscarPorId(Integer id) {

        if (id == null || id <= 0) {

            return null;
        }

        return recompensaRepository
                .findById(id)
                .orElse(null);
    }

    // =====================================
    // DELETE - ELIMINAR RECOMPENSA
    // =====================================

    @Override
    @Transactional
    public void eliminar(Integer id) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                "El ID de recompensa no es valido"
            );
        }

        // Verificar que exista

        Recompensa recompensaExistente =
            recompensaRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "No se encontro la recompensa con ID: "
                        + id
                    )
                );

        // Eliminar recompensa
        // MySQL impide eliminar registros
        // vinculados a canjes existentes.

        recompensaRepository.delete(
            recompensaExistente
        );

        recompensaRepository.flush();
    }

    // =====================================
    // CONSULTAR RECOMPENSAS ACTIVAS
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Recompensa> listarActivos() {

        return recompensaRepository.findByEstado(
            Recompensa.EstadoRecompensa.activo
        );
    }

    // =====================================
    // VALIDACIONES DE RECOMPENSA
    // =====================================

    private void validarRecompensa(
            Recompensa recompensa) {

        // Verificar objeto

        if (recompensa == null) {

            throw new IllegalArgumentException(
                "Los datos de la recompensa son obligatorios"
            );
        }

        // =================================
        // VALIDAR NOMBRE
        // =================================

        if (recompensa.getNombre() == null ||
            recompensa.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                "El nombre de la recompensa es obligatorio"
            );
        }

        if (recompensa.getNombre().trim().length() > 100) {

            throw new IllegalArgumentException(
                "El nombre no puede superar 100 caracteres"
            );
        }

        // =================================
        // VALIDAR CATEGORIA
        // =================================

        if (recompensa.getCategoria() == null) {

            throw new IllegalArgumentException(
                "Debe seleccionar una categoria"
            );
        }

        // =================================
        // VALIDAR COSTO EN PUNTOS
        // =================================

        Integer costo = recompensa.getCosto_puntos();

        if (costo == null || costo < 0) {

            throw new IllegalArgumentException(
                "El costo en puntos no puede ser negativo"
            );
        }

        // =================================
        // VALIDAR STOCK
        // =================================

        Integer stock = recompensa.getStock();

        if (stock == null || stock < 0) {

            throw new IllegalArgumentException(
                "El stock no puede ser negativo"
            );
        }

        // =================================
        // VALIDAR URL DE IMAGEN
        // =================================

        if (recompensa.getUrl_imagen() != null &&
            recompensa.getUrl_imagen().length() > 255) {

            throw new IllegalArgumentException(
                "La URL de imagen es demasiado larga"
            );
        }
    }
}
