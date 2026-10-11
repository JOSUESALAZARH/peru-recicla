package com.perurecicla.controller;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.CanjeRepository;

import com.perurecicla.service.UsuarioService;
import com.perurecicla.service.EntregaCanjeService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * PERU RECICLA - PASO 78
 *
 * Gestion administrativa de canjes.
 *
 * CONSERVA:
 * - Buscar comprobantes por codigo.
 * - Mostrar detalle de un ticket.
 * - Confirmar entrega fisica.
 * - Verificar administrador activo.
 * - Mensajes de exito y error.
 *
 * NUEVO:
 * - Listar canjes pendientes.
 * - Mostrar datos del vecino.
 * - Mostrar recompensa y puntos.
 * - Ordenar del mas reciente al antiguo.
 * - Paginar de 10 en 10.
 * - Confirmar una entrega desde la lista.
 *
 * No descuenta puntos ni stock.
 */
@Controller
public class AdministradorCanjesController {

    private static final int TAMANO_PAGINA = 10;

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm"
        );

    private final UsuarioService usuarioService;

    private final CanjeRepository canjeRepository;

    private final EntregaCanjeService entregaCanjeService;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public AdministradorCanjesController(
            UsuarioService usuarioService,
            CanjeRepository canjeRepository,
            EntregaCanjeService entregaCanjeService) {

        this.usuarioService =
            usuarioService;

        this.canjeRepository =
            canjeRepository;

        this.entregaCanjeService =
            entregaCanjeService;
    }

    // =====================================
    // MOSTRAR BUSCADOR Y LISTADO
    // =====================================

    @GetMapping("/admin/canjes")
    @Transactional(readOnly = true)
    public String mostrarCanjes(

            @RequestParam(
                name = "codigo",
                required = false
            )
            String codigo,

            @RequestParam(
                name = "pagina",
                defaultValue = "0"
            )
            int pagina,

            Principal principal,

            Model model) {

        // =================================
        // 1. VALIDAR ADMINISTRADOR
        // =================================

        Usuario administrador =
                validarAdministrador(principal);

        model.addAttribute(
            "administrador",
            administrador.getNombre()
        );

        // =================================
        // 2. MANTENER BUSQUEDA POR TICKET
        // =================================

        String codigoLimpio =
                codigo == null
                ? ""
                : codigo.trim();

        model.addAttribute(
            "codigo",
            codigoLimpio
        );

        if (!codigoLimpio.isEmpty()) {

            if (codigoLimpio.length() > 20) {

                model.addAttribute(
                    "error",
                    "El codigo del ticket no es valido."
                );

            } else {

                Optional<Canje> resultado =
                    canjeRepository
                        .buscarPorCodigoTicket(
                            codigoLimpio
                        );

                if (resultado.isPresent()) {

                    model.addAttribute(
                        "canje",
                        resultado.get()
                    );

                } else {

                    model.addAttribute(
                        "error",
                        "No se encontro el ticket."
                    );
                }
            }
        }

        // =================================
        // 3. CONSULTAR CANJES PENDIENTES
        // =================================

        int paginaSolicitada =
                Math.max(0, pagina);

        Page<Canje> paginaCanjes =
            canjeRepository
                .listarPorEstadoOrdenado(
                    Canje.EstadoCanje.pendiente,
                    PageRequest.of(
                        paginaSolicitada,
                        TAMANO_PAGINA
                    )
                );

        // =================================
        // 4. AJUSTAR PAGINA SI YA NO EXISTE
        //
        // Ejemplo:
        // Se entrega el ultimo ticket
        // de la ultima pagina.
        // =================================

        int totalPaginas =
                paginaCanjes.getTotalPages();

        if (totalPaginas > 0
                && paginaSolicitada >= totalPaginas) {

            paginaSolicitada =
                    totalPaginas - 1;

            paginaCanjes =
                canjeRepository
                    .listarPorEstadoOrdenado(
                        Canje.EstadoCanje.pendiente,
                        PageRequest.of(
                            paginaSolicitada,
                            TAMANO_PAGINA
                        )
                    );
        }

        if (totalPaginas == 0) {

            paginaSolicitada = 0;
        }

        // =================================
        // 5. PREPARAR FILAS DE LA TABLA
        // =================================

        List<FilaCanje> filas =
            paginaCanjes
                .getContent()
                .stream()
                .map(FilaCanje::new)
                .toList();

        // =================================
        // 6. ENVIAR DATOS A THYMELEAF
        // =================================

        model.addAttribute(
            "canjesPendientes",
            filas
        );

        model.addAttribute(
            "totalPendientes",
            paginaCanjes.getTotalElements()
        );

        model.addAttribute(
            "paginaActual",
            paginaSolicitada
        );

        model.addAttribute(
            "totalPaginas",
            paginaCanjes.getTotalPages()
        );

        model.addAttribute(
            "tieneAnterior",
            paginaSolicitada > 0
        );

        model.addAttribute(
            "tieneSiguiente",
            paginaSolicitada + 1
                < paginaCanjes.getTotalPages()
        );

        model.addAttribute(
            "numeroPaginaVisible",
            paginaCanjes.getTotalPages() == 0
                ? 0
                : paginaSolicitada + 1
        );

        return "admin-canjes";
    }

    // =====================================
    // CONFIRMAR ENTREGA FISICA
    // =====================================

    @PostMapping("/admin/canjes/entregar")
    public String entregarRecompensa(

            @RequestParam("codigo")
            String codigo,

            @RequestParam(
                name = "pagina",
                defaultValue = "0"
            )
            int pagina,

            Principal principal,

            RedirectAttributes redirect) {

        // =================================
        // 1. SEGUNDA VALIDACION DE ACCESO
        // =================================

        validarAdministrador(principal);

        String codigoLimpio =
                codigo == null
                ? ""
                : codigo.trim();

        // =================================
        // 2. CONFIRMAR ENTREGA
        //
        // El servicio existente verifica:
        // - Que el ticket exista.
        // - Que siga pendiente.
        // - Que solo cambie de estado una vez.
        //
        // No se descuentan puntos ni stock.
        // =================================

        try {

            entregaCanjeService.confirmarEntrega(
                codigoLimpio
            );

            redirect.addFlashAttribute(
                "exito",
                "Entrega confirmada correctamente. "
                + "El ticket ya figura como entregado."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirect.addFlashAttribute(
                "error",
                e.getMessage()
            );
        }

        // =================================
        // 3. VOLVER AL LISTADO
        //
        // Se conserva el codigo para mostrar
        // el detalle y la pagina que se
        // estaba consultando.
        // =================================

        if (!codigoLimpio.isEmpty()
                && codigoLimpio.length() <= 20) {

            redirect.addAttribute(
                "codigo",
                codigoLimpio
            );
        }

        redirect.addAttribute(
            "pagina",
            Math.max(0, pagina)
        );

        return "redirect:/admin/canjes";
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

        Usuario usuario =
            usuarioService.buscarPorCorreo(
                principal.getName()
            );

        if (usuario == null
                || usuario.getRol()
                    != Usuario.Rol.ROLE_administrador
                || usuario.getEstado()
                    != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para "
                + "administradores activos"
            );
        }

        return usuario;
    }

    // =====================================
    // NUEVO - FILA DEL LISTADO
    //
    // Evita mostrar entidades completas
    // directamente en la tabla.
    // =====================================

    public static class FilaCanje {

        private final String codigo;

        private final String vecino;

        private final String recompensa;

        private final Integer puntos;

        private final String fecha;

        private final String estado;

        // =================================
        // CONSTRUCTOR
        // =================================

        public FilaCanje(Canje canje) {

            this.codigo =
                canje.getCodigo_ticket() == null
                    ? "Sin codigo"
                    : canje.getCodigo_ticket();

            if (canje.getUsuario() != null) {

                String nombre =
                    canje.getUsuario().getNombre() == null
                        ? ""
                        : canje.getUsuario().getNombre();

                String apellido =
                    canje.getUsuario().getApellido() == null
                        ? ""
                        : canje.getUsuario().getApellido();

                this.vecino =
                    (nombre + " " + apellido).trim();

            } else {

                this.vecino = "Sin vecino";
            }

            this.recompensa =
                canje.getRecompensa() == null
                    ? "Sin recompensa"
                    : canje.getRecompensa().getNombre();

            this.puntos =
                canje.getPuntos_descontados() == null
                    ? 0
                    : canje.getPuntos_descontados();

            this.fecha =
                canje.getFecha_canje() == null
                    ? "Sin fecha"
                    : canje.getFecha_canje()
                        .format(FORMATO_FECHA);

            this.estado =
                canje.getEstado() == null
                    ? "Sin estado"
                    : canje.getEstado().name();
        }

        // =================================
        // GETTERS PARA THYMELEAF
        // =================================

        public String getCodigo() {
            return codigo;
        }

        public String getVecino() {
            return vecino;
        }

        public String getRecompensa() {
            return recompensa;
        }

        public Integer getPuntos() {
            return puntos;
        }

        public String getFecha() {
            return fecha;
        }

        public String getEstado() {
            return estado;
        }
    }
}