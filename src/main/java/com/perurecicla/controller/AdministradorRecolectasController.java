
package com.perurecicla.controller;

import com.perurecicla.model.Recolecta;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.RecolectaRepository;
import com.perurecicla.repository.UsuarioRepository;

import com.perurecicla.service.RecolectaService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * PERU RECICLA - PASO 61
 *
 * Modulo administrativo de validacion
 * de recolectas.
 */
@Controller
@RequestMapping("/admin/recolectas")
public class AdministradorRecolectasController {

    private final UsuarioRepository usuarioRepository;

    private final RecolectaRepository recolectaRepository;

    private final RecolectaService recolectaService;

    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AdministradorRecolectasController(
            UsuarioRepository usuarioRepository,
            RecolectaRepository recolectaRepository,
            RecolectaService recolectaService) {

        this.usuarioRepository = usuarioRepository;
        this.recolectaRepository = recolectaRepository;
        this.recolectaService = recolectaService;
    }

    // =====================================
    // VERIFICAR ADMINISTRADOR ACTIVO
    // =====================================

    private Usuario administradorActivo(
            Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario usuario = usuarioRepository
            .findByCorreo(principal.getName())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuario no encontrado"
                )
            );

        if (usuario.getRol()
                != Usuario.Rol.ROLE_administrador
                || usuario.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo a administradores activos"
            );
        }

        return usuario;
    }

    // =====================================
    // MOSTRAR LISTADO DE RECOLECTAS
    // =====================================

    @GetMapping
    @Transactional(readOnly = true)
    public String mostrarRecolectas(

            @RequestParam(
                name = "estado",
                defaultValue = "todos"
            ) String estado,

            Principal principal,
            Model model) {

        Usuario administrador =
            administradorActivo(principal);

        String filtro = switch (estado) {

            case "pendiente", "validado", "rechazado" ->
                estado;

            default -> "todos";
        };

        List<Recolecta> todas =
            recolectaRepository.findAll();

        long pendientes = todas.stream()
            .filter(r ->
                r.getEstado_validacion()
                    == Recolecta.EstadoValidacion.pendiente
            )
            .count();

        long validadas = todas.stream()
            .filter(r ->
                r.getEstado_validacion()
                    == Recolecta.EstadoValidacion.validado
            )
            .count();

        long rechazadas = todas.stream()
            .filter(r ->
                r.getEstado_validacion()
                    == Recolecta.EstadoValidacion.rechazado
            )
            .count();

        List<FilaRecolecta> filas =
            todas.stream()

                .filter(r ->
                    filtro.equals("todos")
                    || (
                        r.getEstado_validacion() != null
                        && r.getEstado_validacion()
                            .name().equals(filtro)
                    )
                )

                .sorted(
                    Comparator.comparing(
                        Recolecta::getId_recolecta,
                        Comparator.nullsLast(
                            Comparator.reverseOrder()
                        )
                    )
                )

                .map(FilaRecolecta::new)

                .toList();

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

        model.addAttribute(
            "totalRecolectas",
            todas.size()
        );

        model.addAttribute(
            "pendientes",
            pendientes
        );

        model.addAttribute(
            "validadas",
            validadas
        );

        model.addAttribute(
            "rechazadas",
            rechazadas
        );

        model.addAttribute(
            "filtro",
            filtro
        );

        model.addAttribute(
            "recolectas",
            filas
        );

        return "admin-recolectas";
    }

    // =====================================
    // APROBAR O RECHAZAR
    // =====================================

    @PostMapping("/procesar")
    public String procesarRecolecta(

            @RequestParam("idRecolecta")
            Integer idRecolecta,

            @RequestParam("accion")
            String accion,

            Principal principal,

            RedirectAttributes flash) {

        administradorActivo(principal);

        if (!"validado".equals(accion)
                && !"rechazado".equals(accion)) {

            flash.addFlashAttribute(
                "error",
                "Accion no permitida."
            );

            return "redirect:/admin/recolectas";
        }

        try {

            recolectaService.validarRecolecta(
                idRecolecta,
                accion
            );

            String mensaje;

            if ("validado".equals(accion)) {

                mensaje =
                    "Recolecta N.° "
                    + idRecolecta
                    + " aprobada. Puntos acreditados al vecino.";

            } else {

                mensaje =
                    "Recolecta N.° "
                    + idRecolecta
                    + " rechazada. No se acreditaron puntos.";
            }

            flash.addFlashAttribute(
                "exito",
                mensaje
            );

        } catch (
            IllegalArgumentException
            | IllegalStateException ex
        ) {

            flash.addFlashAttribute(
                "error",
                ex.getMessage()
            );

        } catch (RuntimeException ex) {

            flash.addFlashAttribute(
                "error",
                "No se pudo procesar la recolecta. "
                + "Intenta nuevamente."
            );
        }

        return "redirect:/admin/recolectas";
    }

    // =====================================
    // DATOS PARA LA TABLA
    // =====================================

    public static class FilaRecolecta {

        private final Integer id;
        private final String fecha;
        private final String vecino;
        private final String recolector;
        private final String material;
        private final BigDecimal peso;
        private final BigDecimal puntos;
        private final String estado;

        public FilaRecolecta(Recolecta r) {

            this.id =
                r.getId_recolecta();

            this.fecha =
                r.getFecha_recolecta() == null
                    ? "Sin fecha"
                    : r.getFecha_recolecta()
                        .format(FECHA);

            this.vecino =
                nombre(r.getVecino());

            this.recolector =
                nombre(r.getRecolector());

            this.material =
                r.getMaterial() == null
                    ? "Sin material"
                    : r.getMaterial()
                        .getTipo_material();

            this.peso =
                r.getPeso() == null
                    ? BigDecimal.ZERO
                    : r.getPeso();

            this.puntos =
                r.getPuntos_generados() == null
                    ? BigDecimal.ZERO
                    : r.getPuntos_generados();

            this.estado =
                r.getEstado_validacion() == null
                    ? "Sin estado"
                    : r.getEstado_validacion()
                        .name();
        }

        private static String nombre(Usuario u) {

            if (u == null) {
                return "No especificado";
            }

            String nombre =
                u.getNombre() == null
                    ? ""
                    : u.getNombre();

            String apellido =
                u.getApellido() == null
                    ? ""
                    : u.getApellido();

            return nombre + " " + apellido;
        }

        public Integer getId() {
            return id;
        }

        public String getFecha() {
            return fecha;
        }

        public String getVecino() {
            return vecino;
        }

        public String getRecolector() {
            return recolector;
        }

        public String getMaterial() {
            return material;
        }

        public BigDecimal getPeso() {
            return peso;
        }

        public BigDecimal getPuntos() {
            return puntos;
        }

        public String getEstado() {
            return estado;
        }
    }
}
