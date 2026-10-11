
package com.perurecicla.controller;

import com.perurecicla.model.Recolecta;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.MaterialRepository;
import com.perurecicla.repository.RecolectaRepository;
import com.perurecicla.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * PERU RECICLA - PASO 59
 *
 * Panel del recolector.
 *
 * Funciones:
 * - Validar al recolector autenticado
 * - Mostrar resumen de sus recolectas
 * - Mostrar estados de validacion
 * - Consultar sus ultimas operaciones
 * - Consultar cantidad de materiales
 *
 * Solo realiza consultas a MySQL.
 */
@Controller
@RequestMapping("/recolector")
public class PanelRecolectorController {

    private final UsuarioRepository usuarioRepository;

    private final RecolectaRepository recolectaRepository;

    private final MaterialRepository materialRepository;

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PanelRecolectorController(
            UsuarioRepository usuarioRepository,
            RecolectaRepository recolectaRepository,
            MaterialRepository materialRepository) {

        this.usuarioRepository = usuarioRepository;

        this.recolectaRepository = recolectaRepository;

        this.materialRepository = materialRepository;
    }

    // ==========================================
    // PANEL PRINCIPAL DEL RECOLECTOR
    // ==========================================

    @GetMapping({"/panel", ""})
    @Transactional(readOnly = true)
    public String mostrarPanel(
            Principal principal,
            Model model) {

        // ======================================
        // 1. VERIFICAR SESION
        // ======================================

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario recolector = usuarioRepository
            .findByCorreo(principal.getName())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuario no encontrado"
                )
            );

        // ======================================
        // 2. VERIFICAR ROL Y ESTADO
        // ======================================

        if (recolector.getRol()
                != Usuario.Rol.ROLE_recolector
                || recolector.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo a recolectores activos"
            );
        }

        // ======================================
        // 3. OBTENER SUS RECOLECTAS
        // ======================================

        Integer idRecolector =
            recolector.getId_usuario();

        List<Recolecta> recolectas =
            recolectaRepository.findAll()
                .stream()
                .filter(r ->
                    r.getRecolector() != null
                    && idRecolector.equals(
                        r.getRecolector()
                            .getId_usuario()
                    )
                )
                .toList();

        // ======================================
        // 4. CALCULAR INDICADORES
        // ======================================

        long totalRecolectas = recolectas.size();

        long pendientes = recolectas.stream()
            .filter(r ->
                r.getEstado_validacion()
                    == Recolecta.EstadoValidacion.pendiente
            )
            .count();

        long validadas = recolectas.stream()
            .filter(r ->
                r.getEstado_validacion()
                    == Recolecta.EstadoValidacion.validado
            )
            .count();

        long rechazadas = recolectas.stream()
            .filter(r ->
                r.getEstado_validacion()
                    == Recolecta.EstadoValidacion.rechazado
            )
            .count();

        long materialesRegistrados =
            materialRepository.count();

        // ======================================
        // 5. ULTIMAS CINCO RECOLECTAS
        // ======================================

        List<ResumenRecolecta> ultimasRecolectas =
            recolectas.stream()
                .sorted(
                    Comparator.comparing(
                        Recolecta::getFecha_recolecta,
                        Comparator.nullsFirst(
                            Comparator.naturalOrder()
                        )
                    ).reversed()
                )
                .limit(5)
                .map(ResumenRecolecta::new)
                .toList();

        // ======================================
        // 6. ENVIAR INFORMACION A LA VISTA
        // ======================================

        model.addAttribute(
            "recolector",
            recolector.getNombre() + " "
                + recolector.getApellido()
        );

        model.addAttribute(
            "correo",
            recolector.getCorreo()
        );

        model.addAttribute(
            "totalRecolectas",
            totalRecolectas
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
            "materialesRegistrados",
            materialesRegistrados
        );

        model.addAttribute(
            "ultimasRecolectas",
            ultimasRecolectas
        );

        return "panel-recolector";
    }

    // ==========================================
    // DATOS PARA LA TABLA
    // ==========================================

    public static class ResumenRecolecta {

        private final Integer id;
        private final String fecha;
        private final String vecino;
        private final String material;
        private final BigDecimal peso;
        private final BigDecimal puntos;
        private final String estado;

        public ResumenRecolecta(Recolecta recolecta) {

            this.id =
                recolecta.getId_recolecta();

            this.fecha =
                recolecta.getFecha_recolecta() == null
                    ? "Sin fecha"
                    : recolecta.getFecha_recolecta()
                        .format(FORMATO_FECHA);

            if (recolecta.getVecino() == null) {

                this.vecino = "No especificado";

            } else {

                this.vecino =
                    recolecta.getVecino().getNombre()
                    + " "
                    + recolecta.getVecino().getApellido();
            }

            this.material =
                recolecta.getMaterial() == null
                    ? "No especificado"
                    : recolecta.getMaterial()
                        .getTipo_material();

            this.peso =
                recolecta.getPeso() == null
                    ? BigDecimal.ZERO
                    : recolecta.getPeso();

            this.puntos =
                recolecta.getPuntos_generados() == null
                    ? BigDecimal.ZERO
                    : recolecta.getPuntos_generados();

            this.estado =
                recolecta.getEstado_validacion() == null
                    ? "Sin estado"
                    : recolecta.getEstado_validacion()
                        .name();
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
