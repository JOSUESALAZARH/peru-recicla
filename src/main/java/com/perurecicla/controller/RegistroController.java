
package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.service.UsuarioService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

/**
 * PERU RECICLA
 *
 * Controlador de Registro de Usuarios.
 *
 * Permite registrar vecinos mediante
 * un formulario web con Thymeleaf.
 *
 * Arquitectura:
 * Controller -> Service -> Repository -> MySQL
 */

@Controller
public class RegistroController {

    // =====================================
    // SERVICIO DE USUARIOS
    // =====================================

    private final UsuarioService usuarioService;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public RegistroController(
            UsuarioService usuarioService) {

        this.usuarioService = usuarioService;
    }

    // =====================================
    // MOSTRAR FORMULARIO DE REGISTRO
    // =====================================

    @GetMapping("/registro")
    public String mostrarRegistro(
            @RequestParam(
                name = "exito",
                required = false
            ) String exito,
            Model model) {

        model.addAttribute(
            "titulo",
            "Registro de Vecinos - PERU RECICLA"
        );

        if (exito != null) {
            model.addAttribute(
                "mensajeExito",
                "Tu cuenta ha sido registrada correctamente."
            );
        }

        return "registro";
    }

    // =====================================
    // PROCESAR REGISTRO DEL VECINO
    // =====================================

    @PostMapping("/registro")
    public String registrarVecino(

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
                required = false
            )
            String telefono,

            @RequestParam("contrasena")
            String contrasena,

            Model model) {

        // Conservar los datos no sensibles
        // si el formulario presenta errores.

        model.addAttribute(
            "nombreIngresado", nombre
        );

        model.addAttribute(
            "apellidoIngresado", apellido
        );

        model.addAttribute(
            "dniIngresado", dni
        );

        model.addAttribute(
            "correoIngresado", correo
        );

        model.addAttribute(
            "telefonoIngresado", telefono
        );

        // La contrasena nunca se devuelve
        // al formulario HTML.

        try {

            // =================================
            // VALIDAR NOMBRE Y APELLIDO
            // =================================

            if (nombre == null ||
                nombre.isBlank() ||
                nombre.trim().length() > 100) {

                throw new IllegalArgumentException(
                    "Ingrese un nombre valido (maximo 100 caracteres)."
                );
            }

            if (apellido == null ||
                apellido.isBlank() ||
                apellido.trim().length() > 100) {

                throw new IllegalArgumentException(
                    "Ingrese un apellido valido (maximo 100 caracteres)."
                );
            }

            // =================================
            // VALIDAR DNI
            // =================================

            if (dni == null ||
                !dni.matches("\\d{8}")) {

                throw new IllegalArgumentException(
                    "El DNI debe tener exactamente 8 digitos."
                );
            }

            // =================================
            // VALIDAR CORREO
            // =================================

            if (correo == null ||
                correo.isBlank() ||
                correo.length() > 150 ||
                !correo.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                )) {

                throw new IllegalArgumentException(
                    "Ingrese un correo electronico valido."
                );
            }

            // =================================
            // VALIDAR CONTRASENA
            // =================================

            if (contrasena == null ||
                contrasena.length() < 8) {

                throw new IllegalArgumentException(
                    "La contrasena debe tener al menos 8 caracteres."
                );
            }

            // =================================
            // VALIDAR TELEFONO OPCIONAL
            // =================================

            if (telefono != null &&
                !telefono.isBlank() &&
                !telefono.matches("\\+?[0-9]{7,15}")) {

                throw new IllegalArgumentException(
                    "Ingrese un telefono valido."
                );
            }

            // =================================
            // CREAR OBJETO USUARIO
            // =================================

            Usuario usuario = new Usuario();

            usuario.setNombre(
                nombre.trim()
            );

            usuario.setApellido(
                apellido.trim()
            );

            usuario.setDni(
                dni.trim()
            );

            usuario.setCorreo(
                correo.trim().toLowerCase(Locale.ROOT)
            );

            usuario.setTelefono(
                telefono == null || telefono.isBlank()
                    ? null
                    : telefono.trim()
            );

            usuario.setContrasena(
                contrasena
            );

            // =================================
            // REGISTRAR USUARIO EN MYSQL
            // =================================

            // El servicio asignara:
            // ROLE_vecino
            // Estado activo
            // Puntos iniciales en cero
            // Contrasena cifrada con BCrypt

            usuarioService.registrarUsuario(
                usuario
            );

            // =================================
            // REGISTRO EXITOSO
            // =================================

            return "redirect:/registro?exito";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                "error",
                e.getMessage()
            );

            return "registro";

        } catch (DataIntegrityViolationException e) {

            model.addAttribute(
                "error",
                "El DNI o correo ya se encuentra registrado."
            );

            return "registro";
        }
    }
}
