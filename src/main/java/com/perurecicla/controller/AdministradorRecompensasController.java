package com.perurecicla.controller;

import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.UsuarioRepository;
import com.perurecicla.service.RecompensaService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * PERU RECICLA - PASO 74
 *
 * Gestion administrativa de recompensas.
 *
 * Funciones:
 * - Listar recompensas
 * - Registrar productos
 * - Editar productos
 * - Modificar stock, puntos, categoria y estado
 * - Registrar URL de imagen
 * - Subir fotografias desde la computadora
 * - Reemplazar o quitar fotografias
 */
@Controller
@RequestMapping("/admin/recompensas")
public class AdministradorRecompensasController {

    private static final long MAX_IMAGEN =
            5L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;

    private final RecompensaService recompensaService;

    public AdministradorRecompensasController(
            UsuarioRepository usuarioRepository,
            RecompensaService recompensaService) {

        this.usuarioRepository = usuarioRepository;
        this.recompensaService = recompensaService;
    }

    // =====================================
    // VALIDAR ADMINISTRADOR
    // =====================================

    private Usuario validarAdministrador(
            Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario administrador = usuarioRepository
            .findByCorreo(principal.getName())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrador no encontrado"
                )
            );

        if (administrador.getRol()
                != Usuario.Rol.ROLE_administrador
                || administrador.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para administradores activos"
            );
        }

        return administrador;
    }

    // =====================================
    // LISTAR RECOMPENSAS
    // =====================================

    @GetMapping
    public String listarRecompensas(
            Principal principal,
            Model model) {

        Usuario administrador =
                validarAdministrador(principal);

        List<Recompensa> recompensas =
                recompensaService.listarTodas()
                    .stream()
                    .sorted(
                        Comparator.comparing(
                            Recompensa::getId_recompensa,
                            Comparator.nullsLast(
                                Comparator.reverseOrder()
                            )
                        )
                    )
                    .toList();

        long activas = recompensas.stream()
            .filter(r ->
                r.getEstado()
                    == Recompensa.EstadoRecompensa.activo
            )
            .count();

        long inactivas = recompensas.stream()
            .filter(r ->
                r.getEstado()
                    == Recompensa.EstadoRecompensa.inactivo
            )
            .count();

        long sinStock = recompensas.stream()
            .filter(r ->
                r.getStock() == null
                || r.getStock() == 0
            )
            .count();

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

        model.addAttribute("recompensas", recompensas);

        model.addAttribute(
            "totalRecompensas",
            recompensas.size()
        );

        model.addAttribute("activas", activas);
        model.addAttribute("inactivas", inactivas);
        model.addAttribute("sinStock", sinStock);

        return "admin-recompensas";
    }

    // =====================================
    // FORMULARIO NUEVA RECOMPENSA
    // =====================================

    @GetMapping("/nuevo")
    public String nuevaRecompensa(
            Principal principal,
            Model model) {

        validarAdministrador(principal);

        Recompensa recompensa = new Recompensa();

        recompensa.setEstado(
            Recompensa.EstadoRecompensa.activo
        );

        recompensa.setStock(0);

        prepararFormulario(model, recompensa, false);

        return "admin-recompensa-form";
    }

    // =====================================
    // FORMULARIO EDITAR RECOMPENSA
    // =====================================

    @GetMapping("/editar/{id}")
    public String editarRecompensa(
            @PathVariable Integer id,
            Principal principal,
            Model model) {

        validarAdministrador(principal);

        Recompensa recompensa =
                recompensaService.buscarPorId(id);

        if (recompensa == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Recompensa no encontrada"
            );
        }

        prepararFormulario(model, recompensa, true);

        return "admin-recompensa-form";
    }

    private void prepararFormulario(
            Model model,
            Recompensa recompensa,
            boolean esEdicion) {

        model.addAttribute("recompensa", recompensa);

        model.addAttribute(
            "categorias",
            Recompensa.Categoria.values()
        );

        model.addAttribute(
            "estados",
            Recompensa.EstadoRecompensa.values()
        );

        model.addAttribute("esEdicion", esEdicion);
    }

    // =====================================
    // GUARDAR RECOMPENSA Y FOTOGRAFIA
    // =====================================

    @PostMapping("/guardar")
    public String guardarRecompensa(

            @RequestParam(
                name = "id",
                required = false
            )
            Integer id,

            @RequestParam("nombre")
            String nombre,

            @RequestParam("categoria")
            String categoria,

            @RequestParam(
                name = "descripcion",
                defaultValue = ""
            )
            String descripcion,

            @RequestParam("costo_puntos")
            String costoPuntos,

            @RequestParam("stock")
            String stock,

            @RequestParam(
                name = "url_imagen",
                defaultValue = ""
            )
            String urlImagen,

            @RequestParam(
                name = "imagen",
                required = false
            )
            MultipartFile imagen,

            @RequestParam(
                name = "quitarImagen",
                defaultValue = "false"
            )
            boolean quitarImagen,

            @RequestParam("estado")
            String estado,

            Principal principal,

            RedirectAttributes redirect) {

        validarAdministrador(principal);

        boolean esEdicion = id != null;

        Path archivoNuevo = null;

        try {

            // =================================
            // 1. VALIDAR DATOS
            // =================================

            String nombreLimpio =
                nombre == null ? "" : nombre.trim();

            if (nombreLimpio.isBlank()
                    || nombreLimpio.length() > 100) {

                throw new IllegalArgumentException(
                    "El nombre debe tener entre "
                    + "1 y 100 caracteres."
                );
            }

            String descripcionLimpia =
                descripcion == null
                    ? ""
                    : descripcion.trim();

            if (descripcionLimpia.length() > 2000) {

                throw new IllegalArgumentException(
                    "La descripcion no debe superar "
                    + "2000 caracteres."
                );
            }

            Recompensa.Categoria categoriaEnum;
            Recompensa.EstadoRecompensa estadoEnum;

            try {

                categoriaEnum =
                    Recompensa.Categoria.valueOf(
                        categoria
                    );

                estadoEnum =
                    Recompensa.EstadoRecompensa.valueOf(
                        estado
                    );

            } catch (IllegalArgumentException
                     | NullPointerException ex) {

                throw new IllegalArgumentException(
                    "Selecciona una categoria "
                    + "y un estado validos."
                );
            }

            int puntos = leerEntero(
                costoPuntos,
                "El costo en puntos",
                false
            );

            int cantidadStock = leerEntero(
                stock,
                "El stock",
                true
            );

            // =================================
            // 2. CONSULTAR DATOS EXISTENTES
            // =================================

            Recompensa recompensa;

            if (esEdicion) {

                recompensa =
                    recompensaService.buscarPorId(id);

                if (recompensa == null) {

                    throw new IllegalArgumentException(
                        "La recompensa no existe."
                    );
                }

            } else {

                recompensa = new Recompensa();
            }

            String imagenAnterior =
                    recompensa.getUrl_imagen();

            // =================================
            // 3. ACTUALIZAR DATOS
            // =================================

            recompensa.setNombre(nombreLimpio);

            recompensa.setCategoria(categoriaEnum);

            recompensa.setDescripcion(
                descripcionLimpia
            );

            recompensa.setCosto_puntos(puntos);

            recompensa.setStock(cantidadStock);

            recompensa.setEstado(estadoEnum);

            // =================================
            // 4. DETERMINAR IMAGEN
            // =================================

            String nuevaUrl;

            if (imagen != null && !imagen.isEmpty()) {

                // Prioridad 1:
                // fotografia seleccionada

                archivoNuevo = almacenarImagen(imagen);

                nuevaUrl =
                    "/images/recompensas/"
                    + archivoNuevo.getFileName();

            } else if (quitarImagen) {

                // Prioridad 2:
                // quitar fotografia

                nuevaUrl = null;

            } else if (urlImagen != null
                    && !urlImagen.isBlank()) {

                // Prioridad 3:
                // direccion de imagen

                if (urlImagen.trim().equals(
                        imagenAnterior)) {

                    nuevaUrl = imagenAnterior;

                } else {

                    nuevaUrl =
                        validarUrlImagen(urlImagen);
                }

            } else {

                // Prioridad 4:
                // conservar imagen existente

                nuevaUrl = imagenAnterior;
            }

            recompensa.setUrl_imagen(nuevaUrl);

            // =================================
            // 5. GUARDAR EN MYSQL
            // =================================

            recompensaService.guardar(recompensa);

            redirect.addFlashAttribute(
                "exito",
                esEdicion
                    ? "Recompensa actualizada correctamente."
                    : "Recompensa registrada correctamente."
            );

            return "redirect:/admin/recompensas";

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            eliminarArchivoFallido(archivoNuevo);

            redirect.addFlashAttribute(
                "error",
                ex.getMessage()
            );

        } catch (IOException ex) {

            eliminarArchivoFallido(archivoNuevo);

            redirect.addFlashAttribute(
                "error",
                "No se pudo guardar la fotografia."
            );

        } catch (RuntimeException ex) {

            eliminarArchivoFallido(archivoNuevo);

            redirect.addFlashAttribute(
                "error",
                "No se pudo guardar la recompensa."
            );
        }

        if (esEdicion) {

            return "redirect:/admin/recompensas/editar/"
                + id;
        }

        return "redirect:/admin/recompensas/nuevo";
    }

    // =====================================
    // VALIDAR NUMEROS
    // =====================================

    private int leerEntero(
            String texto,
            String campo,
            boolean permiteCero) {

        String valor =
            texto == null ? "" : texto.trim();

        if (!valor.matches("[0-9]{1,9}")) {

            throw new IllegalArgumentException(
                campo + " debe ser un entero valido."
            );
        }

        int numero = Integer.parseInt(valor);

        if (!permiteCero && numero <= 0) {

            throw new IllegalArgumentException(
                campo + " debe ser mayor que cero."
            );
        }

        return numero;
    }

    // =====================================
    // VALIDAR URL DE IMAGEN
    // =====================================

    private String validarUrlImagen(String valor) {

        String url = valor.trim();

        if (url.length() > 255) {

            throw new IllegalArgumentException(
                "La direccion de imagen es muy larga."
            );
        }

        // URLs externas HTTPS

        if (url.startsWith("https://")) {

            try {

                URI uri = URI.create(url);

                if (!"https".equalsIgnoreCase(
                        uri.getScheme())
                        || uri.getHost() == null
                        || uri.getRawUserInfo() != null) {

                    throw new IllegalArgumentException(
                        "La URL de imagen no es valida."
                    );
                }

                return url;

            } catch (IllegalArgumentException ex) {

                throw new IllegalArgumentException(
                    "La URL de imagen no es valida."
                );
            }
        }

        // Archivos internos de PERU RECICLA

        if (url.matches(
                "/images/(recompensas/)?[A-Za-z0-9_.-]+")
                && !url.contains("..")) {

            return url;
        }

        // Nombre simple de archivo

        if (url.matches("[A-Za-z0-9_.-]+")
                && !url.contains("..")) {

            return url;
        }

        throw new IllegalArgumentException(
            "Utiliza una URL HTTPS o una imagen "
            + "de la carpeta /images/."
        );
    }

    // =====================================
    // ALMACENAR FOTOGRAFIA
    //
    // Utiliza el mismo directorio
    // del controlador de imagenes
    // existente.
    // =====================================

    private Path almacenarImagen(
            MultipartFile imagen) throws IOException {

        if (imagen.getSize() > MAX_IMAGEN) {

            throw new IllegalArgumentException(
                "La fotografia no debe superar 5 MB."
            );
        }

        String extension =
                detectarExtension(imagen);

        Path carpeta = Path.of(
            System.getProperty("user.home"),
            "PeruRecicla",
            "imagenes",
            "recompensas"
        ).toAbsolutePath().normalize();

        Files.createDirectories(carpeta);

        String nombre =
                UUID.randomUUID().toString()
                + extension;

        Path destino =
                carpeta.resolve(nombre).normalize();

        try (InputStream entrada =
                imagen.getInputStream()) {

            Files.copy(entrada, destino);

        } catch (IOException ex) {

            Files.deleteIfExists(destino);

            throw ex;
        }

        return destino;
    }

    // =====================================
    // COMPROBAR FORMATO DEL ARCHIVO
    // =====================================

    private String detectarExtension(
            MultipartFile imagen) throws IOException {

        byte[] firma;

        try (InputStream entrada =
                imagen.getInputStream()) {

            firma = entrada.readNBytes(12);
        }

        // JPG

        if (firma.length >= 3
                && (firma[0] & 0xFF) == 0xFF
                && (firma[1] & 0xFF) == 0xD8
                && (firma[2] & 0xFF) == 0xFF) {

            return ".jpg";
        }

        // PNG

        if (firma.length >= 8
                && (firma[0] & 0xFF) == 0x89
                && firma[1] == 0x50
                && firma[2] == 0x4E
                && firma[3] == 0x47
                && firma[4] == 0x0D
                && firma[5] == 0x0A
                && firma[6] == 0x1A
                && firma[7] == 0x0A) {

            return ".png";
        }

        // WEBP

        if (firma.length >= 12
                && firma[0] == 'R'
                && firma[1] == 'I'
                && firma[2] == 'F'
                && firma[3] == 'F'
                && firma[8] == 'W'
                && firma[9] == 'E'
                && firma[10] == 'B'
                && firma[11] == 'P') {

            return ".webp";
        }

        throw new IllegalArgumentException(
            "Solo se permiten imagenes "
            + "JPG, PNG o WebP."
        );
    }

    // =====================================
    // LIMPIAR ARCHIVOS DE SUBIDAS FALLIDAS
    // =====================================

    private void eliminarArchivoFallido(
            Path archivo) {

        if (archivo == null) {
            return;
        }

        try {

            Files.deleteIfExists(archivo);

        } catch (IOException ex) {

            // La limpieza no debe impedir
            // mostrar el mensaje de error.
        }
    }
}