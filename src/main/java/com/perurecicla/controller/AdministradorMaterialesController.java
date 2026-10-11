package com.perurecicla.controller;

import com.perurecicla.model.Material;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.UsuarioRepository;
import com.perurecicla.service.MaterialService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;

/**
 * PERU RECICLA - PASO 82
 *
 * Gestion administrativa de materiales.
 *
 * - Listar materiales.
 * - Consultar indicadores.
 * - Registrar materiales.
 * - Editar materiales.
 * - Activar y desactivar materiales.
 *
 * Nunca elimina historiales.
 */
@Controller
@RequestMapping("/admin/materiales")
public class AdministradorMaterialesController {

    private final MaterialService materialService;

    private final UsuarioRepository usuarioRepository;

    public AdministradorMaterialesController(
            MaterialService materialService,
            UsuarioRepository usuarioRepository) {

        this.materialService = materialService;
        this.usuarioRepository = usuarioRepository;
    }

    // =====================================
    // LISTADO Y FORMULARIO
    // =====================================

    @GetMapping
    public String listar(

            @RequestParam(
                name = "nuevo",
                defaultValue = "false"
            )
            boolean nuevo,

            @RequestParam(
                name = "editar",
                required = false
            )
            Integer editar,

            Principal principal,

            Model model) {

        Usuario admin =
            validarAdministrador(principal);

        List<Material> materiales =
            materialService.listarTodos()
                .stream()
                .sorted(
                    Comparator.comparing(
                        Material::getId_material,
                        Comparator.nullsLast(
                            Comparator.reverseOrder()
                        )
                    )
                )
                .toList();

        model.addAttribute(
            "administrador",
            admin.getNombre()
        );

        model.addAttribute(
            "materiales",
            materiales
        );

        model.addAttribute(
            "totalMateriales",
            materiales.size()
        );

        model.addAttribute(
            "totalActivos",
            materiales.stream()
                .filter(m ->
                    m.getEstado()
                        == Material.EstadoMaterial.activo
                )
                .count()
        );

        model.addAttribute(
            "totalInactivos",
            materiales.stream()
                .filter(m ->
                    m.getEstado()
                        == Material.EstadoMaterial.inactivo
                )
                .count()
        );

        // Mostrar formulario solo cuando
        // se solicita crear o editar.

        Material materialFormulario = null;

        if (editar != null) {

            materialFormulario =
                materialService.buscarPorId(editar);

            if (materialFormulario == null) {

                model.addAttribute(
                    "errorPagina",
                    "Material no encontrado."
                );
            }

        } else if (nuevo) {

            materialFormulario = new Material();
        }

        model.addAttribute(
            "materialFormulario",
            materialFormulario
        );

        model.addAttribute(
            "editando",
            materialFormulario != null
                && materialFormulario.getId_material() != null
        );

        return "admin-materiales";
    }

    // =====================================
    // GUARDAR MATERIAL NUEVO O EDITADO
    // =====================================

    @PostMapping("/guardar")
    public String guardar(

            @RequestParam(
                name = "id",
                required = false
            )
            Integer id,

            @RequestParam("tipo_material")
            String nombre,

            @RequestParam("puntos_por_gramo")
            String puntosTexto,

            @RequestParam(
                name = "descripcion",
                defaultValue = ""
            )
            String descripcion,

            @RequestParam("estado")
            String estado,

            Principal principal,

            RedirectAttributes mensajes) {

        validarAdministrador(principal);

        try {

            if (nombre == null || nombre.isBlank()) {

                throw new IllegalArgumentException(
                    "El nombre es obligatorio."
                );
            }

            if (descripcion != null
                    && descripcion.length() > 2000) {

                throw new IllegalArgumentException(
                    "La descripcion no puede superar "
                    + "2000 caracteres."
                );
            }

            BigDecimal puntos;

            try {

                puntos = new BigDecimal(
                    puntosTexto.trim().replace(',', '.')
                );

            } catch (RuntimeException ex) {

                throw new IllegalArgumentException(
                    "Ingresa un valor valido "
                    + "de puntos por gramo."
                );
            }

            Material.EstadoMaterial estadoMaterial;

            try {

                estadoMaterial =
                    Material.EstadoMaterial.valueOf(
                        estado
                    );

            } catch (RuntimeException ex) {

                throw new IllegalArgumentException(
                    "Selecciona un estado valido."
                );
            }

            Material datos = new Material();

            datos.setTipo_material(
                nombre.trim()
            );

            datos.setPuntos_por_gramo(
                puntos
            );

            datos.setDescripcion(
                descripcion == null
                    ? ""
                    : descripcion.trim()
            );

            datos.setEstado(
                estadoMaterial
            );

            if (id == null) {

                materialService.guardar(datos);

                mensajes.addFlashAttribute(
                    "exito",
                    "Material registrado correctamente."
                );

            } else {

                Material anterior =
                    materialService.buscarPorId(id);

                if (anterior == null) {

                    throw new IllegalArgumentException(
                        "El material no existe."
                    );
                }

                // Conservar la imagen original.
                // Este formulario no la edita.

                datos.setUrl_imagen(
                    anterior.getUrl_imagen()
                );

                materialService.actualizar(
                    id,
                    datos
                );

                mensajes.addFlashAttribute(
                    "exito",
                    "Material actualizado correctamente."
                );
            }

            return "redirect:/admin/materiales";

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            mensajes.addFlashAttribute(
                "error",
                ex.getMessage()
            );

            return id == null
                ? "redirect:/admin/materiales?nuevo=true"
                : "redirect:/admin/materiales?editar=" + id;
        }
    }

    // =====================================
    // CAMBIAR DISPONIBILIDAD
    // =====================================

    @PostMapping("/cambiar-estado")
    public String cambiarEstado(

            @RequestParam("id")
            Integer id,

            @RequestParam("estado")
            String estado,

            Principal principal,

            RedirectAttributes mensajes) {

        validarAdministrador(principal);

        try {

            Material.EstadoMaterial nuevo;

            try {

                nuevo =
                    Material.EstadoMaterial.valueOf(
                        estado
                    );

            } catch (RuntimeException ex) {

                throw new IllegalArgumentException(
                    "Estado invalido."
                );
            }

            Material actual =
                materialService.buscarPorId(id);

            if (actual == null) {

                throw new IllegalArgumentException(
                    "El material no existe."
                );
            }

            Material datos = new Material();

            datos.setTipo_material(
                actual.getTipo_material()
            );

            datos.setPuntos_por_gramo(
                actual.getPuntos_por_gramo()
            );

            datos.setDescripcion(
                actual.getDescripcion()
            );

            datos.setUrl_imagen(
                actual.getUrl_imagen()
            );

            datos.setEstado(
                nuevo
            );

            materialService.actualizar(
                id,
                datos
            );

            mensajes.addFlashAttribute(
                "exito",
                "Estado actualizado correctamente."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            mensajes.addFlashAttribute(
                "error",
                ex.getMessage()
            );
        }

        return "redirect:/admin/materiales";
    }

    // =====================================
    // PROTEGER ACCESO ADMINISTRATIVO
    // =====================================

    private Usuario validarAdministrador(
            Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario usuario =
            usuarioRepository
                .findByCorreo(principal.getName())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Administrador no encontrado"
                    )
                );

        if (usuario.getRol()
                != Usuario.Rol.ROLE_administrador
                || usuario.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso solo para administradores activos"
            );
        }

        return usuario;
    }
}