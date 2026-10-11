
package com.perurecicla.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * PERU RECICLA
 *
 * Controlador de Acceso al Sistema.
 *
 * Proyecto:
 * Ecosistema Digital de Incentivos
 * para el Reciclaje.
 *
 * Tecnologias:
 *
 * Java 21
 * Spring Boot 3.4.2
 * Spring Security
 * Spring MVC
 * Thymeleaf
 * MySQL
 *
 * Funcionalidades:
 *
 * 1. Mostrar el formulario de login.
 * 2. Integrar la vista login.html.
 * 3. Permitir el acceso mediante
 *    las rutas configuradas.
 *
 * La autenticacion del usuario
 * es procesada por Spring Security
 * mediante UsuarioDetailsService.
 *
 * Arquitectura:
 *
 * Controller -> Thymeleaf
 */

@Controller
public class AccesoController {

    // =====================================
    // MOSTRAR PAGINA DE INICIO DE SESION
    // =====================================

    @GetMapping("/login")
    public String mostrarLogin() {

        // Retorna la vista Thymeleaf
        // ubicada en:
        //
        // src/main/resources/templates/login.html

        return "login";
    }
}
