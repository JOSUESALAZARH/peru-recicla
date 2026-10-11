package com.perurecicla.controller;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;

import com.perurecicla.service.CanjeService;
import com.perurecicla.service.RecompensaService;
import com.perurecicla.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * PERU RECICLA - PASO 77
 *
 * Controlador de confirmacion de canjes.
 *
 * CONSERVA:
 * - Identificacion del vecino autenticado.
 * - Verificacion de vecino activo.
 * - Consulta de recompensa activa.
 * - Verificacion de puntos suficientes.
 * - Verificacion de stock.
 * - Ejecucion del canje mediante CanjeService.
 * - Generacion y presentacion del ticket.
 * - Mensajes de exito y error.
 * - Redireccion despues del POST.
 *
 * NUEVO:
 * - Token temporal de un solo uso.
 * - Validacion del token en el servidor.
 * - Bloqueo de reenvios accidentales.
 *
 * No modifica el modelo Canje ni las tablas MySQL.
 */
@Controller
public class CanjeController {

    private final UsuarioService usuarioService;

    private final RecompensaService recompensaService;

    private final CanjeService canjeService;

    // =========================================
    // CONSTRUCTOR
    // =========================================

    public CanjeController(
            UsuarioService usuarioService,
            RecompensaService recompensaService,
            CanjeService canjeService) {

        this.usuarioService = usuarioService;

        this.recompensaService = recompensaService;

        this.canjeService = canjeService;
    }

    // =========================================
    // MOSTRAR CONFIRMACION DE CANJE
    // =========================================

    @GetMapping("/vecino/recompensas/{id}/confirmar")
    public String mostrarConfirmacion(
            @PathVariable("id") Integer id,
            Principal principal,
            HttpSession session,
            Model model) {

        // =====================================
        // 1. VALIDAR VECINO
        // =====================================

        Usuario vecino =
                obtenerVecinoActivo(principal);

        // =====================================
        // 2. OBTENER RECOMPENSA
        // =====================================

        Recompensa recompensa =
                obtenerRecompensaActiva(id);

        // =====================================
        // 3. CONSULTAR PUNTOS
        // =====================================

        BigDecimal puntos =
                vecino.getPuntos_acumulados() == null
                ? BigDecimal.ZERO
                : vecino.getPuntos_acumulados();

        // =====================================
        // 4. CONSULTAR COSTO
        // =====================================

        Integer costo =
                recompensa.getCosto_puntos();

        // =====================================
        // 5. COMPROBAR STOCK
        // =====================================

        boolean conStock =
                recompensa.getStock() != null
                && recompensa.getStock() > 0;

        // =====================================
        // 6. COMPROBAR COSTO VALIDO
        // =====================================

        boolean costoValido =
                costo != null
                && costo >= 0;

        // =====================================
        // 7. COMPROBAR PUNTOS
        // =====================================

        boolean puntosSuficientes =
                costoValido
                && puntos.compareTo(
                    BigDecimal.valueOf(costo.longValue())
                ) >= 0;

        boolean puedeCanjear =
                conStock && puntosSuficientes;

        // =====================================
        // 8. DATOS PARA THYMELEAF
        // =====================================

        model.addAttribute(
                "titulo",
                "Confirmar canje"
        );

        model.addAttribute(
                "nombre",
                vecino.getNombre()
        );

        model.addAttribute(
                "recompensa",
                recompensa
        );

        model.addAttribute(
                "puntos",
                puntos
        );

        model.addAttribute(
                "conStock",
                conStock
        );

        model.addAttribute(
                "puntosSuficientes",
                puntosSuficientes
        );

        model.addAttribute(
                "puedeCanjear",
                puedeCanjear
        );

        // =====================================
        // 9. GENERAR TOKEN DE SOLICITUD
        //
        // Solo generar si:
        // - Tiene stock.
        // - Tiene puntos.
        // - No acaba de confirmarse un canje.
        //
        // Cada token se almacena temporalmente
        // en la sesion del navegador.
        // =====================================

        String solicitudToken = "";

        boolean canjeYaConfirmado =
                model.containsAttribute("mensajeExito");

        if (puedeCanjear && !canjeYaConfirmado) {

            solicitudToken =
                    UUID.randomUUID().toString();

            synchronized (session) {

                session.setAttribute(
                    claveToken(id),
                    solicitudToken
                );
            }
        }

        model.addAttribute(
            "solicitudToken",
            solicitudToken
        );

        return "confirmar-canje";
    }

