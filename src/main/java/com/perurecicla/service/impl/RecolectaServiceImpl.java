package com.perurecicla.service.impl;

import com.perurecicla.model.Material;
import com.perurecicla.model.Recolecta;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.MaterialRepository;
import com.perurecicla.repository.RecolectaRepository;
import com.perurecicla.repository.UsuarioRepository;

import com.perurecicla.service.RecolectaService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * PERU RECICLA - PASO 82
 *
 * Mantiene registros y puntos historicos.
 *
 * Conserva:
 * - Registrar recolectas
 * - Calcular y guardar puntos
 * - Aprobar o rechazar recolectas
 * - Acreditacion unica
 * - Historial y consultas
 *
 * Incorpora:
 * - Validacion de material activo
 * - Lectura protegida de la tarifa
 */
@Service
public class RecolectaServiceImpl
        implements RecolectaService {

    private static final BigDecimal LIMITE =
        new BigDecimal("99999999.99");

    private final RecolectaRepository recolectaRepository;

    private final UsuarioRepository usuarioRepository;

    private final MaterialRepository materialRepository;

    private final EntityManager entityManager;

    public RecolectaServiceImpl(
            RecolectaRepository recolectaRepository,
            UsuarioRepository usuarioRepository,
            MaterialRepository materialRepository,
            EntityManager entityManager) {

        this.recolectaRepository = recolectaRepository;
        this.usuarioRepository = usuarioRepository;
        this.materialRepository = materialRepository;
        this.entityManager = entityManager;
    }

    // =====================================
    // REGISTRAR RECOLECTA
    // =====================================

    @Override
    @Transactional
    public Recolecta registrarPeso(
            Recolecta recolecta) {

        if (recolecta == null) {

            throw new IllegalArgumentException(
                "Los datos de la recolecta son obligatorios"
            );
        }

        BigDecimal peso =
            recolecta.getPeso();

        if (peso == null
                || peso.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "El peso debe ser mayor a cero"
            );
        }

        peso = peso.setScale(
            2,
            RoundingMode.HALF_UP
        );

        if (peso.compareTo(LIMITE) > 0) {

            throw new IllegalArgumentException(
                "El peso excede el limite permitido"
            );
        }

        // =================================
        // BUSCAR VECINO
        // =================================

        if (recolecta.getVecino() == null
                || recolecta.getVecino()
                    .getId_usuario() == null) {

            throw new IllegalArgumentException(
                "Debe seleccionar un vecino"
            );
        }

        Usuario vecino =
            usuarioRepository
                .findById(
                    recolecta.getVecino()
                        .getId_usuario()
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El vecino no existe"
                    )
                );

        if (vecino.getRol()
                != Usuario.Rol.ROLE_vecino) {

            throw new IllegalArgumentException(
                "El usuario seleccionado no es vecino"
            );
        }

        // =================================
        // BUSCAR RECOLECTOR
        // =================================

        if (recolecta.getRecolector() == null
                || recolecta.getRecolector()
                    .getId_usuario() == null) {

            throw new IllegalArgumentException(
                "Debe seleccionar un recolector"
            );
        }

        Usuario recolector =
            usuarioRepository
                .findById(
                    recolecta.getRecolector()
                        .getId_usuario()
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El recolector no existe"
                    )
                );

        if (recolector.getRol()
                != Usuario.Rol.ROLE_recolector) {

            throw new IllegalArgumentException(
                "El usuario seleccionado no es recolector"
            );
        }

        // =================================
        // BUSCAR MATERIAL
        // =================================

        if (recolecta.getMaterial() == null
                || recolecta.getMaterial()
                    .getId_material() == null) {

            throw new IllegalArgumentException(
                "Debe seleccionar un material"
            );
        }

        Material material =
            materialRepository
                .findById(
                    recolecta.getMaterial()
                        .getId_material()
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El material no existe"
                    )
                );

        // =================================
        // PROTEGER LECTURA DE LA TARIFA
        // =================================

        entityManager.refresh(
            material,
            LockModeType.PESSIMISTIC_READ
        );

        // NUEVO: VALIDAR ESTADO ACTIVO

        if (material.getEstado()
                != Material.EstadoMaterial.activo) {

            throw new IllegalArgumentException(
                "El material esta inactivo "
                + "y no admite nuevos pesajes"
            );
        }

        // =================================
        // CALCULAR PUNTOS
        // =================================

        BigDecimal factor =
            material.getPuntos_por_gramo();

        if (factor == null
                || factor.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "El material no tiene una puntuacion valida"
            );
        }

        BigDecimal puntosGenerados =
            peso.multiply(factor)
                .setScale(
                    2,
                    RoundingMode.HALF_UP
                );

        if (puntosGenerados.compareTo(
                BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "El peso registrado no genera puntos"
            );
        }

        if (puntosGenerados.compareTo(LIMITE) > 0) {

            throw new IllegalArgumentException(
                "Los puntos exceden el limite permitido"
            );
        }

        // =================================
        // REGISTRAR VALOR HISTORICO
        // =================================

        recolecta.setId_recolecta(null);

        recolecta.setVecino(
            vecino
        );

        recolecta.setRecolector(
            recolector
        );

        recolecta.setMaterial(
            material
        );

        recolecta.setPeso(
            peso
        );

        // Este valor permanece guardado
        // aunque cambie la tarifa.

        recolecta.setPuntos_generados(
            puntosGenerados
        );

        recolecta.setFecha_recolecta(
            LocalDateTime.now()
        );

        recolecta.setEstado_validacion(
            Recolecta.EstadoValidacion.pendiente
        );

        return recolectaRepository.save(
            recolecta
        );
    }

    // =====================================
    // VALIDAR O RECHAZAR RECOLECTA
    // =====================================

    @Override
    @Transactional
    public void validarRecolecta(
            Integer idRecolecta,
            String estado) {

        if (idRecolecta == null
                || idRecolecta <= 0) {

            throw new IllegalArgumentException(
                "El ID de recolecta no es valido"
            );
        }

        if (estado == null || estado.isBlank()) {

            throw new IllegalArgumentException(
                "Debe indicar el estado de validacion"
            );
        }

        String nuevoEstado =
            estado.strip().toLowerCase(
                Locale.ROOT
            );

        if (!nuevoEstado.equals("validado")
                && !nuevoEstado.equals("rechazado")) {

            throw new IllegalArgumentException(
                "El estado debe ser validado o rechazado"
            );
        }

        // Bloqueo existente del PASO 61.

        Recolecta recolecta =
            recolectaRepository
                .buscarPorIdParaValidacion(
                    idRecolecta
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "La recolecta no existe"
                    )
                );

        if (recolecta.getEstado_validacion()
                != Recolecta.EstadoValidacion.pendiente) {

            throw new IllegalStateException(
                "La recolecta ya fue procesada"
            );
        }

        if (nuevoEstado.equals("validado")) {

            // =============================
            // APROBAR Y ACREDITAR
            // =============================

            if (recolecta.getVecino() == null
                    || recolecta.getVecino()
                        .getId_usuario() == null) {

                throw new IllegalStateException(
                    "La recolecta no tiene un vecino valido"
                );
            }

            Usuario vecino =
                usuarioRepository
                    .findById(
                        recolecta.getVecino()
                            .getId_usuario()
                    )
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "El vecino no existe"
                        )
                    );

            entityManager.refresh(
                vecino,
                LockModeType.PESSIMISTIC_WRITE
            );

            if (vecino.getRol()
                    != Usuario.Rol.ROLE_vecino
                    || vecino.getEstado()
                    != Usuario.Estado.activo) {

                throw new IllegalStateException(
                    "El vecino debe estar activo"
                );
            }

            // Tomar los puntos guardados,
            // NO la tarifa actual del material.

            BigDecimal puntosGenerados =
                recolecta.getPuntos_generados();

            if (puntosGenerados == null
                    || puntosGenerados
                        .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalStateException(
                    "La recolecta no tiene puntos validos"
                );
            }

            BigDecimal saldoActual =
                vecino.getPuntos_acumulados() == null
                    ? BigDecimal.ZERO
                    : vecino.getPuntos_acumulados();

            BigDecimal nuevoSaldo =
                saldoActual.add(
                    puntosGenerados
                );

            if (nuevoSaldo.compareTo(LIMITE) > 0) {

                throw new IllegalStateException(
                    "El saldo supera el limite permitido"
                );
            }

            vecino.setPuntos_acumulados(
                nuevoSaldo
            );

            recolecta.setEstado_validacion(
                Recolecta.EstadoValidacion.validado
            );

            usuarioRepository.save(
                vecino
            );

        } else {

            // =============================
            // RECHAZAR RECOLECTA
            // =============================

            recolecta.setEstado_validacion(
                Recolecta.EstadoValidacion.rechazado
            );
        }

        recolectaRepository.save(
            recolecta
        );
    }

    // =====================================
    // LISTAR TODAS
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Recolecta> listarTodas() {

        return recolectaRepository.findAll();
    }

    // =====================================
    // LISTAR PENDIENTES
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Recolecta> listarPendientes() {

        return recolectaRepository.buscarPorEstado(
            Recolecta.EstadoValidacion.pendiente
        );
    }

    // =====================================
    // HISTORIAL DEL VECINO
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Recolecta> listarHistorialPorVecino(
            Integer idUsuario) {

        if (idUsuario == null
                || idUsuario <= 0) {

            throw new IllegalArgumentException(
                "El ID del usuario no es valido"
            );
        }

        Usuario vecino =
            usuarioRepository
                .findById(idUsuario)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El vecino no existe"
                    )
                );

        return recolectaRepository.findByVecino(
            vecino
        );
    }
}