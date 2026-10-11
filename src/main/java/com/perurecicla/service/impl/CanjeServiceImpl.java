
package com.perurecicla.service.impl;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.CanjeRepository;
import com.perurecicla.repository.RecompensaRepository;
import com.perurecicla.repository.UsuarioRepository;

import com.perurecicla.service.CanjeService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PERU RECICLA
 *
 * Implementacion del servicio de canjes.
 *
 * Funcionalidades:
 *
 * 1. Validar usuario.
 * 2. Verificar recompensa.
 * 3. Comprobar puntos disponibles.
 * 4. Verificar stock.
 * 5. Descontar puntos.
 * 6. Actualizar stock.
 * 7. Generar codigo de ticket.
 * 8. Registrar canje en MySQL.
 * 9. Consultar historial de canjes.
 *
 * Arquitectura:
 * Controller -> Service -> Repository -> MySQL
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
    // CONSTRUCTOR
    // =====================================

    public CanjeServiceImpl(
            CanjeRepository canjeRepository,
            UsuarioRepository usuarioRepository,
            RecompensaRepository recompensaRepository) {

        this.canjeRepository = canjeRepository;

        this.usuarioRepository = usuarioRepository;

        this.recompensaRepository =
            recompensaRepository;
    }

    // =====================================
    // REALIZAR CANJE DE RECOMPENSA
    // =====================================

    @Override
    @Transactional
    public Canje realizarCanje(
            Integer idUsuario,
            Integer idRecompensa) {

        // =================================
        // VALIDAR IDENTIFICADORES
        // =================================

        if (idUsuario == null || idUsuario <= 0) {

            throw new IllegalArgumentException(
                "El ID del usuario no es valido"
            );
        }

        if (idRecompensa == null ||
            idRecompensa <= 0) {

            throw new IllegalArgumentException(
                "El ID de la recompensa no es valido"
            );
        }

        // =================================
        // BUSCAR USUARIO
        // =================================

        Usuario usuario = usuarioRepository
            .findById(idUsuario)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "El usuario no existe"
                )
            );

        // =================================
        // VALIDAR ROL DEL USUARIO
        // =================================

        if (usuario.getRol() !=
                Usuario.Rol.ROLE_vecino) {

            throw new IllegalArgumentException(
                "Solo los vecinos pueden realizar canjes"
            );
        }

        // =================================
        // VALIDAR ESTADO DEL USUARIO
        // =================================

        if (usuario.getEstado() !=
                Usuario.Estado.activo) {

            throw new IllegalStateException(
                "El usuario no se encuentra activo"
            );
        }

        // =================================
        // BUSCAR RECOMPENSA
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
        // VERIFICAR ESTADO DE RECOMPENSA
        // =================================

        if (recompensa.getEstado() !=
                Recompensa.EstadoRecompensa.activo) {

            throw new IllegalStateException(
                "La recompensa no esta disponible"
            );
        }

        // =================================
        // VERIFICAR STOCK
        // =================================

        Integer stockActual =
            recompensa.getStock();

        if (stockActual == null ||
            stockActual <= 0) {

            throw new IllegalStateException(
                "No existe stock disponible"
            );
        }

        // =================================
        // VERIFICAR COSTO EN PUNTOS
        // =================================

        Integer costoPuntos =
            recompensa.getCosto_puntos();

        if (costoPuntos == null ||
            costoPuntos < 0) {

            throw new IllegalStateException(
                "La recompensa tiene un costo invalido"
            );
        }

        // =================================
        // CONSULTAR PUNTOS DEL USUARIO
        // =================================

        BigDecimal puntosDisponibles =
            usuario.getPuntos_acumulados();

        if (puntosDisponibles == null) {

            puntosDisponibles = BigDecimal.ZERO;
        }

        BigDecimal puntosNecesarios =
            BigDecimal.valueOf(costoPuntos);

        // =================================
        // VALIDAR SALDO
        // =================================

        if (puntosDisponibles.compareTo(
                puntosNecesarios) < 0) {

            throw new IllegalStateException(
                "El usuario no tiene puntos suficientes"
            );
        }

        // =================================
        // CALCULAR NUEVO SALDO
        // =================================

        BigDecimal nuevoSaldo =
            puntosDisponibles.subtract(
                puntosNecesarios
            );

        // =================================
        // CALCULAR NUEVO STOCK
        // =================================

        Integer nuevoStock = stockActual - 1;

        // =================================
        // ACTUALIZAR PUNTOS DEL VECINO
        // =================================

        usuario.setPuntos_acumulados(
            nuevoSaldo
        );

        usuarioRepository.save(usuario);

        // =================================
        // ACTUALIZAR STOCK DE RECOMPENSA
        // =================================

        recompensa.setStock(
            nuevoStock
        );

        recompensaRepository.save(
            recompensa
        );

        // =================================
        // GENERAR CODIGO UNICO DE TICKET
        // =================================

        String codigoTicket =
            generarCodigoTicket();

        // =================================
        // CREAR REGISTRO DE CANJE
        // =================================

        Canje canje = new Canje();

        canje.setUsuario(usuario);

        canje.setRecompensa(recompensa);

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
        // REGISTRAR CANJE EN MYSQL
        // =================================

        return canjeRepository.save(canje);
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

        if (idUsuario == null || idUsuario <= 0) {

            throw new IllegalArgumentException(
                "El ID del usuario no es valido"
            );
        }

        Usuario usuario = usuarioRepository
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
    // GENERAR CODIGO DE TICKET
    // =====================================

    private String generarCodigoTicket() {

        // Crear identificador aleatorio UUID

        String identificador = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 16)
            .toUpperCase();

        // Longitud total: 19 caracteres

        return "TK-" + identificador;
    }
}
