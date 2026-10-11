package com.perurecicla.service.impl;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.CanjeRepository;
import com.perurecicla.repository.RecompensaRepository;
import com.perurecicla.repository.UsuarioRepository;

import com.perurecicla.service.CanjeService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PERU RECICLA - PASO 76
 *
 * Servicio de canjes.
 *
 * CONSERVA:
 * - Validacion del vecino
 * - Validacion de la recompensa
 * - Comprobacion de puntos
 * - Verificacion de stock
 * - Descuento de puntos
 * - Actualizacion de stock
 * - Generacion del ticket
 * - Registro en MySQL
 * - Historial de canjes
 *
 * MEJORA:
 * - Bloqueo de saldo del vecino
 * - Bloqueo de stock de recompensa
 * - Lectura actualizada antes del descuento
 * - Proteccion de operaciones simultaneas
 *
 * No modifica la estructura de las tablas.
 */
@Service
public class CanjeServiceImpl
        implements CanjeService {

    // =====================================
    // REPOSITORIOS
    // =====================================

    private final CanjeRepository canjeRepository;

    private final UsuarioRepository usuarioRepository;

    private final RecompensaRepository recompensaRepository;

    // =====================================
    // ENTITY MANAGER
    // =====================================

    private final EntityManager entityManager;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public CanjeServiceImpl(
            CanjeRepository canjeRepository,
            UsuarioRepository usuarioRepository,
            RecompensaRepository recompensaRepository,
            EntityManager entityManager) {

        this.canjeRepository =
                canjeRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.recompensaRepository =
                recompensaRepository;

        this.entityManager =
                entityManager;
    }

    // =====================================
    // REALIZAR CANJE
    // =====================================

    @Override
    @Transactional
    public Canje realizarCanje(
            Integer idUsuario,
            Integer idRecompensa) {

        // =================================
        // 1. VALIDAR IDENTIFICADORES
        // =================================

        if (idUsuario == null
                || idUsuario <= 0) {

            throw new IllegalArgumentException(
                "El ID del usuario no es valido"
            );
        }

        if (idRecompensa == null
                || idRecompensa <= 0) {

            throw new IllegalArgumentException(
                "El ID de la recompensa no es valido"
            );
        }

        // =================================
        // 2. BUSCAR USUARIO
        // =================================

        Usuario usuario =
            usuarioRepository
                .findById(idUsuario)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El usuario no existe"
                    )
                );

        // =================================
        // 3. BLOQUEAR EL SALDO DEL USUARIO
        //
        // Evita que dos canjes simultaneos
        // calculen usando el mismo saldo.
        //
        // refresh actualiza los datos y
        // solicita bloqueo de escritura.
        // =================================

        entityManager.refresh(
            usuario,
            LockModeType.PESSIMISTIC_WRITE
        );

        // =================================
        // 4. VALIDAR ROL Y ESTADO
        // =================================

        if (usuario.getRol()
                != Usuario.Rol.ROLE_vecino) {

            throw new IllegalArgumentException(
                "Solo los vecinos pueden "
                + "realizar canjes"
            );
        }

        if (usuario.getEstado()
                != Usuario.Estado.activo) {

            throw new IllegalStateException(
                "El usuario no se encuentra activo"
            );
        }

        // =================================
        // 5. BUSCAR RECOMPENSA
        // =================================

        Recompensa recompensa =
            recompensaRepository
                .findById(idRecompensa)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "La recompensa no existe"
                    )
                );

        // =================================
        // 6. BLOQUEAR STOCK
        //
        // Dos vecinos no deben descontar
        // unidades tomando el mismo stock
        // inicial de una recompensa.
        // =================================

        entityManager.refresh(
            recompensa,
            LockModeType.PESSIMISTIC_WRITE
        );

        // =================================
        // 7. VALIDAR ESTADO DE RECOMPENSA
        // =================================

        if (recompensa.getEstado()
                != Recompensa.EstadoRecompensa.activo) {

            throw new IllegalStateException(
                "La recompensa no esta disponible"
            );
        }

        // =================================
        // 8. VALIDAR STOCK
        // =================================

        Integer stockActual =
                recompensa.getStock();

        if (stockActual == null
                || stockActual <= 0) {

            throw new IllegalStateException(
                "No existe stock disponible"
            );
        }

        // =================================
        // 9. VALIDAR COSTO
        // =================================

        Integer costoPuntos =
                recompensa.getCosto_puntos();

        if (costoPuntos == null
                || costoPuntos < 0) {

            throw new IllegalStateException(
                "La recompensa tiene "
                + "un costo invalido"
            );
        }

        // =================================
        // 10. CONSULTAR SALDO ACTUAL
        // =================================

        BigDecimal puntosDisponibles =
                usuario.getPuntos_acumulados();

        if (puntosDisponibles == null) {

            puntosDisponibles =
                    BigDecimal.ZERO;
        }

        BigDecimal puntosNecesarios =
                BigDecimal.valueOf(costoPuntos);

        // =================================
        // 11. VERIFICAR PUNTOS SUFICIENTES
        // =================================

        if (puntosDisponibles.compareTo(
                puntosNecesarios) < 0) {

            throw new IllegalStateException(
                "El usuario no tiene "
                + "puntos suficientes"
            );
        }

        // =================================
        // 12. CALCULAR NUEVO SALDO
        // =================================

        BigDecimal nuevoSaldo =
            puntosDisponibles.subtract(
                puntosNecesarios
            );

        // =================================
        // 13. CALCULAR NUEVO STOCK
        // =================================

        int nuevoStock =
                stockActual - 1;

        // =================================
        // 14. DESCONTAR PUNTOS
        // =================================

        usuario.setPuntos_acumulados(
            nuevoSaldo
        );

        usuarioRepository.save(
            usuario
        );

        // =================================
        // 15. DESCONTAR STOCK
        // =================================

        recompensa.setStock(
            nuevoStock
        );

        recompensaRepository.save(
            recompensa
        );

        // =================================
        // 16. GENERAR TICKET
        // =================================

        String codigoTicket =
                generarCodigoTicket();

        // =================================
        // 17. CREAR REGISTRO DE CANJE
        // =================================

        Canje canje = new Canje();

        canje.setUsuario(
            usuario
        );

        canje.setRecompensa(
            recompensa
        );

        canje.setPuntos_descontados(
            costoPuntos
        );

        canje.setCodigo_ticket(
            codigoTicket
        );

        canje.setFecha_canje(
            LocalDateTime.now()
        );

        canje.setEstado(
            Canje.EstadoCanje.pendiente
        );

        // =================================
        // 18. REGISTRAR EN MYSQL
        //
        // La transaccion confirma juntos:
        //
        // - Nuevo saldo del vecino
        // - Nuevo stock de recompensa
        // - Nuevo registro de canje
        //
        // Si ocurre un error que provoque
        // rollback, se revierten los tres.
        // =================================

        return canjeRepository.save(
            canje
        );
    }

    // =====================================
    // LISTAR RECOMPENSAS DISPONIBLES
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Recompensa> listarPremiosDisponibles() {

        return recompensaRepository
            .listarDisponiblesParaCanje();
    }

    // =====================================
    // CONSULTAR HISTORIAL POR USUARIO
    // =====================================

    @Override
    @Transactional(readOnly = true)
    public List<Canje> listarCanjesPorUsuario(
            Integer idUsuario) {

        if (idUsuario == null
                || idUsuario <= 0) {

            throw new IllegalArgumentException(
                "El ID del usuario no es valido"
            );
        }

        Usuario usuario =
            usuarioRepository
                .findById(idUsuario)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El usuario no existe"
                    )
                );

        return canjeRepository.findByUsuario(
            usuario
        );
    }

    // =====================================
    // GENERAR CODIGO UNICO DE TICKET
    // =====================================

    private String generarCodigoTicket() {

        String identificador =
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();

        return "TK-" + identificador;
    }
}