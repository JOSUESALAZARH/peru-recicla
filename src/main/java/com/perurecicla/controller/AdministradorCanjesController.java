
package com.perurecicla.controller;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.CanjeRepository;

import com.perurecicla.service.UsuarioService;
import com.perurecicla.service.EntregaCanjeService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Optional;

/**
 * PERU RECICLA - PASO 50
 *
 * Controlador administrativo de canjes.
 *
 * GET  /admin/canjes
 * POST /admin/canjes/entregar
 *
 * Acceso exclusivo para administradores activos.
 */
@Controller
public class AdministradorCanjesController {

    private final UsuarioService usuarioService;
    private final CanjeRepository canjeRepository;
    private final EntregaCanjeService entregaCanjeService;

    public AdministradorCanjesController(
            UsuarioService usuarioService,
            CanjeRepository canjeRepository,
            EntregaCanjeService entregaCanjeService) {

        this.usuarioService = usuarioService;
        this.canjeRepository = canjeRepository;
        this.entregaCanjeService = entregaCanjeService;
    }

    // =====================================
    // BUSCAR Y MOSTRAR TICKET
    // =====================================

    @GetMapping("/admin/canjes")
    public String mostrarCanjes(
            @RequestParam(
                value = "codigo",
                required = false
            ) String codigo,
            Principal principal,
            Model model) {

        Usuario administrador =
                validarAdministrador(principal);

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

        String codigoLimpio =
                codigo == null ? "" : codigo.trim();

        model.addAttribute("codigo", codigoLimpio);

        if (!codigoLimpio.isEmpty()) {

            if (codigoLimpio.length() > 20) {

                model.addAttribute(
                    "error",
                    "El codigo del ticket no es valido"
                );

            } else {

                Optional<Canje> resultado =
                    canjeRepository
                        .buscarPorCodigoTicket(
                            codigoLimpio
                        );

                if (resultado.isPresent()) {

                    model.addAttribute(
                        "canje",
                        resultado.get()
                    );

                } else {

                    model.addAttribute(
                        "error",
                        "No se encontro el ticket"
                    );
                }
            }
        }

        return "admin-canjes";
    }

    // =====================================
    // CONFIRMAR ENTREGA
    // =====================================

    @PostMapping("/admin/canjes/entregar")
    public String entregarRecompensa(
            @RequestParam("codigo") String codigo,
            Principal principal,
            RedirectAttributes redirect) {

        // SEGUNDA VALIDACION DE AUTORIZACION
        validarAdministrador(principal);

        String codigoLimpio =
                codigo == null ? "" : codigo.trim();

        try {

            // EL SERVICIO CONTROLA LA TRANSACCION
            entregaCanjeService.confirmarEntrega(
                codigoLimpio
            );

            redirect.addFlashAttribute(
                "exito",
                "Entrega confirmada correctamente. "
                + "El ticket ya figura como entregado."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException e) {

            redirect.addFlashAttribute(
                "error",
                e.getMessage()
            );
        }

        redirect.addAttribute(
            "codigo",
            codigoLimpio
        );

        return "redirect:/admin/canjes";
    }

    // =====================================
    // VERIFICAR ADMINISTRADOR ACTIVO
    // =====================================

    private Usuario validarAdministrador(
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

        if (usuario == null
                || usuario.getRol()
                    != Usuario.Rol.ROLE_administrador
                || usuario.getEstado()
                    != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para "
                + "administradores activos"
            );
        }

        return usuario;
    }
}
