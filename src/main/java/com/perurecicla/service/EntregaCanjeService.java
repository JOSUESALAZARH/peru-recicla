
package com.perurecicla.service;

import com.perurecicla.model.Canje;
import com.perurecicla.repository.CanjeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PERU RECICLA - PASO 50
 *
 * Servicio para registrar la entrega fisica
 * de recompensas previamente canjeadas.
 *
 * Reglas:
 *
 * 1. El ticket debe existir.
 * 2. El canje debe estar pendiente.
 * 3. No se descuentan puntos nuevamente.
 * 4. No se reduce el stock nuevamente.
 * 5. Una entrega no puede repetirse.
 */
@Service
public class EntregaCanjeService {

    private final CanjeRepository canjeRepository;

    public EntregaCanjeService(
            CanjeRepository canjeRepository) {

        this.canjeRepository = canjeRepository;
    }

    @Transactional
    public void confirmarEntrega(String codigoTicket) {

        // VALIDAR CODIGO
        if (codigoTicket == null
                || codigoTicket.isBlank()) {

            throw new IllegalArgumentException(
                "El codigo de ticket es obligatorio"
            );
        }

        String codigo = codigoTicket.trim();

        if (codigo.length() > 20) {
            throw new IllegalArgumentException(
                "El codigo del ticket no es valido"
            );
        }

        // BUSCAR EL CANJE EN MYSQL
        Canje canje = canjeRepository
                .buscarPorCodigoTicket(codigo)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "No existe un canje con ese ticket"
                    )
                );

        // VERIFICAR ESTADO ACTUAL
        if (canje.getEstado()
                != Canje.EstadoCanje.pendiente) {

            throw new IllegalStateException(
                "El canje no esta pendiente. "
                + "No puede entregarse nuevamente."
            );
        }

        // ACTUALIZACION ATOMICA
        int actualizados =
            canjeRepository.actualizarEstadoSiCoincide(
                canje.getId_canje(),
                Canje.EstadoCanje.pendiente,
                Canje.EstadoCanje.entregado
            );

        // PROTECCION ANTE DOS CONFIRMACIONES
        if (actualizados != 1) {

            throw new IllegalStateException(
                "No se pudo confirmar la entrega. "
                + "Es posible que el ticket ya "
                + "haya sido procesado."
            );
        }
    }
}
