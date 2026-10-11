
package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.service.UsuarioService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;

/**
 * PERU RECICLA
 *
 * Controlador del panel del vecino.
 *
 * Funcionalidades:
 *
 * 1. Identificar al usuario autenticado.
 * 2. Buscar sus datos en MySQL.
 * 3. Verificar que sea vecino.
 * 4. Consultar puntos acumulados.
 * 5. Mostrar su panel personal.
 *
 * Arquitectura:
 *
 * Controller -> Service -> Repository -> MySQL
 */

@Controller
public class PanelVecinoController {

    // =====================================
    // SERVICIO DE USUARIOS
    // =====================================

    private final UsuarioService usuarioService;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public PanelVecinoController(
            UsuarioService usuarioService) {

        this.usuarioService = usuarioService;
    }

    // =====================================
    // MOSTRAR PANEL DEL VECINO
    // =====================================

    @GetMapping("/vecino")
    public String mostrarPanel(
            Principal principal,
            Model model) {

        // =================================
        // VERIFICAR SESION
        // =================================

        if (principal == null) {

            return "redirect:/login";
        }

        // =================================
        // OBTENER CORREO AUTENTICADO
        // =================================

        String correo = principal.getName();

        // =================================
        // BUSCAR USUARIO EN MYSQL
        // =================================

        Usuario usuario =
            usuarioService.buscarPorCorreo(correo);

        if (usuario == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se encontro la cuenta autenticada"
            );
        }

        // =================================
        // VERIFICAR ROL DEL USUARIO
        // =================================

        if (usuario.getRol() !=
                Usuario.Rol.ROLE_vecino) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para vecinos"
            );
        }

        // =================================
        // VERIFICAR ESTADO DE CUENTA
        // =================================

        if (usuario.getEstado() !=
                Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta no esta activa"
            );
        }

        // =================================
        // CONSULTAR PUNTOS ACUMULADOS
        // =================================

        BigDecimal puntos =
            usuario.getPuntos_acumulados();

        if (puntos == null) {

            puntos = BigDecimal.ZERO;
        }

        // =================================
        // ENVIAR DATOS A THYMELEAF
        // =================================

        model.addAttribute(
            "titulo",
            "Panel del Vecino"
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

        // =================================
        // RETORNAR VISTA DEL PANEL
        // =================================

        return "panel-vecino";
    }
}
