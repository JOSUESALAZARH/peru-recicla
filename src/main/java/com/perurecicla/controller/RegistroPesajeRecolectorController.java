package com.perurecicla.controller;

import com.perurecicla.model.Material;
import com.perurecicla.model.Recolecta;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.MaterialRepository;
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
import java.math.RoundingMode;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;

/**
 * PERU RECICLA - PASO 82
 *
 * Registro de pesajes con materiales activos.
 *
 * Se conservan las validaciones del
 * vecino, recolector, peso y puntos.
 */
@Controller
@RequestMapping("/recolector/pesajes")
public class RegistroPesajeRecolectorController {

    private final UsuarioRepository usuarioRepository;

    private final MaterialRepository materialRepository;

    private final RecolectaService recolectaService;

    private static final BigDecimal PESO_MAXIMO =
        new BigDecimal("1000000.00");

    private static final BigDecimal PUNTOS_MAXIMOS =
        new BigDecimal("99999999.99");

    public RegistroPesajeRecolectorController(
            UsuarioRepository usuarioRepository,
            MaterialRepository materialRepository,
            RecolectaService recolectaService) {

        this.usuarioRepository = usuarioRepository;
        this.materialRepository = materialRepository;
        this.recolectaService = recolectaService;
    }

    // =====================================
    // VALIDAR RECOLECTOR
    // =====================================

