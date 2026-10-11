
package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.model.Recolecta;

import com.perurecicla.service.UsuarioService;
import com.perurecicla.service.RecolectaService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Collections;
import java.math.BigDecimal;

/**
 * PERU RECICLA
 *
 * Controlador del historial de recolectas.
 *
 * Funcionalidades:
 *
 * 1. Identificar al vecino autenticado.
 * 2. Consultar los datos del usuario.
 * 3. Obtener su historial de recolectas.
 * 4. Mostrar materiales y pesos.
 * 5. Mostrar puntos generados.
 * 6. Mostrar estados de validacion.
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
 * Thymeleaf
 * JPA / Hibernate
 * MySQL
 */

@Controller
public class HistorialRecolectaController {

    // =====================================
    // SERVICIOS
    // =====================================

    private final UsuarioService usuarioService;

    private final RecolectaService recolectaService;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public HistorialRecolectaController(
            UsuarioService usuarioService,
            RecolectaService recolectaService) {

        this.usuarioService = usuarioService;
        this.recolectaService = recolectaService;
    }

    // =====================================
    // MOSTRAR HISTORIAL DE RECOLECTAS
    // =====================================

    @GetMapping("/vecino/recolectas")
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
        // BUSCAR VECINO EN MYSQL
        // =================================

        Usuario usuario =
            usuarioService.buscarPorCorreo(correo);

        if (usuario == null) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
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
        // CONSULTAR HISTORIAL
        // =================================

        List<Recolecta> recolectas =
            recolectaService
                .listarHistorialPorVecino(
                    usuario.getId_usuario()
                );

        // =================================
        // PREVENIR LISTAS NULAS
        // =================================

        if (recolectas == null) {

            recolectas = Collections.emptyList();
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
            "Mis Recolectas"
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
            "puntos",
            puntos
        );

        model.addAttribute(
            "recolectas",
            recolectas
        );

        model.addAttribute(
            "totalRecolectas",
            recolectas.size()
        );

        // =================================
        // RETORNAR VISTA
        // =================================

        return "historial-recolectas";
    }
}
