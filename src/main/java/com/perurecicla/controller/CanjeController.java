
package com.perurecicla.controller;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;

import com.perurecicla.service.CanjeService;
import com.perurecicla.service.RecompensaService;
import com.perurecicla.service.UsuarioService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * PERU RECICLA
 *
 * Controlador de confirmacion de canjes.
 *
 * Arquitectura:
 * Controller -> Service -> Repository -> MySQL
 *
 * Java 21
 * Spring Boot 3.4.2
 * Spring MVC
 * Spring Security
 * Thymeleaf
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
            Model model) {

        Usuario vecino = obtenerVecinoActivo(principal);

        Recompensa recompensa =
                obtenerRecompensaActiva(id);

        BigDecimal puntos =
                vecino.getPuntos_acumulados() == null
                ? BigDecimal.ZERO
                : vecino.getPuntos_acumulados();

        Integer costo = recompensa.getCosto_puntos();

        boolean conStock =
                recompensa.getStock() != null
                && recompensa.getStock() > 0;

        boolean costoValido =
                costo != null && costo >= 0;

        boolean puntosSuficientes =
                costoValido
                && puntos.compareTo(
                    BigDecimal.valueOf(costo.longValue())
                ) >= 0;

        model.addAttribute(
                "titulo", "Confirmar canje");

        model.addAttribute(
                "nombre", vecino.getNombre());

        model.addAttribute(
                "recompensa", recompensa);

        model.addAttribute(
                "puntos", puntos);

        model.addAttribute(
                "conStock", conStock);

        model.addAttribute(
                "puntosSuficientes",
                puntosSuficientes);

        model.addAttribute(
                "puedeCanjear",
                conStock && puntosSuficientes);

        return "confirmar-canje";
    }

    // =========================================
    // PROCESAR EL CANJE
    // =========================================

    @PostMapping("/vecino/recompensas/{id}/confirmar")
    public String confirmarCanje(
            @PathVariable("id") Integer id,
            Principal principal,
            RedirectAttributes mensajes) {

        // El usuario se obtiene de la sesion.
        // No se acepta su ID desde el formulario.

        Usuario vecino = obtenerVecinoActivo(principal);

        try {

            // La validacion definitiva y los
            // cambios de puntos y stock se
            // realizan en el servicio existente.

            Canje canje = canjeService.realizarCanje(
                    vecino.getId_usuario(),
                    id
            );

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

        // Evita que actualizar la pagina
        // repita directamente el POST.

        return "redirect:/vecino/recompensas/"
                + id + "/confirmar";
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

        // Reutilizamos listarActivos(),
        // que ya existe en RecompensaService.

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
