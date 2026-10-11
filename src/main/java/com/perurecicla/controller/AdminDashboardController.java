
package com.perurecicla.controller;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.CanjeRepository;
import com.perurecicla.repository.UsuarioRepository;
import com.perurecicla.repository.RecompensaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * PERU RECICLA - PASO 54
 *
 * Dashboard principal del administrador.
 *
 * URL: /admin
 *
 * Consulta datos reales de:
 * - Usuarios
 * - Canjes
 * - Recompensas
 *
 * Solo lectura: no modifica los datos de MySQL.
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UsuarioRepository usuarioRepository;
    private final CanjeRepository canjeRepository;
    private final RecompensaRepository recompensaRepository;

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AdminDashboardController(
            UsuarioRepository usuarioRepository,
            CanjeRepository canjeRepository,
            RecompensaRepository recompensaRepository) {

        this.usuarioRepository = usuarioRepository;
        this.canjeRepository = canjeRepository;
        this.recompensaRepository = recompensaRepository;
    }

    // =====================================
    // INICIO DEL ADMINISTRADOR
    // =====================================

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public String dashboard(
            Principal principal,
            Model model) {

        // =====================================
        // VALIDAR SESION Y ADMINISTRADOR
        // =====================================

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario administrador =
            usuarioRepository
                .findByCorreo(principal.getName())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Usuario no encontrado"
                    )
                );

        if (administrador.getRol()
                != Usuario.Rol.ROLE_administrador
                || administrador.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo a administradores activos"
            );
        }

        // =====================================
        // CONSULTAR CANJES
        // =====================================

        List<Canje> canjes =
            canjeRepository.findAll();

        long totalCanjes = canjes.size();

        long pendientes = canjes.stream()
            .filter(c ->
                c.getEstado()
                    == Canje.EstadoCanje.pendiente
            )
            .count();

        long entregados = canjes.stream()
            .filter(c ->
                c.getEstado()
                    == Canje.EstadoCanje.entregado
            )
            .count();

        long cancelados = canjes.stream()
            .filter(c ->
                c.getEstado()
                    == Canje.EstadoCanje.cancelado
            )
            .count();

        // =====================================
        // CONSULTAR VECINOS
        // =====================================

        long totalVecinos =
            usuarioRepository.findAll()
                .stream()
                .filter(usuario ->
                    usuario.getRol()
                        == Usuario.Rol.ROLE_vecino
                )
                .count();

        // =====================================
        // CONSULTAR RECOMPENSAS
        // =====================================

        long totalRecompensas =
            recompensaRepository.count();

        // =====================================
        // ULTIMOS 5 CANJES
        // =====================================

        List<ResumenCanje> ultimosCanjes =
            canjes.stream()

                .sorted(
                    Comparator.comparing(
                        Canje::getFecha_canje,
                        Comparator.nullsFirst(
                            Comparator.naturalOrder()
                        )
                    ).reversed()
                )

                .limit(5)

                .map(ResumenCanje::new)

                .toList();

        // =====================================
        // ENVIAR DATOS A THYMELEAF
        // =====================================

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

        model.addAttribute(
            "totalCanjes",
            totalCanjes
        );

        model.addAttribute(
            "pendientes",
            pendientes
        );

        model.addAttribute(
            "entregados",
            entregados
        );

        model.addAttribute(
            "cancelados",
            cancelados
        );

        model.addAttribute(
            "totalVecinos",
            totalVecinos
        );

        model.addAttribute(
            "totalRecompensas",
            totalRecompensas
        );

        model.addAttribute(
            "ultimosCanjes",
            ultimosCanjes
        );

        return "admin-dashboard";
    }

    // =====================================
    // DATOS PARA LA TABLA DE CANJES
    // =====================================

    public static class ResumenCanje {

        private final String codigo;
        private final String vecino;
        private final String recompensa;
        private final Integer puntos;
        private final String fecha;
        private final String estado;

        public ResumenCanje(Canje canje) {

            this.codigo =
                canje.getCodigo_ticket() == null
                    ? "Sin codigo"
                    : canje.getCodigo_ticket();

            if (canje.getUsuario() != null) {

                this.vecino =
                    canje.getUsuario().getNombre()
                    + " "
                    + canje.getUsuario().getApellido();

            } else {

                this.vecino = "No especificado";
            }

            this.recompensa =
                canje.getRecompensa() == null
                    ? "No especificada"
                    : canje.getRecompensa().getNombre();

            this.puntos =
                canje.getPuntos_descontados() == null
                    ? 0
                    : canje.getPuntos_descontados();

            this.fecha =
                canje.getFecha_canje() == null
                    ? "Sin fecha"
                    : canje.getFecha_canje()
                        .format(FORMATO_FECHA);

            this.estado =
                canje.getEstado() == null
                    ? "Sin estado"
                    : canje.getEstado().name();
        }

        public String getCodigo() {
            return codigo;
        }

        public String getVecino() {
            return vecino;
        }

        public String getRecompensa() {
            return recompensa;
        }

        public Integer getPuntos() {
            return puntos;
        }

        public String getFecha() {
            return fecha;
        }

        public String getEstado() {
            return estado;
        }
    }
}
