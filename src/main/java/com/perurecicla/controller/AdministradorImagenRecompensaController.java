package com.perurecicla.controller;

import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.UsuarioRepository;
import com.perurecicla.service.RecompensaService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.Principal;
import java.util.UUID;

/**
 * PERU RECICLA - PASO 74
 *
 * Permite al administrador:
 *
 * 1. Seleccionar una fotografia.
 * 2. Guardarla fuera del proyecto.
 * 3. Asociarla a una recompensa.
 * 4. Reemplazar la imagen asignada.
 * 5. Mostrarla en el navegador.
 *
 * No modifica el sistema de canjes.
 */
@Controller
public class AdministradorImagenRecompensaController {

    private static final long LIMITE_BYTES =
            5L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;

    private final RecompensaService recompensaService;

    public AdministradorImagenRecompensaController(
            UsuarioRepository usuarioRepository,
            RecompensaService recompensaService) {

        this.usuarioRepository = usuarioRepository;
        this.recompensaService = recompensaService;
    }

    // ==========================================
    // CARPETA DE ALMACENAMIENTO
    // ==========================================

    private Path directorioImagenes() {

        return Path.of(
            System.getProperty("user.home"),
            "PeruRecicla",
            "imagenes",
            "recompensas"
        ).toAbsolutePath().normalize();
    }

    // ==========================================
    // VERIFICAR ADMINISTRADOR ACTIVO
    // ==========================================

    private void validarAdministrador(
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
    }

    // ==========================================
    // MOSTRAR FORMULARIO DE IMAGEN
    // ==========================================

    @GetMapping("/admin/recompensas/imagen/{id}")
    public String mostrarFormulario(
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

        model.addAttribute(
            "recompensa",
            recompensa
        );

        return "admin-recompensa-imagen";
    }

    // ==========================================
    // GUARDAR FOTOGRAFIA
    // ==========================================

    @PostMapping("/admin/recompensas/imagen/{id}")
    public String subirImagen(
            @PathVariable Integer id,

            @RequestParam("imagen")
            MultipartFile imagen,

            Principal principal,

            RedirectAttributes redirect) {

        validarAdministrador(principal);

        Recompensa recompensa =
                recompensaService.buscarPorId(id);

        if (recompensa == null) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Recompensa no encontrada"
            );
        }

        Path archivoCreado = null;

        try {

            // 1. Validar que exista la fotografia

            if (imagen == null || imagen.isEmpty()) {

                throw new IllegalArgumentException(
                    "Selecciona una fotografia."
                );
            }

            // 2. Limitar el tamaño

            if (imagen.getSize() > LIMITE_BYTES) {

                throw new IllegalArgumentException(
                    "La fotografia no debe superar 5 MB."
                );
            }

            // 3. Verificar el contenido real
            //    y determinar la extension.

            String extension =
                    detectarExtension(imagen);

            // 4. Crear directorio si no existe

            Path carpeta = directorioImagenes();

            Files.createDirectories(carpeta);

            // 5. Nombre aleatorio para evitar
            //    sobrescribir archivos existentes

            String nombreArchivo =
                    UUID.randomUUID().toString()
                    + extension;

            Path destino = carpeta
                    .resolve(nombreArchivo)
                    .normalize();

            // 6. Guardar fotografia

            try (InputStream entrada =
                    imagen.getInputStream()) {

                Files.copy(entrada, destino);
            }

            archivoCreado = destino;

            // 7. Registrar URL en la recompensa

            String url =
                    "/images/recompensas/"
                    + nombreArchivo;

            recompensa.setUrl_imagen(url);

            // Utiliza el servicio existente:
            // no es necesario modificar MySQL.

            recompensaService.guardar(recompensa);

            redirect.addFlashAttribute(
                "exito",
                "Fotografia guardada correctamente."
            );

            return "redirect:/admin/recompensas";

        } catch (IllegalArgumentException ex) {

            borrarArchivoFallido(archivoCreado);

            redirect.addFlashAttribute(
                "error",
                ex.getMessage()
            );

        } catch (IOException ex) {

            borrarArchivoFallido(archivoCreado);

            redirect.addFlashAttribute(
                "error",
                "No se pudo almacenar la fotografia."
            );

        } catch (RuntimeException ex) {

            borrarArchivoFallido(archivoCreado);

            redirect.addFlashAttribute(
                "error",
                "No se pudo actualizar la recompensa."
            );
        }

        return "redirect:/admin/recompensas/imagen/"
                + id;
    }

    // ==========================================
    // VALIDAR FORMATO REAL DEL ARCHIVO
    // ==========================================

    private String detectarExtension(
            MultipartFile imagen) throws IOException {

        byte[] firma;

        try (InputStream entrada =
                imagen.getInputStream()) {

            firma = entrada.readNBytes(12);
        }

        // JPEG

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

        // WEBP: cabecera RIFF ... WEBP

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
            "Formato no permitido. "
            + "Utiliza JPG, PNG o WebP."
        );
    }

    // ==========================================
    // LIMPIAR ARCHIVO SI FALLO EL GUARDADO
    // ==========================================

    private void borrarArchivoFallido(
            Path archivo) {

        if (archivo != null) {

            try {

                Files.deleteIfExists(archivo);

            } catch (IOException ex) {

                // No interrumpir la respuesta
                // por un error de limpieza.
            }
        }
    }

    // ==========================================
    // MOSTRAR FOTOGRAFIA EN EL NAVEGADOR
    //
    // Spring Security ya permite /images/**
    // en el SecurityConfig actual.
    // ==========================================

    @GetMapping("/images/recompensas/{nombre:.+}")
    public ResponseEntity<Resource> mostrarImagen(
            @PathVariable String nombre) {

        // Solo archivos con nombres UUID
        // generados por la aplicacion.

        if (!nombre.matches(
                "[0-9a-f-]{36}\\.(jpg|png|webp)")) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND
            );
        }

        Path archivo = directorioImagenes()
                .resolve(nombre)
                .normalize();

        if (!Files.isRegularFile(archivo)) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND
            );
        }

        MediaType tipo;

        if (nombre.endsWith(".png")) {

            tipo = MediaType.IMAGE_PNG;

        } else if (nombre.endsWith(".webp")) {

            tipo = MediaType.parseMediaType(
                "image/webp"
            );

        } else {

            tipo = MediaType.IMAGE_JPEG;
        }

        Resource recurso =
                new FileSystemResource(archivo);

        return ResponseEntity.ok()
            .contentType(tipo)
            .header(
                "X-Content-Type-Options",
                "nosniff"
            )
            .body(recurso);
    }
}