    // =========================================
    // PROCESAR EL CANJE
    // =========================================

    @PostMapping("/vecino/recompensas/{id}/confirmar")
    public String confirmarCanje(
            @PathVariable("id") Integer id,

            @RequestParam(
                name = "solicitudToken",
                required = false
            )
            String solicitudToken,

            Principal principal,

            HttpSession session,

            RedirectAttributes mensajes) {

        // =====================================
        // 1. VALIDAR VECINO
        // =====================================

        Usuario vecino =
                obtenerVecinoActivo(principal);

        // =====================================
        // 2. COMPROBAR TOKEN DE UN SOLO USO
        //
        // La comprobacion y la eliminacion
        // deben ejecutarse juntas.
        //
        // Una segunda solicitud con el mismo
        // token no puede pasar esta validacion.
        // =====================================

        boolean tokenValido = false;

        synchronized (session) {

            Object tokenGuardado =
                    session.getAttribute(
                        claveToken(id)
                    );

            if (solicitudToken != null
                    && !solicitudToken.isBlank()
                    && tokenGuardado instanceof String
                    && solicitudToken.equals(
                        tokenGuardado
                    )) {

                // Consumir token ANTES del canje.

                session.removeAttribute(
                    claveToken(id)
                );

                tokenValido = true;
            }
        }

        // =====================================
        // 3. RECHAZAR SOLICITUD REPETIDA
        // =====================================

        if (!tokenValido) {

            mensajes.addFlashAttribute(
                "mensajeError",
                "Esta solicitud de canje ya fue "
                + "utilizada o ha caducado. "
                + "Revisa tu historial antes "
                + "de intentar nuevamente."
            );

            return "redirect:/vecino/recompensas/"
                    + id + "/confirmar";
        }

        // =====================================
        // 4. REALIZAR CANJE
        //
        // CanjeService controla:
        //
        // - Puntos suficientes.
        // - Stock disponible.
        // - Estado de la recompensa.
        // - Descuento de puntos.
        // - Reduccion del stock.
        // - Registro del ticket en MySQL.
        //
        // El PASO 76 ya agrego bloqueos
        // de saldo y stock al servicio.
        // =====================================

        try {

            Canje canje =
                canjeService.realizarCanje(
                    vecino.getId_usuario(),
                    id
                );

            // =================================
            // 5. MOSTRAR RESULTADO EXITOSO
            // =================================

            mensajes.addFlashAttribute(
                "mensajeExito",
                "Canje registrado correctamente."
            );

            mensajes.addFlashAttribute(
                "codigoTicket",
                canje.getCodigo_ticket()
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            mensajes.addFlashAttribute(
                "mensajeError",
                ex.getMessage()
            );
        }

        // =====================================
        // 6. REDIRECCION POST/REDIRECT/GET
        //
        // Actualizar la pagina no repite
        // directamente el POST del canje.
        // =====================================

        return "redirect:/vecino/recompensas/"
                + id + "/confirmar";
    }

    // =========================================
    // CLAVE DE TOKEN POR RECOMPENSA
    //
    // Una recompensa tiene su propia clave
    // dentro de la sesion del vecino.
    // =========================================

    private String claveToken(Integer id) {

        return "PERURECICLA_CANJE_TOKEN_"
                + id;
    }

    // =========================================
    // VALIDAR VECINO AUTENTICADO
    // =========================================

    private Usuario obtenerVecinoActivo(
            Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario usuario =
                usuarioService.buscarPorCorreo(
                    principal.getName()
                );

        if (usuario == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se encontro el usuario autenticado"
            );
        }

        if (usuario.getRol()
                    != Usuario.Rol.ROLE_vecino
                || usuario.getEstado()
                    != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para vecinos activos"
            );
        }

        return usuario;
    }

    // =========================================
    // CONSULTAR RECOMPENSA ACTIVA
    // =========================================

    private Recompensa obtenerRecompensaActiva(
            Integer id) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Recompensa no encontrada"
            );
        }

        // Reutilizar el servicio existente.

        List<Recompensa> activas =
                recompensaService.listarActivos();

        if (activas == null) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Recompensa no encontrada"
            );
        }

        return activas.stream()
                .filter(r ->
                    r != null
                    && id.equals(
                        r.getId_recompensa()
                    )
                )
                .findFirst()
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recompensa no encontrada o inactiva"
                    )
                );
    }
}