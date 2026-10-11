
package com.perurecicla.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * PERU RECICLA
 *
 * Controlador de Inicio
 *
 * Administra las solicitudes HTTP
 * relacionadas con la pagina principal
 * del sistema.
 *
 * Tecnologias:
 *
 * Java 21
 * Spring Boot 3.4.2
 * Spring MVC
 * Thymeleaf
 *
 * Arquitectura:
 *
 * Controller -> Service -> Repository
 */

@Controller
public class InicioController {

    // =====================================
    // PAGINA PRINCIPAL DEL SISTEMA
    // =====================================

    @GetMapping({"/", "/inicio"})
    public String mostrarInicio(Model model) {

        // =================================
        // INFORMACION GENERAL
        // =================================

        model.addAttribute(
            "titulo",
            "PERU RECICLA"
        );

        model.addAttribute(
            "subtitulo",
            "Ecosistema Digital de Incentivos para el Reciclaje"
        );

        model.addAttribute(
            "mensaje",
            "Recicla, acumula puntos y canjea recompensas."
        );

        // =================================
        // RETORNAR VISTA THYMELEAF
        // =================================

        return "inicio";
    }
}
