
package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.model.Recompensa;

import com.perurecicla.service.UsuarioService;
import com.perurecicla.service.RecompensaService;

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
 * Controlador del catalogo de recompensas.
 *
 * Funcionalidades:
 *
 * 1. Identificar al vecino autenticado.
 * 2. Consultar sus puntos acumulados.
 * 3. Obtener recompensas activas.
 * 4. Consultar costos y stock.
 * 5. Enviar informacion a Thymeleaf.
 *
 * Arquitectura:
 *
 * Controller -> Service -> Repository -> MySQL
 *
 * Java 21
 * Spring Boot 3.4.2
 * Spring Data JPA
 * Thymeleaf
 */

@Controller
public class CatalogoRecompensasController {

    // =====================================
    // SERVICIOS DEL SISTEMA
    // =====================================

    private final UsuarioService usuarioService;

    private final RecompensaService recompensaService;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public CatalogoRecompensasController(
            UsuarioService usuarioService,
            RecompensaService recompensaService) {

        this.usuarioService = usuarioService;
        this.recompensaService = recompensaService;
    }

    // =====================================
    // MOSTRAR CATALOGO DE RECOMPENSAS
    // =====================================

    @GetMapping("/vecino/recompensas")
    public String mostrarCatalogo(
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
                "No se encontro el usuario autenticado"
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
                "La cuenta no se encuentra activa"
            );
        }

        // =================================
        // CONSULTAR SALDO DEL VECINO
        // =================================

        BigDecimal puntos =
            usuario.getPuntos_acumulados();

        if (puntos == null) {

            puntos = BigDecimal.ZERO;
        }

        // =================================
        // CONSULTAR RECOMPENSAS ACTIVAS
        // =================================

        List<Recompensa> recompensas =
            recompensaService.listarActivos();

        if (recompensas == null) {

            recompensas = Collections.emptyList();
        }

        // =================================
        // CONTAR RECOMPENSAS CON STOCK
        // =================================

        long cantidadDisponibles = recompensas
            .stream()
            .filter(r ->
                r != null &&
                r.getStock() != null &&
                r.getStock() > 0
            )
            .count();

        // =================================
        // ENVIAR DATOS A THYMELEAF
        // =================================

        model.addAttribute(
            "titulo",
            "Catalogo de Recompensas"
        );

        model.addAttribute(
            "nombre",
            usuario.getNombre()
        );

        model.addAttribute(
            "puntos",
            puntos
        );

        model.addAttribute(
            "recompensas",
            recompensas
        );

        model.addAttribute(
            "totalRecompensas",
            recompensas.size()
        );

        model.addAttribute(
            "cantidadDisponibles",
            cantidadDisponibles
        );

        // =================================
        // RETORNAR VISTA
        // =================================

        return "catalogo-recompensas";
    }
}
