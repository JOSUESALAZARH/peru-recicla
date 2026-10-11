
package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.repository.UsuarioRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * PERU RECICLA - PASO 58
 *
 * Registro administrativo de recolectores.
 *
 * Funcionalidades:
 * - Registrar una cuenta de recolector
 * - Listar recolectores
 * - Verificar DNI y correo duplicados
 * - Guardar contrasenas cifradas con BCrypt
 *
 * Solo administradores activos.
 */
@Controller
@RequestMapping("/admin/recolectores")
public class AdministradorRecolectoresController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdministradorRecolectoresController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // VALIDACION DEL ADMINISTRADOR
    // ==========================================

    private Usuario obtenerAdministrador(
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

        return administrador;
    }

    // ==========================================
    // MOSTRAR FORMULARIO Y LISTADO
    // ==========================================

    @GetMapping
    public String mostrarRecolectores(
            Principal principal,
            Model model) {

        Usuario administrador =
            obtenerAdministrador(principal);

        List<RecolectorFila> recolectores =
            usuarioRepository.findAll()
                .stream()
                .filter(usuario ->
                    usuario.getRol()
                        == Usuario.Rol.ROLE_recolector
                )
                .sorted(
                    Comparator.comparing(
                        Usuario::getId_usuario,
                        Comparator.nullsLast(
                            Comparator.reverseOrder()
                        )
                    )
                )
                .map(RecolectorFila::new)
                .toList();

        long activos = recolectores.stream()
            .filter(recolector ->
                "activo".equals(recolector.getEstado())
            )
            .count();

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

        model.addAttribute(
            "recolectores",
            recolectores
        );

        model.addAttribute(
            "totalRecolectores",
            recolectores.size()
        );

        model.addAttribute(
            "recolectoresActivos",
            activos
        );

        return "admin-recolectores";
    }

    // ==========================================
    // REGISTRAR NUEVO RECOLECTOR
    // ==========================================

    @PostMapping
    public String registrarRecolector(

            @RequestParam("nombre")
            String nombre,

            @RequestParam("apellido")
            String apellido,

            @RequestParam("dni")
            String dni,

            @RequestParam("correo")
            String correo,

            @RequestParam(
                name = "telefono",
                required = false,
                defaultValue = ""
            )
            String telefono,

            @RequestParam("contrasena")
            String contrasena,

            @RequestParam("confirmacion")
            String confirmacion,

            Principal principal,

            RedirectAttributes redirectAttributes) {

        // Solo administradores activos.
        obtenerAdministrador(principal);

        // ======================================
        // 1. NORMALIZAR DATOS
        // ======================================

        nombre = nombre == null
            ? "" : nombre.strip();

        apellido = apellido == null
            ? "" : apellido.strip();

        dni = dni == null
            ? "" : dni.strip();

        correo = correo == null
            ? "" : correo.strip()
                .toLowerCase(Locale.ROOT);

        telefono = telefono == null
            ? "" : telefono.strip();

        // ======================================
        // 2. VALIDAR CAMPOS
        // ======================================

        if (nombre.isBlank()
                || apellido.isBlank()
                || nombre.length() > 100
                || apellido.length() > 100) {

            return error(
                redirectAttributes,
                "Ingresa un nombre y apellido validos."
            );
        }

        if (!dni.matches("[0-9]{8}")) {

            return error(
                redirectAttributes,
                "El DNI debe contener exactamente 8 digitos."
            );
        }

        if (correo.length() > 150
                || !correo.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                )) {

            return error(
                redirectAttributes,
                "Ingresa un correo electronico valido."
            );
        }

        if (telefono.length() > 20) {

            return error(
                redirectAttributes,
                "El telefono no debe superar 20 caracteres."
            );
        }

        if (contrasena == null
                || contrasena.length() < 10
                || contrasena.getBytes(
                    StandardCharsets.UTF_8
                ).length > 72) {

            return error(
                redirectAttributes,
                "La contrasena debe tener al menos "
                + "10 caracteres y no superar "
                + "72 bytes."
            );
        }

        if (!contrasena.equals(confirmacion)) {

            return error(
                redirectAttributes,
                "Las contrasenas no coinciden."
            );
        }

        // ======================================
        // 3. VERIFICAR DUPLICADOS
        // ======================================

        if (usuarioRepository.findByCorreo(correo)
                .isPresent()) {

            return error(
                redirectAttributes,
                "El correo ya esta registrado."
            );
        }

        final String dniValidado = dni;

        boolean dniExiste =
            usuarioRepository.findAll()
                .stream()
                .anyMatch(usuario ->
                    dniValidado.equals(
                        usuario.getDni()
                    )
                );

        if (dniExiste) {

            return error(
                redirectAttributes,
                "El DNI ya esta registrado."
            );
        }

        // ======================================
        // 4. CREAR USUARIO RECOLECTOR
        // ======================================

        Usuario recolector = new Usuario();

        recolector.setNombre(nombre);
        recolector.setApellido(apellido);
        recolector.setDni(dni);
        recolector.setCorreo(correo);

        recolector.setTelefono(
            telefono.isBlank() ? null : telefono
        );

        // Nunca guardar contrasenas en texto plano.
        recolector.setContrasena(
            passwordEncoder.encode(contrasena)
        );

        // El rol se asigna en el servidor.
        recolector.setRol(
            Usuario.Rol.ROLE_recolector
        );

        recolector.setEstado(
            Usuario.Estado.activo
        );

        recolector.setPuntos_acumulados(
            BigDecimal.ZERO
        );

        // ======================================
        // 5. GUARDAR EN MYSQL
        // ======================================

        try {

            usuarioRepository.saveAndFlush(
                recolector
            );

        } catch (DataIntegrityViolationException e) {

            return error(
                redirectAttributes,
                "No se pudo registrar: revisa si "
                + "el correo o DNI ya existen."
            );
        }

        redirectAttributes.addFlashAttribute(
            "exito",
            "Recolector registrado correctamente."
        );

        return "redirect:/admin/recolectores";
    }

    private String error(
            RedirectAttributes redirectAttributes,
            String mensaje) {

        redirectAttributes.addFlashAttribute(
            "error",
            mensaje
        );

        return "redirect:/admin/recolectores";
    }

    // ==========================================
    // DATOS VISIBLES EN LA TABLA
    // ==========================================

    public static class RecolectorFila {

        private final Integer id;
        private final String nombreCompleto;
        private final String correo;
        private final String telefono;
        private final String estado;

        public RecolectorFila(Usuario usuario) {

            this.id = usuario.getId_usuario();

            this.nombreCompleto =
                usuario.getNombre() + " "
                + usuario.getApellido();

            this.correo =
                usuario.getCorreo();

            this.telefono =
                usuario.getTelefono() == null
                    ? "No registrado"
                    : usuario.getTelefono();

            this.estado =
                usuario.getEstado() == null
                    ? "Sin estado"
                    : usuario.getEstado().name();
        }

        public Integer getId() {
            return id;
        }

        public String getNombreCompleto() {
            return nombreCompleto;
        }

        public String getCorreo() {
            return correo;
        }

        public String getTelefono() {
            return telefono;
        }

        public String getEstado() {
            return estado;
        }
    }
}