    private Usuario obtenerRecolector(
            Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario recolector =
            usuarioRepository
                .findByCorreo(principal.getName())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Usuario no encontrado"
                    )
                );

        if (recolector.getRol()
                != Usuario.Rol.ROLE_recolector
                || recolector.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo pueden registrar pesajes "
                + "los recolectores activos"
            );
        }

        return recolector;
    }

    // =====================================
    // MOSTRAR FORMULARIO
    // =====================================

    @GetMapping({"/nuevo", ""})
    @Transactional(readOnly = true)
    public String mostrarFormulario(
            Principal principal,
            Model model) {

        Usuario recolector =
            obtenerRecolector(principal);

        // Solo vecinos activos.

        List<Usuario> vecinos =
            usuarioRepository.findAll()
                .stream()
                .filter(u ->
                    u.getRol()
                        == Usuario.Rol.ROLE_vecino
                    && u.getEstado()
                        == Usuario.Estado.activo
                )
                .sorted(
                    Comparator.comparing(
                        Usuario::getApellido,
                        Comparator.nullsLast(
                            String.CASE_INSENSITIVE_ORDER
                        )
                    )
                )
                .toList();

        // NUEVO: solo materiales activos.

        List<Material> materiales =
            materialRepository
                .findByEstado(
                    Material.EstadoMaterial.activo
                )
                .stream()
                .filter(m ->
                    m.getPuntos_por_gramo() != null
                    && m.getPuntos_por_gramo()
                        .compareTo(BigDecimal.ZERO) > 0
                )
                .sorted(
                    Comparator.comparing(
                        Material::getTipo_material,
                        Comparator.nullsLast(
                            String.CASE_INSENSITIVE_ORDER
                        )
                    )
                )
                .toList();

        model.addAttribute(
            "recolector",
            recolector.getNombre() + " "
                + recolector.getApellido()
        );

        model.addAttribute(
            "vecinos",
            vecinos
        );

        model.addAttribute(
            "materiales",
            materiales
        );

        return "recolector-pesaje";
    }

    // =====================================
    // REGISTRAR PESAJE
    // =====================================

    @PostMapping("/registrar")
    public String registrarPesaje(

            @RequestParam("idVecino")
            Integer idVecino,

            @RequestParam("idMaterial")
            Integer idMaterial,

            @RequestParam("pesoGramos")
            String pesoGramos,

            Principal principal,

            RedirectAttributes mensajes) {

        Usuario recolector =
            obtenerRecolector(principal);

        // =================================
        // VALIDAR VECINO
        // =================================

        Usuario vecino =
            idVecino == null
                ? null
                : usuarioRepository
                    .findById(idVecino)
                    .orElse(null);

        if (vecino == null
                || vecino.getRol()
                    != Usuario.Rol.ROLE_vecino
                || vecino.getEstado()
                    != Usuario.Estado.activo) {

            return error(
                mensajes,
                "Debes seleccionar un vecino activo."
            );
        }

        // =================================
        // VALIDAR MATERIAL
        // =================================

        Material material =
            idMaterial == null
                ? null
                : materialRepository
                    .findById(idMaterial)
                    .orElse(null);

        if (material == null
                || material.getEstado()
                    != Material.EstadoMaterial.activo
                || material.getPuntos_por_gramo() == null
                || material.getPuntos_por_gramo()
                    .compareTo(BigDecimal.ZERO) <= 0) {

            return error(
                mensajes,
                "Selecciona un material activo "
                + "con factor de puntos valido."
            );
        }

        // =================================
        // VALIDAR PESO
        // =================================

        String textoPeso =
            pesoGramos == null
                ? ""
                : pesoGramos.strip();

        if (!textoPeso.matches(
                "[0-9]{1,7}([.,][0-9]{1,2})?"
            )) {

            return error(
                mensajes,
                "Ingresa un peso valido en gramos "
                + "con hasta dos decimales."
            );
        }

        BigDecimal peso;

        try {

            peso = new BigDecimal(
                textoPeso.replace(',', '.')
            );

        } catch (NumberFormatException e) {

            return error(
                mensajes,
                "El peso ingresado no es valido."
            );
        }

        if (peso.compareTo(BigDecimal.ZERO) <= 0
                || peso.compareTo(PESO_MAXIMO) > 0) {

            return error(
                mensajes,
                "El peso debe ser mayor a cero "
                + "y no superar 1000000 gramos."
            );
        }

        // =================================
        // COMPROBAR PUNTOS ESTIMADOS
        // =================================

        BigDecimal puntosEstimados =
            peso.multiply(
                material.getPuntos_por_gramo()
            )
            .setScale(
                2,
                RoundingMode.HALF_UP
            );

        if (puntosEstimados.compareTo(
                BigDecimal.ZERO) <= 0) {

            return error(
                mensajes,
                "Este pesaje genera menos de "
                + "0.01 puntos. Verifica el peso."
            );
        }

        if (puntosEstimados.compareTo(
                PUNTOS_MAXIMOS) > 0) {

            return error(
                mensajes,
                "El valor de puntos supera "
                + "el limite permitido."
            );
        }

        // =================================
        // CREAR RECOLECTA
        // =================================

        Recolecta recolecta =
            new Recolecta();

        recolecta.setVecino(
            vecino
        );

        recolecta.setRecolector(
            recolector
        );

        recolecta.setMaterial(
            material
        );

        recolecta.setPeso(
            peso
        );

        // El servicio recalcula la tarifa
        // vigente y guarda puntos_generados.
        // No recibe puntos desde el navegador.

        try {

            Recolecta guardada =
                recolectaService.registrarPeso(
                    recolecta
                );

            mensajes.addFlashAttribute(
                "exito",
                "Recolecta N.° "
                + guardada.getId_recolecta()
                + " registrada correctamente. "
                + "Puntos calculados: "
                + guardada.getPuntos_generados()
                + ". Estado: pendiente."
            );

            return "redirect:/recolector/pesajes/nuevo";

        } catch (IllegalArgumentException e) {

            return error(
                mensajes,
                "No se pudo registrar el pesaje. "
                + e.getMessage()
            );
        }
    }

    // =====================================
    // MOSTRAR ERRORES
    // =====================================

    private String error(
            RedirectAttributes mensajes,
            String mensaje) {

        mensajes.addFlashAttribute(
            "error",
            mensaje
        );

        return "redirect:/recolector/pesajes/nuevo";
    }
}