package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.repository.UsuarioRepository;

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
import java.util.Locale;

/**
 * PERU RECICLA - PASO 81
 *
 * Gestion administrativa de usuarios.
 *
 * Funcionalidades existentes:
 * - Listar usuarios
 * - Buscar por nombre, apellido o correo
 * - Filtrar por rol
 * - Filtrar por estado
 * - Mostrar indicadores
 *
 * Funcionalidades nuevas:
 * - Activar cuentas
 * - Desactivar cuentas
 * - Proteger cuentas administrativas
 * - Mantener filtros al guardar
 *
 * No modifica:
 * - DNI
 * - Nombre
 * - Correo
 * - Contrasena
 * - Rol
 * - Puntos acumulados
 * - Historial de operaciones
 */
@Controller
@RequestMapping("/admin/usuarios")
public class AdministradorUsuariosController {

    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm"
        );

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public AdministradorUsuariosController(
            UsuarioRepository usuarioRepository) {

        this.usuarioRepository = usuarioRepository;
    }

    // =====================================
    // LISTADO, INDICADORES Y FILTROS
    // =====================================

    @GetMapping
    @Transactional(readOnly = true)
    public String listarUsuarios(

            @RequestParam(
                name = "buscar",
                defaultValue = ""
            )
            String buscar,

            @RequestParam(
                name = "rol",
                defaultValue = "todos"
            )
            String rol,

            @RequestParam(
                name = "estado",
                defaultValue = "todos"
            )
            String estado,

            Principal principal,

            Model model) {

        // =================================
        // 1. VALIDAR ADMINISTRADOR
        // =================================

        Usuario administrador =
            validarAdministrador(principal);

        // =================================
        // 2. RECUPERAR USUARIOS
        // =================================

        List<Usuario> todos =
            usuarioRepository.findAll();

        // =================================
        // 3. INDICADORES GENERALES
        // =================================

        long totalUsuarios = todos.size();

        long totalVecinos = todos.stream()
            .filter(u ->
                u.getRol()
                    == Usuario.Rol.ROLE_vecino
            )
            .count();

        long totalRecolectores = todos.stream()
            .filter(u ->
                u.getRol()
                    == Usuario.Rol.ROLE_recolector
            )
            .count();

        long totalAdministradores = todos.stream()
            .filter(u ->
                u.getRol()
                    == Usuario.Rol.ROLE_administrador
            )
            .count();

        long totalActivos = todos.stream()
            .filter(u ->
                u.getEstado()
                    == Usuario.Estado.activo
            )
            .count();

        // =================================
        // 4. PREPARAR FILTROS
        // =================================

        String textoBuscar =
            buscar == null
                ? ""
                : buscar.strip();

        if (textoBuscar.length() > 100) {

            textoBuscar =
                textoBuscar.substring(0, 100);
        }

        final String busqueda =
            textoBuscar.toLowerCase(Locale.ROOT);

        final String filtroRol =
            rol == null
                ? "todos"
                : rol;

        final String filtroEstado =
            estado == null
                ? "todos"
                : estado;

        // =================================
        // 5. FILTRAR USUARIOS
        // =================================

        List<UsuarioFila> usuariosFiltrados =
            todos.stream()

                .filter(usuario -> {

                    String nombre =
                        valorSeguro(
                            usuario.getNombre()
                        );

                    String apellido =
                        valorSeguro(
                            usuario.getApellido()
                        );

                    String correo =
                        valorSeguro(
                            usuario.getCorreo()
                        );

                    String texto =
                        (
                            nombre + " "
                            + apellido + " "
                            + correo
                        ).toLowerCase(
                            Locale.ROOT
                        );

                    return busqueda.isEmpty()
                        || texto.contains(busqueda);
                })

                .filter(usuario -> {

                    if ("todos".equals(filtroRol)) {

                        return true;
                    }

                    return usuario.getRol() != null
                        && usuario.getRol()
                            .name()
                            .equals(filtroRol);
                })

                .filter(usuario -> {

                    if ("todos".equals(filtroEstado)) {

                        return true;
                    }

                    return usuario.getEstado() != null
                        && usuario.getEstado()
                            .name()
                            .equals(filtroEstado);
                })

                .sorted(
                    Comparator.comparing(
                        Usuario::getId_usuario,
                        Comparator.nullsLast(
                            Comparator.reverseOrder()
                        )
                    )
                )

                .map(UsuarioFila::new)

                .toList();

        // =================================
        // 6. DATOS DEL ADMINISTRADOR
        // =================================

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

        // =================================
        // 7. INDICADORES
        // =================================

        model.addAttribute(
            "totalUsuarios",
            totalUsuarios
        );

        model.addAttribute(
            "totalVecinos",
            totalVecinos
        );

        model.addAttribute(
            "totalRecolectores",
            totalRecolectores
        );

        model.addAttribute(
            "totalAdministradores",
            totalAdministradores
        );

        model.addAttribute(
            "totalActivos",
            totalActivos
        );

        // =================================
        // 8. RESULTADOS
        // =================================

        model.addAttribute(
            "usuarios",
            usuariosFiltrados
        );

        model.addAttribute(
            "cantidadResultados",
            usuariosFiltrados.size()
        );

        // =================================
        // 9. FILTROS SELECCIONADOS
        // =================================

        model.addAttribute(
            "buscar",
            textoBuscar
        );

        model.addAttribute(
            "rolSeleccionado",
            filtroRol
        );

        model.addAttribute(
            "estadoSeleccionado",
            filtroEstado
        );

        return "admin-usuarios";
    }

    // =====================================
    // NUEVO - CAMBIAR ESTADO DE USUARIO
    // =====================================

    @PostMapping("/cambiar-estado")
    @Transactional
    public String cambiarEstado(

            @RequestParam("idUsuario")
            Integer idUsuario,

            @RequestParam("nuevoEstado")
            String nuevoEstado,

            @RequestParam(
                name = "buscar",
                defaultValue = ""
            )
            String buscar,

            @RequestParam(
                name = "rol",
                defaultValue = "todos"
            )
            String rol,

            @RequestParam(
                name = "estado",
                defaultValue = "todos"
            )
            String estado,

            Principal principal,

            RedirectAttributes redirect) {

        // =================================
        // 1. VALIDAR ADMINISTRADOR ACTIVO
        // =================================

        validarAdministrador(principal);

        // =================================
        // 2. VALIDAR IDENTIFICADOR
        // =================================

        if (idUsuario == null
                || idUsuario <= 0) {

            redirect.addFlashAttribute(
                "mensajeError",
                "El usuario seleccionado no es valido."
            );

            return regresarListado(
                redirect,
                buscar,
                rol,
                estado
            );
        }

        // =================================
        // 3. BUSCAR USUARIO
        // =================================

        Usuario usuario =
            usuarioRepository
                .findById(idUsuario)
                .orElse(null);

        if (usuario == null) {

            redirect.addFlashAttribute(
                "mensajeError",
                "El usuario ya no existe."
            );

            return regresarListado(
                redirect,
                buscar,
                rol,
                estado
            );
        }

        // =================================
        // 4. PROTEGER ADMINISTRADORES
        //
        // En esta version no se permite
        // cambiar el estado de cuentas
        // administrativas.
        // =================================

        if (usuario.getRol()
                == Usuario.Rol.ROLE_administrador) {

            redirect.addFlashAttribute(
                "mensajeError",
                "Las cuentas administrativas "
                + "estan protegidas y no pueden "
                + "desactivarse desde este modulo."
            );

            return regresarListado(
                redirect,
                buscar,
                rol,
                estado
            );
        }

        // =================================
        // 5. VALIDAR ESTADO SOLICITADO
        // =================================

        String estadoLimpio =
            nuevoEstado == null
                ? ""
                : nuevoEstado.trim();

        if (!estadoLimpio.equals("activo")
                && !estadoLimpio.equals("inactivo")) {

            redirect.addFlashAttribute(
                "mensajeError",
                "El estado seleccionado no es valido."
            );

            return regresarListado(
                redirect,
                buscar,
                rol,
                estado
            );
        }

        Usuario.Estado estadoNuevo =
            Usuario.Estado.valueOf(
                estadoLimpio
            );

        // =================================
        // 6. EVITAR CAMBIOS INNECESARIOS
        // =================================

        if (usuario.getEstado() == estadoNuevo) {

            redirect.addFlashAttribute(
                "mensajeExito",
                "La cuenta ya se encuentra "
                + "en el estado seleccionado."
            );

            return regresarListado(
                redirect,
                buscar,
                rol,
                estado
            );
        }

        // =================================
        // 7. MODIFICAR SOLO EL ESTADO
        // =================================

        usuario.setEstado(
            estadoNuevo
        );

        usuarioRepository.save(
            usuario
        );

        // =================================
        // 8. MENSAJE DE CONFIRMACION
        // =================================

        String nombreCompleto =
            (
                valorSeguro(usuario.getNombre())
                + " "
                + valorSeguro(usuario.getApellido())
            ).strip();

        redirect.addFlashAttribute(
            "mensajeExito",
            "El estado de "
                + nombreCompleto
                + " se actualizo a "
                + estadoNuevo.name()
                + " correctamente."
        );

        // =================================
        // 9. REGRESAR AL LISTADO
        // =================================

        return regresarListado(
            redirect,
            buscar,
            rol,
            estado
        );
    }

    // =====================================
    // MANTENER FILTROS AL REGRESAR
    // =====================================

    private String regresarListado(
            RedirectAttributes redirect,
            String buscar,
            String rol,
            String estado) {

        if (buscar != null
                && !buscar.isBlank()) {

            String texto = buscar.strip();

            if (texto.length() > 100) {

                texto = texto.substring(0, 100);
            }

            redirect.addAttribute(
                "buscar",
                texto
            );
        }

        if ("ROLE_vecino".equals(rol)
                || "ROLE_recolector".equals(rol)
                || "ROLE_administrador".equals(rol)) {

            redirect.addAttribute(
                "rol",
                rol
            );
        }

        if ("activo".equals(estado)
                || "inactivo".equals(estado)) {

            redirect.addAttribute(
                "estado",
                estado
            );
        }

        return "redirect:/admin/usuarios";
    }

    // =====================================
    // VALIDAR ADMINISTRADOR ACTIVO
    // =====================================

    private Usuario validarAdministrador(
            Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario administrador =
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

        if (administrador.getRol()
                != Usuario.Rol.ROLE_administrador
                || administrador.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para "
                + "administradores activos"
            );
        }

        return administrador;
    }

    // =====================================
    // UTILIDAD PARA TEXTOS NULOS
    // =====================================

    private static String valorSeguro(
            String valor) {

        return valor == null ? "" : valor;
    }

    // =====================================
    // DATOS VISIBLES EN LA TABLA
    //
    // No incluye DNI ni contrasena.
    // =====================================

    public static class UsuarioFila {

        private final Integer id;

        private final String nombreCompleto;

        private final String correo;

        private final String rol;

        private final String estado;

        private final BigDecimal puntos;

        private final String fechaRegistro;

        private final boolean editable;

        public UsuarioFila(Usuario usuario) {

            this.id =
                usuario.getId_usuario();

            this.nombreCompleto =
                (
                    valorSeguro(usuario.getNombre())
                    + " "
                    + valorSeguro(usuario.getApellido())
                ).strip();

            this.correo =
                valorSeguro(usuario.getCorreo());

            if (usuario.getRol() == null) {

                this.rol = "Sin rol";

            } else {

                this.rol =
                    switch (usuario.getRol()) {

                        case ROLE_vecino ->
                            "Vecino";

                        case ROLE_recolector ->
                            "Recolector";

                        case ROLE_administrador ->
                            "Administrador";
                    };
            }

            this.estado =
                usuario.getEstado() == null
                    ? "Sin estado"
                    : usuario.getEstado().name();

            this.puntos =
                usuario.getPuntos_acumulados() == null
                    ? BigDecimal.ZERO
                    : usuario.getPuntos_acumulados();

            this.fechaRegistro =
                usuario.getFecha_registro() == null
                    ? "Sin fecha"
                    : usuario.getFecha_registro()
                        .format(FORMATO_FECHA);

            this.editable =
                usuario.getRol()
                    != Usuario.Rol.ROLE_administrador;
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

        public String getRol() {
            return rol;
        }

        public String getEstado() {
            return estado;
        }

        public BigDecimal getPuntos() {
            return puntos;
        }

        public String getFechaRegistro() {
            return fechaRegistro;
        }

        public boolean isEditable() {
            return editable;
        }
    }
}