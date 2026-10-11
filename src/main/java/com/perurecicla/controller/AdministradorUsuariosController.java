
package com.perurecicla.controller;

import com.perurecicla.model.Usuario;
import com.perurecicla.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * PERU RECICLA - PASO 56
 *
 * Gestion administrativa de usuarios.
 *
 * Funcionalidades:
 * - Listado de usuarios
 * - Busqueda por nombre y correo
 * - Filtro por rol
 * - Filtro por estado
 * - Indicadores generales
 *
 * Seguridad:
 * - Solo administradores activos
 * - No muestra DNI ni contrasenas
 * - No modifica registros de MySQL
 */
@Controller
@RequestMapping("/admin/usuarios")
public class AdministradorUsuariosController {

    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AdministradorUsuariosController(
            UsuarioRepository usuarioRepository) {

        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listarUsuarios(

            @RequestParam(
                name = "buscar",
                defaultValue = ""
            ) String buscar,

            @RequestParam(
                name = "rol",
                defaultValue = "todos"
            ) String rol,

            @RequestParam(
                name = "estado",
                defaultValue = "todos"
            ) String estado,

            Principal principal,
            Model model) {

        // =====================================
        // 1. VALIDAR AL ADMINISTRADOR
        // =====================================

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario administrador =
            usuarioRepository
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
                "Acceso exclusivo para administradores activos"
            );
        }

        // =====================================
        // 2. RECUPERAR LOS USUARIOS
        // =====================================

        List<Usuario> todos =
            usuarioRepository.findAll();

        // =====================================
        // 3. CALCULAR INDICADORES
        // =====================================

        long totalUsuarios = todos.size();

        long totalVecinos = todos.stream()
            .filter(u ->
                u.getRol() == Usuario.Rol.ROLE_vecino
            )
            .count();

        long totalRecolectores = todos.stream()
            .filter(u ->
                u.getRol() == Usuario.Rol.ROLE_recolector
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
                u.getEstado() == Usuario.Estado.activo
            )
            .count();

        // =====================================
        // 4. PREPARAR FILTROS
        // =====================================

        String textoBuscar = buscar == null
            ? ""
            : buscar.strip();

        if (textoBuscar.length() > 100) {

            textoBuscar =
                textoBuscar.substring(0, 100);
        }

        final String busqueda =
            textoBuscar.toLowerCase(Locale.ROOT);

        final String filtroRol =
            rol == null ? "todos" : rol;

        final String filtroEstado =
            estado == null ? "todos" : estado;

        // =====================================
        // 5. APLICAR FILTROS
        // =====================================

        List<UsuarioFila> usuariosFiltrados =
            todos.stream()

                .filter(usuario -> {

                    String nombre =
                        valorSeguro(usuario.getNombre());

                    String apellido =
                        valorSeguro(usuario.getApellido());

                    String correo =
                        valorSeguro(usuario.getCorreo());

                    String texto =
                        (nombre + " " + apellido + " " + correo)
                            .toLowerCase(Locale.ROOT);

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

        // =====================================
        // 6. ENVIAR DATOS A LA VISTA
        // =====================================

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

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

        model.addAttribute(
            "usuarios",
            usuariosFiltrados
        );

        model.addAttribute(
            "cantidadResultados",
            usuariosFiltrados.size()
        );

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

    private static String valorSeguro(String valor) {

        return valor == null ? "" : valor;
    }

    // =====================================
    // DATOS VISIBLES EN EL LISTADO
    //
    // NO INCLUYE CONTRASENA NI DNI
    // =====================================

    public static class UsuarioFila {

        private final Integer id;
        private final String nombreCompleto;
        private final String correo;
        private final String rol;
        private final String estado;
        private final BigDecimal puntos;
        private final String fechaRegistro;

        public UsuarioFila(Usuario usuario) {

            this.id = usuario.getId_usuario();

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

                this.rol = switch (usuario.getRol()) {

                    case ROLE_vecino -> "Vecino";

                    case ROLE_recolector -> "Recolector";

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
    }
}
