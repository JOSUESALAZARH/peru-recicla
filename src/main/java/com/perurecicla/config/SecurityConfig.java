
package com.perurecicla.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PERU RECICLA - PASO 59
 *
 * Seguridad y acceso de los tres perfiles.
 *
 * Administrador -> /admin
 * Vecino        -> /vecino
 * Recolector    -> /recolector/panel
 *
 * Conserva:
 * - Login personalizado
 * - Autenticacion con MySQL
 * - Cifrado BCrypt
 * - Proteccion de rutas
 * - Proteccion CSRF
 * - Cierre de sesion
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

            // =====================================
            // AUTORIZACION
            // =====================================

            .authorizeHttpRequests(auth -> auth

                // PAGINAS PUBLICAS
                .requestMatchers(
                    HttpMethod.GET,
                    "/",
                    "/inicio",
                    "/registro",
                    "/login"
                ).permitAll()

                // REGISTRO PUBLICO
                .requestMatchers(
                    HttpMethod.POST,
                    "/registro"
                ).permitAll()

                // ARCHIVOS ESTATICOS
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()

                // MODULO ADMINISTRATIVO
                .requestMatchers(
                    "/admin",
                    "/admin/**"
                ).hasAuthority(
                    "ROLE_administrador"
                )

                // MODULO DEL VECINO
                .requestMatchers(
                    "/vecino",
                    "/vecino/**"
                ).hasAuthority(
                    "ROLE_vecino"
                )

                // MODULO DEL RECOLECTOR
                // NUEVO - PASO 59
                .requestMatchers(
                    "/recolector",
                    "/recolector/**"
                ).hasAuthority(
                    "ROLE_recolector"
                )

                // OTRAS RUTAS INTERNAS
                .anyRequest().authenticated()
            )

            // =====================================
            // LOGIN PERSONALIZADO
            // =====================================

            .formLogin(form -> form

                .loginPage("/login")

                .loginProcessingUrl("/login")

                .usernameParameter("username")

                .passwordParameter("password")

                // REDIRECCION AUTOMATICA SEGUN ROL
                .successHandler(
                    (request, response, authentication) -> {

                        boolean esAdministrador =
                            authentication
                                .getAuthorities()
                                .stream()
                                .anyMatch(autoridad ->
                                    autoridad.getAuthority()
                                        .equals(
                                            "ROLE_administrador"
                                        )
                                );

                        boolean esVecino =
                            authentication
                                .getAuthorities()
                                .stream()
                                .anyMatch(autoridad ->
                                    autoridad.getAuthority()
                                        .equals(
                                            "ROLE_vecino"
                                        )
                                );

                        boolean esRecolector =
                            authentication
                                .getAuthorities()
                                .stream()
                                .anyMatch(autoridad ->
                                    autoridad.getAuthority()
                                        .equals(
                                            "ROLE_recolector"
                                        )
                                );

                        String destino;

                        if (esAdministrador) {

                            destino = "/admin";

                        } else if (esVecino) {

                            destino = "/vecino";

                        } else if (esRecolector) {

                            // NUEVO - PASO 59
                            destino = "/recolector/panel";

                        } else {

                            destino = "/";
                        }

                        response.sendRedirect(
                            request.getContextPath()
                            + destino
                        );
                    }
                )

                .failureUrl("/login?error")

                .permitAll()
            )

            // =====================================
            // CIERRE DE SESION
            // =====================================

            .logout(logout -> logout

                .logoutUrl("/logout")

                .logoutSuccessUrl("/login?logout")

                .permitAll()
            );

        return http.build();
    }
}
