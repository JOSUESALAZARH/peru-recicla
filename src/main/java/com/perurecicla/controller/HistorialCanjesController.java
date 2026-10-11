
package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.model.Canje;

import com.perurecicla.service.UsuarioService;
import com.perurecicla.service.CanjeService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;

/**
 * PERU RECICLA
 *
 * Controlador del Historial de Canjes.
 *
 * Funcionalidades:
 *
 * 1. Identificar al vecino autenticado.
 * 2. Consultar sus datos desde MySQL.
 * 3. Consultar los canjes del vecino.
 * 4. Mostrar recompensas canjeadas.
 * 5. Mostrar puntos descontados.
 * 6. Mostrar fechas y codigos de ticket.
 * 7. Mostrar estados de los canjes.
 *
 * Arquitectura:
 *
 * Controller -> Service -> Repository -> MySQL
 *
 * Tecnologias:
 *
 * Java 21
 * Spring Boot 3.4.2
 * Spring MVC
 * Spring Security
 * Spring Data JPA
 * Hibernate
 * Thymeleaf
 * MySQL
 */

@Controller
public class HistorialCanjesController {

    // =====================================
    // SERVICIOS
    // =====================================

    private final UsuarioService usuarioService;

    private final CanjeService canjeService;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public HistorialCanjesController(
            UsuarioService usuarioService,
            CanjeService canjeService) {

        this.usuarioService = usuarioService;
        this.canjeService = canjeService;
    }

    // =====================================
    // MOSTRAR HISTORIAL DE CANJES
    // =====================================

    @GetMapping("/vecino/canjes")
    public String mostrarHistorial(
            Principal principal,
            Model model) {

        // =================================
        // VALIDAR SESION
        // =================================

        if (principal == null) {

            return "redirect:/login";
        }

        // =================================
        // OBTENER CORREO AUTENTICADO
        // =================================

        String correo = principal.getName();

        // =================================
        // CONSULTAR USUARIO EN MYSQL
        // =================================

        Usuario usuario =
            usuarioService.buscarPorCorreo(correo);

        if (usuario == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se encontro el usuario"
            );
        }

        // =================================
        // VERIFICAR ROL DEL VECINO
        // =================================

        if (usuario.getRol() !=
                Usuario.Rol.ROLE_vecino) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para vecinos"
            );
        }

        // =================================
        // VERIFICAR ESTADO DE LA CUENTA
        // =================================

        if (usuario.getEstado() !=
                Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta no esta activa"
            );
        }

        // =================================
        // CONSULTAR HISTORIAL DE CANJES
        // =================================

        List<Canje> canjes =
            canjeService.listarCanjesPorUsuario(
                usuario.getId_usuario()
            );

        // =================================
        // PREVENIR LISTA NULA
        // =================================

        if (canjes == null) {

            canjes = Collections.emptyList();
        }

        // =================================
        // CONSULTAR SALDO DE PUNTOS
        // =================================

        BigDecimal puntos =
            usuario.getPuntos_acumulados();

        if (puntos == null) {

            puntos = BigDecimal.ZERO;
        }

        // =================================
        // ENVIAR INFORMACION A THYMELEAF
        // =================================

        model.addAttribute(
            "titulo",
            "Mis Canjes"
        );

        model.addAttribute(
            "nombre",
            usuario.getNombre()
        );

        model.addAttribute(
            "apellido",
            usuario.getApellido()
        );

        model.addAttribute(
            "correo",
            usuario.getCorreo()
        );

        model.addAttribute(
            "puntos",
            puntos
        );

        model.addAttribute(
            "canjes",
            canjes
        );

        model.addAttribute(
            "totalCanjes",
            canjes.size()
        );

        // =================================
        // RETORNAR VISTA THYMELEAF
        // =================================

        return "historial-canjes";
    }
}
