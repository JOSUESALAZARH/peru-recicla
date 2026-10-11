package com.perurecicla.service;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;
import com.perurecicla.repository.CanjeRepository;
import com.perurecicla.repository.RecompensaRepository;
import com.perurecicla.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PERU RECICLA - PASO 83
 *
 * Cancelacion administrativa de un canje pendiente.
 * Una sola transaccion realiza lo siguiente:
 * 1. Cambia estado a cancelado.
 * 2. Reembolsa los puntos originalmente descontados.
 * 3. Restituye una unidad de stock.
 * 4. Guarda motivo, fecha e ID del administrador.
 *
 * El bloqueo del ticket y de los recursos impide
 * repetir el reembolso ante solicitudes simultaneas.
 */
@Service
public class CancelacionCanjeService {

    private static final BigDecimal SALDO_MAXIMO =
        new BigDecimal("99999999.99");

    private final CanjeRepository canjeRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecompensaRepository recompensaRepository;
    private final EntityManager entityManager;

    public CancelacionCanjeService(
            CanjeRepository canjeRepository,
            UsuarioRepository usuarioRepository,
            RecompensaRepository recompensaRepository,
            EntityManager entityManager) {

        this.canjeRepository = canjeRepository;
        this.usuarioRepository = usuarioRepository;
        this.recompensaRepository = recompensaRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void cancelarCanje(
            String codigoTicket,
            String motivo,
            String correoAdministrador) {

        if (correoAdministrador == null
                || correoAdministrador.isBlank()) {
            throw new IllegalStateException("Debes iniciar sesion.");
        }

        // El rol y el estado se validan de nuevo en el servicio.
        Usuario administrador = usuarioRepository
            .findByCorreo(correoAdministrador)
            .orElseThrow(() -> new IllegalStateException(
                "La cuenta administrativa no existe."
            ));

        if (administrador.getRol() != Usuario.Rol.ROLE_administrador
                || administrador.getEstado() != Usuario.Estado.activo) {
            throw new IllegalStateException(
                "Solo un administrador activo puede cancelar canjes."
            );
        }

        String codigo = codigoTicket == null
            ? "" : codigoTicket.strip();

        if (codigo.isBlank() || codigo.length() > 20) {
            throw new IllegalArgumentException(
                "Ingresa un codigo de ticket valido."
            );
        }

        String motivoLimpio = motivo == null ? "" : motivo.strip();

        if (motivoLimpio.length() < 8 || motivoLimpio.length() > 300) {
            throw new IllegalArgumentException(
                "El motivo debe contener entre 8 y 300 caracteres."
            );
        }

        // Buscar el canje y bloquear su fila.
        // Otra cancelacion o una entrega concurrente
        // tendra que esperar hasta terminar esta transaccion.
        Canje canje = canjeRepository.buscarPorCodigoTicket(codigo)
            .orElseThrow(() -> new IllegalArgumentException(
                "No existe un canje con ese ticket."
            ));

        entityManager.refresh(
            canje,
            LockModeType.PESSIMISTIC_WRITE
        );

        if (canje.getEstado() != Canje.EstadoCanje.pendiente) {
            throw new IllegalStateException(
                "Solo pueden cancelarse canjes pendientes. "
                + "Este ticket ya fue entregado o cancelado."
            );
        }

        Usuario vecino = canje.getUsuario();
        Recompensa recompensa = canje.getRecompensa();

        if (vecino == null || recompensa == null) {
            throw new IllegalStateException(
                "El canje no tiene vecino o recompensa validos."
            );
        }

        // Mismo orden de bloqueo utilizado por CanjeServiceImpl:
        // primero saldo del vecino, despues stock de recompensa.
        entityManager.refresh(
            vecino,
            LockModeType.PESSIMISTIC_WRITE
        );

        entityManager.refresh(
            recompensa,
            LockModeType.PESSIMISTIC_WRITE
        );

        Integer puntosOriginales = canje.getPuntos_descontados();
        if (puntosOriginales == null || puntosOriginales < 0) {
            throw new IllegalStateException(
                "El ticket tiene una cantidad de puntos invalida."
            );
        }

        BigDecimal saldoActual = vecino.getPuntos_acumulados();
        if (saldoActual == null || saldoActual.signum() < 0) {
            throw new IllegalStateException(
                "El saldo del vecino no es valido."
            );
        }

        BigDecimal saldoRestituido = saldoActual.add(
            BigDecimal.valueOf(puntosOriginales)
        );

        if (saldoRestituido.compareTo(SALDO_MAXIMO) > 0) {
            throw new IllegalStateException(
                "La devolucion supera el saldo maximo permitido."
            );
        }

        Integer stockActual = recompensa.getStock();
        if (stockActual == null || stockActual < 0
                || stockActual == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                "No se puede restituir el stock de la recompensa."
            );
        }

        // Estos cambios se confirman JUNTOS o se revierten JUNTOS.
        vecino.setPuntos_acumulados(saldoRestituido);
        recompensa.setStock(stockActual + 1);

        canje.setEstado(Canje.EstadoCanje.cancelado);
        canje.setMotivo_cancelacion(motivoLimpio);
        canje.setFecha_cancelacion(LocalDateTime.now());
        canje.setId_administrador_cancelacion(
            administrador.getId_usuario()
        );

        usuarioRepository.save(vecino);
        recompensaRepository.save(recompensa);
        canjeRepository.save(canje);
    }
}