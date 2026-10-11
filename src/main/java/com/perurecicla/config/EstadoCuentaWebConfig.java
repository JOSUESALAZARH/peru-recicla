package com.perurecicla.config;

import com.perurecicla.model.Usuario;
import com.perurecicla.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import org.springframework.web.servlet.HandlerInterceptor;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * PERU RECICLA - PASO 81
 *
 * Control del estado de cuentas
 * con sesiones ya iniciadas.
 *
 * Objetivo:
 *
 * Si un administrador desactiva una cuenta,
 * la siguiente solicitud del usuario
 * comprueba su estado en MySQL.
 *
 * Si esta inactiva:
 *
 * - Se invalida su sesion.
 * - Se elimina su autenticacion.
 * - Se redirige al login.
 *
 * No modifica:
 * - SecurityConfig
 * - UsuarioDetailsService
 * - Base de datos
 * - Roles de usuarios
 */
@Configuration
public class EstadoCuentaWebConfig
        implements WebMvcConfigurer {

    private final UsuarioRepository usuarioRepository;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public EstadoCuentaWebConfig(
            UsuarioRepository usuarioRepository) {

        this.usuarioRepository =
            usuarioRepository;
    }

    // =====================================
    // CONFIGURAR INTERCEPTOR
    // =====================================

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {

        registry.addInterceptor(
            new HandlerInterceptor() {

                @Override
                public boolean preHandle(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Object handler) throws Exception {

                    // =========================
                    // 1. OBTENER AUTENTICACION
                    // =========================

                    Authentication autenticacion =
                        SecurityContextHolder
                            .getContext()
                            .getAuthentication();

                    // =========================
                    // 2. PERMITIR VISITANTES
                    // =========================

                    if (autenticacion == null
                            || !autenticacion.isAuthenticated()
                            || autenticacion
                                instanceof AnonymousAuthenticationToken) {

                        return true;
                    }

                    // =========================
                    // 3. CONSULTAR ESTADO REAL
                    // =========================

                    Usuario usuario =
                        usuarioRepository
                            .findByCorreo(
                                autenticacion.getName()
                            )
                            .orElse(null);

                    // =========================
                    // 4. PERMITIR CUENTA ACTIVA
                    // =========================

                    if (usuario != null
                            && usuario.getEstado()
                                == Usuario.Estado.activo) {

                        return true;
                    }

                    // =========================
                    // 5. CERRAR SESION INVALIDA
                    // =========================

                    SecurityContextLogoutHandler logout =
                        new SecurityContextLogoutHandler();

                    logout.logout(
                        request,
                        response,
                        autenticacion
                    );

                    // =========================
                    // 6. REDIRIGIR AL LOGIN
                    // =========================

                    response.sendRedirect(
                        request.getContextPath()
                            + "/login?logout"
                    );

                    return false;
                }
            }
        )
        .addPathPatterns(
            "/",
            "/inicio",
            "/admin",
            "/admin/**",
            "/vecino",
            "/vecino/**",
            "/recolector",
            "/recolector/**"
        );
    }
}