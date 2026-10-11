package com.perurecicla.controller;

import com.perurecicla.model.Material;
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
 * PERU RECICLA - PASO 82
 *
 * Panel principal del recolector.
 *
 * CONSERVA:
 * - Validacion del recolector autenticado
 * - Total de recolectas
 * - Recolectas pendientes
 * - Recolectas validadas
 * - Recolectas rechazadas
 * - Ultimas cinco recolectas
 * - Historial reciente
 *
 * MEJORA:
 * - Cuenta solamente materiales activos
 *
 * CORRECCION:
 * - Peso y puntos usan BigDecimal
 * - Evita incompatibilidades con String
 */
@Controller
@RequestMapping("/recolector")
public class PanelRecolectorController {

    // =====================================
    // REPOSITORIOS
    // =====================================

    private final UsuarioRepository usuarioRepository;

    private final RecolectaRepository recolectaRepository;

    private final MaterialRepository materialRepository;

    // =====================================
    // FORMATO DE FECHA
    // =====================================

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm"
        );

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public PanelRecolectorController(
            UsuarioRepository usuarioRepository,
            RecolectaRepository recolectaRepository,
            MaterialRepository materialRepository) {

        this.usuarioRepository = usuarioRepository;

        this.recolectaRepository = recolectaRepository;

        this.materialRepository = materialRepository;
    }

    // =====================================
    // PANEL PRINCIPAL
    // =====================================

    @GetMapping({"/panel", ""})
    @Transactional(readOnly = true)
    public String mostrarPanel(
            Principal principal,
            Model model) {

        // =================================
        // 1. VALIDAR SESION
        // =================================

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        // =================================
        // 2. OBTENER RECOLECTOR
        // =================================

        Usuario recolector =
            usuarioRepository
                .findByCorreo(
                    principal.getName()
                )
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Usuario no encontrado"
                    )
                );

        // =================================
        // 3. VALIDAR ROL Y ESTADO
        // =================================

        if (recolector.getRol()
                != Usuario.Rol.ROLE_recolector
                || recolector.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo a recolectores activos"
            );
        }

        // =================================
        // 4. OBTENER SUS RECOLECTAS
        // =================================

        Integer idRecolector =
            recolector.getId_usuario();

        List<Recolecta> recolectas =
            recolectaRepository
                .findAll()
                .stream()
                .filter(r ->
                    r.getRecolector() != null
                    && idRecolector.equals(
                        r.getRecolector()
                            .getId_usuario()
                    )
                )
                .toList();

        // =================================
        // 5. INDICADORES DE RECOLECTAS
        // =================================

        long totalRecolectas =
            recolectas.size();

        long pendientes =
            recolectas.stream()
                .filter(r ->
                    r.getEstado_validacion()
                        == Recolecta.EstadoValidacion.pendiente
                )
                .count();

        long validadas =
            recolectas.stream()
                .filter(r ->
                    r.getEstado_validacion()
                        == Recolecta.EstadoValidacion.validado
                )
                .count();

        long rechazadas =
            recolectas.stream()
                .filter(r ->
                    r.getEstado_validacion()
                        == Recolecta.EstadoValidacion.rechazado
                )
                .count();

        // =================================
        // 6. MATERIALES ACTIVOS
        //
        // PASO 82:
        // Ya no cuenta todos los materiales.
        // Solo considera los activos.
        // =================================

        long materialesRegistrados =
            materialRepository.countByEstado(
                Material.EstadoMaterial.activo
            );

        // =================================
        // 7. ULTIMAS CINCO RECOLECTAS
        // =================================

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

        // =================================
        // 8. DATOS DEL RECOLECTOR
        // =================================

        model.addAttribute(
            "recolector",
            recolector.getNombre() + " "
                + recolector.getApellido()
        );

        model.addAttribute(
            "correo",
            recolector.getCorreo()
        );

        // =================================
        // 9. ENVIAR INDICADORES
        // =================================

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

        // =================================
        // 10. ENVIAR HISTORIAL RECIENTE
        // =================================

        model.addAttribute(
            "ultimasRecolectas",
            ultimasRecolectas
        );

        return "panel-recolector";
    }

    // =====================================
    // RESUMEN DE CADA RECOLECTA
    // =====================================

    public static class ResumenRecolecta {

        // =================================
        // ATRIBUTOS
        // =================================

        private final Integer id;

        private final String fecha;

        private final String vecino;

        private final String material;

        // CORRECCION:
        // Ambos campos deben ser BigDecimal.

        private final BigDecimal peso;

        private final BigDecimal puntos;

        private final String estado;

        // =================================
        // CONSTRUCTOR
        // =================================

        public ResumenRecolecta(
                Recolecta recolecta) {

            // ID

            this.id =
                recolecta.getId_recolecta();

            // FECHA

            this.fecha =
                recolecta.getFecha_recolecta() == null
                    ? "Sin fecha"
                    : recolecta.getFecha_recolecta()
                        .format(FORMATO_FECHA);

            // VECINO

            if (recolecta.getVecino() == null) {

                this.vecino =
                    "No especificado";

            } else {

                this.vecino =
                    recolecta.getVecino().getNombre()
                    + " "
                    + recolecta.getVecino().getApellido();
            }

            // MATERIAL

            this.material =
                recolecta.getMaterial() == null
                    ? "No especificado"
                    : recolecta.getMaterial()
                        .getTipo_material();

            // =================================
            // PESO - BIGDECIMAL
            // =================================

            this.peso =
                recolecta.getPeso() == null
                    ? BigDecimal.ZERO
                    : recolecta.getPeso();

            // =================================
            // PUNTOS - BIGDECIMAL
            // =================================

            this.puntos =
                recolecta.getPuntos_generados() == null
                    ? BigDecimal.ZERO
                    : recolecta.getPuntos_generados();

            // =================================
            // ESTADO
            // =================================

            this.estado =
                recolecta.getEstado_validacion() == null
                    ? "Sin estado"
                    : recolecta.getEstado_validacion()
                        .name();
        }

        // =================================
        // GETTERS
        // =================================

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