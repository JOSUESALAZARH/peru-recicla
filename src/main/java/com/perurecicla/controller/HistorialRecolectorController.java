
package com.perurecicla.controller;

import com.perurecicla.model.Material;
import com.perurecicla.model.Recolecta;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.MaterialRepository;
import com.perurecicla.repository.RecolectaRepository;
import com.perurecicla.repository.UsuarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * PERU RECICLA - PASO 66
 *
 * Historial completo del recolector.
 *
 * Funciones:
 * 1. Verificar recolector autenticado.
 * 2. Filtrar por estado.
 * 3. Filtrar por material.
 * 4. Filtrar por fechas.
 * 5. Buscar por ID de recolecta.
 * 6. Mostrar 10 o 20 registros por pagina.
 * 7. Navegar entre paginas.
 * 8. Calcular resumen estadistico en MySQL.
 *
 * No modifica recolectas ni puntos.
 */
@Controller
@RequestMapping("/recolector")
public class HistorialRecolectorController {

    // =====================================
    // REPOSITORIOS
    // =====================================

    private final UsuarioRepository usuarioRepository;

    private final RecolectaRepository recolectaRepository;

    private final MaterialRepository materialRepository;

    // =====================================
    // CONSTANTES
    // =====================================

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int TAMANO_PREDETERMINADO = 10;

    private static final LocalDateTime FECHA_MINIMA =
        LocalDate.of(1900, 1, 1).atStartOfDay();

    private static final LocalDateTime FECHA_MAXIMA =
        LocalDate.of(9999, 12, 31).atStartOfDay();

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public HistorialRecolectorController(
            UsuarioRepository usuarioRepository,
            RecolectaRepository recolectaRepository,
            MaterialRepository materialRepository) {

        this.usuarioRepository = usuarioRepository;
        this.recolectaRepository = recolectaRepository;
        this.materialRepository = materialRepository;
    }

    // =====================================
    // VERIFICAR RECOLECTOR AUTENTICADO
    // LOGICA CONSERVADA
    // =====================================

    private Usuario obtenerRecolector(
            Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario usuario = usuarioRepository
            .findByCorreo(principal.getName())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Recolector no encontrado"
                )
            );

        if (usuario.getRol()
                != Usuario.Rol.ROLE_recolector
                || usuario.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para recolectores activos"
            );
        }

        return usuario;
    }

    // =====================================
    // MOSTRAR HISTORIAL COMPLETO
    // =====================================

    @GetMapping("/historial-completo")
    @Transactional(readOnly = true)
    public String mostrarHistorial(

            @RequestParam(
                name = "estado",
                defaultValue = "todos"
            )
            String estado,

            @RequestParam(
                name = "materialId",
                required = false
            )
            Integer materialId,

            @RequestParam(
                name = "desde",
                required = false
            )
            String desde,

            @RequestParam(
                name = "hasta",
                required = false
            )
            String hasta,

            // NUEVO: BUSQUEDA POR ID
            @RequestParam(
                name = "id",
                required = false
            )
            String id,

            // NUEVO: NUMERO DE PAGINA
            @RequestParam(
                name = "page",
                defaultValue = "0"
            )
            int page,

            // NUEVO: REGISTROS POR PAGINA
            @RequestParam(
                name = "size",
                defaultValue = "10"
            )
            int size,

            Principal principal,
            Model model) {

        // =================================
        // 1. IDENTIFICAR RECOLECTOR
        // =================================

        Usuario recolector =
            obtenerRecolector(principal);

        Integer idRecolector =
            recolector.getId_usuario();

        // =================================
        // 2. VALIDAR FILTRO DE ESTADO
        // =================================

        Set<String> estadosPermitidos = Set.of(
            "todos",
            "pendiente",
            "validado",
            "rechazado"
        );

        String filtroEstado =
            estadosPermitidos.contains(estado)
                ? estado
                : "todos";

        Recolecta.EstadoValidacion estadoConsulta =
            null;

        if (!filtroEstado.equals("todos")) {

            estadoConsulta =
                Recolecta.EstadoValidacion.valueOf(
                    filtroEstado
                );
        }

        // =================================
        // 3. VALIDAR BUSQUEDA POR ID
        // =================================

        String idBuscado =
            id == null ? "" : id.trim();

        Integer idBusqueda = null;

        String advertencia = null;

        if (!idBuscado.isEmpty()) {

            try {

                idBusqueda =
                    Integer.valueOf(idBuscado);

                if (idBusqueda <= 0) {

                    throw new NumberFormatException(
                        "ID no positivo"
                    );
                }

            } catch (NumberFormatException ex) {

                advertencia =
                    "El numero de recolecta debe "
                    + "ser un entero positivo.";

                idBusqueda = null;
                idBuscado = "";
            }
        }

        // =================================
        // 4. VALIDAR MATERIAL
        // =================================

        if (materialId != null && materialId <= 0) {

            materialId = null;

            advertencia = agregarAdvertencia(
                advertencia,
                "El material seleccionado no es valido."
            );
        }

        // =================================
        // 5. VALIDAR FECHAS
        // =================================

        boolean usarDesde = false;
        boolean usarHasta = false;

        LocalDateTime desdeConsulta =
            FECHA_MINIMA;

        LocalDateTime hastaConsulta =
            FECHA_MAXIMA;

        String desdeTexto =
            desde == null ? "" : desde.trim();

        String hastaTexto =
            hasta == null ? "" : hasta.trim();

        try {

            LocalDate fechaInicio = null;
            LocalDate fechaFin = null;

            if (!desdeTexto.isBlank()) {

                fechaInicio =
                    LocalDate.parse(desdeTexto);
            }

            if (!hastaTexto.isBlank()) {

                fechaFin =
                    LocalDate.parse(hastaTexto);
            }

            if (fechaInicio != null
                    && fechaFin != null
                    && fechaInicio.isAfter(fechaFin)) {

                throw new IllegalArgumentException(
                    "La fecha inicial no puede ser "
                    + "posterior a la fecha final."
                );
            }

            if (fechaInicio != null) {

                usarDesde = true;

                desdeConsulta =
                    fechaInicio.atStartOfDay();
            }

            if (fechaFin != null) {

                // Limite superior exclusivo:
                // incluye todo el ultimo dia.

                usarHasta = true;

                hastaConsulta =
                    fechaFin.plusDays(1)
                        .atStartOfDay();
            }

        } catch (IllegalArgumentException
                 | DateTimeException ex) {

            advertencia = agregarAdvertencia(
                advertencia,
                "El rango de fechas no es valido. "
                + "Se mostraran resultados sin "
                + "filtros de fechas."
            );

            usarDesde = false;
            usarHasta = false;

            desdeConsulta = FECHA_MINIMA;
            hastaConsulta = FECHA_MAXIMA;

            desdeTexto = "";
            hastaTexto = "";
        }

        // =================================
        // 6. VALIDAR PAGINACION
        // =================================

        int tamanoPagina =
            size == 20
                ? 20
                : TAMANO_PREDETERMINADO;

        int paginaSolicitada =
            page >= 0 && page <= 100000
                ? page
                : 0;

        // =================================
        // 7. CONSULTAR MYSQL
        // =================================

        Page<Recolecta> resultado =
            recolectaRepository.buscarHistorialPaginado(

                idRecolector,

                estadoConsulta,

                materialId,

                idBusqueda,

                usarDesde,

                desdeConsulta,

                usarHasta,

                hastaConsulta,

                PageRequest.of(
                    paginaSolicitada,
                    tamanoPagina
                )
            );

        // =================================
        // 8. CORREGIR PAGINA FUERA DE RANGO
        // =================================

        if (paginaSolicitada > 0
                && (
                    resultado.getTotalPages() == 0
                    || paginaSolicitada
                        >= resultado.getTotalPages()
                )) {

            paginaSolicitada = Math.max(
                0,
                resultado.getTotalPages() - 1
            );

            resultado =
                recolectaRepository.buscarHistorialPaginado(

                    idRecolector,

                    estadoConsulta,

                    materialId,

                    idBusqueda,

                    usarDesde,

                    desdeConsulta,

                    usarHasta,

                    hastaConsulta,

                    PageRequest.of(
                        paginaSolicitada,
                        tamanoPagina
                    )
                );
        }

        // =================================
        // 9. TOTAL HISTORICO
        // SIN FILTROS
        // =================================

        long totalHistorico =
            recolectaRepository
                .contarHistorialPorRecolector(
                    idRecolector
                );

        // =================================
        // 10. RESUMEN ESTADISTICO
        // CON TODOS LOS FILTROS
        // =================================

        List<Object[]> resumen =
            recolectaRepository.resumirHistorialRecolector(

                idRecolector,

                estadoConsulta,

                materialId,

                idBusqueda,

                usarDesde,

                desdeConsulta,

                usarHasta,

                hastaConsulta,

                Recolecta.EstadoValidacion.pendiente,

                Recolecta.EstadoValidacion.validado,

                Recolecta.EstadoValidacion.rechazado
            );

        Object[] datosResumen =
            resumen.isEmpty()
                ? new Object[5]
                : resumen.get(0);

        long pendientes =
            valorLong(datosResumen[0]);

        long validadas =
            valorLong(datosResumen[1]);

        long rechazadas =
            valorLong(datosResumen[2]);

        BigDecimal pesoTotal =
            valorDecimal(datosResumen[3]);

        BigDecimal puntosAcreditados =
            valorDecimal(datosResumen[4]);

        // =================================
        // 11. CONVERTIR PAGINA A FILAS
        // =================================

        List<FilaHistorial> filas =
            resultado.getContent()
                .stream()
                .map(FilaHistorial::new)
                .toList();

        // =================================
        // 12. MATERIALES PARA FILTRO
        // =================================

        List<Material> materiales =
            materialRepository.findAll()
                .stream()
                .sorted(
                    Comparator.comparing(
                        Material::getTipo_material,
                        Comparator.nullsLast(
                            String.CASE_INSENSITIVE_ORDER
                        )
                    )
                )
                .toList();

        // =================================
        // 13. ATRIBUTOS GENERALES
        // =================================

        model.addAttribute(
            "recolector",
            nombreCompleto(recolector)
        );

        model.addAttribute(
            "totalHistorico",
            totalHistorico
        );

        // Es el total de registros que cumplen
        // los filtros, no solo la pagina actual.

        model.addAttribute(
            "totalMostrados",
            resultado.getTotalElements()
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
            "pesoTotal",
            pesoTotal
        );

        model.addAttribute(
            "puntosAcreditados",
            puntosAcreditados
        );

        model.addAttribute(
            "recolectas",
            filas
        );

        model.addAttribute(
            "materiales",
            materiales
        );

        // =================================
        // 14. CONSERVAR FILTROS EN PANTALLA
        // =================================

        model.addAttribute(
            "estadoSeleccionado",
            filtroEstado
        );

        model.addAttribute(
            "materialSeleccionado",
            materialId
        );

        model.addAttribute(
            "desde",
            desdeTexto
        );

        model.addAttribute(
            "hasta",
            hastaTexto
        );

        model.addAttribute(
            "idBuscado",
            idBuscado
        );

        model.addAttribute(
            "advertencia",
            advertencia
        );

        // =================================
        // 15. DATOS DE PAGINACION
        // =================================

        model.addAttribute(
            "paginaActual",
            resultado.getNumber()
        );

        model.addAttribute(
            "totalPaginas",
            Math.max(1, resultado.getTotalPages())
        );

        model.addAttribute(
            "ultimaPagina",
            Math.max(
                0,
                resultado.getTotalPages() - 1
            )
        );

        model.addAttribute(
            "hayAnterior",
            resultado.hasPrevious()
        );

        model.addAttribute(
            "haySiguiente",
            resultado.hasNext()
        );

        model.addAttribute(
            "tamanoPagina",
            tamanoPagina
        );

        model.addAttribute(
            "cantidadEnPagina",
            resultado.getNumberOfElements()
        );

        return "recolector-historial-completo";
    }

    // =====================================
    // CONVERTIR ESTADISTICA A LONG
    // =====================================

    private static long valorLong(
            Object valor) {

        if (valor == null) {
            return 0L;
        }

        return ((Number) valor).longValue();
    }

    // =====================================
    // CONVERTIR ESTADISTICA A BIGDECIMAL
    // =====================================

    private static BigDecimal valorDecimal(
            Object valor) {

        if (valor == null) {
            return BigDecimal.ZERO;
        }

        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }

        return new BigDecimal(valor.toString());
    }

    // =====================================
    // ACUMULAR ADVERTENCIAS
    // =====================================

    private static String agregarAdvertencia(
            String actual,
            String nueva) {

        if (actual == null || actual.isBlank()) {
            return nueva;
        }

        return actual + " " + nueva;
    }

    // =====================================
    // NOMBRE COMPLETO
    // LOGICA CONSERVADA
    // =====================================

    private static String nombreCompleto(
            Usuario usuario) {

        if (usuario == null) {
            return "No especificado";
        }

        String nombre =
            usuario.getNombre() == null
                ? ""
                : usuario.getNombre();

        String apellido =
            usuario.getApellido() == null
                ? ""
                : usuario.getApellido();

        return (nombre + " " + apellido).trim();
    }

    // =====================================
    // FILAS DEL HISTORIAL
    // LOGICA CONSERVADA
    // =====================================

    public static class FilaHistorial {

        private final Integer id;

        private final String fecha;

        private final String vecino;

        private final String material;

        private final BigDecimal peso;

        private final BigDecimal puntos;

        private final String estado;

        public FilaHistorial(Recolecta recolecta) {

            this.id =
                recolecta.getId_recolecta();

            this.fecha =
                recolecta.getFecha_recolecta() == null
                    ? "Sin fecha"
                    : recolecta.getFecha_recolecta()
                        .format(FORMATO_FECHA);

            this.vecino =
                nombreCompleto(
                    recolecta.getVecino()
                );

            this.material =
                recolecta.getMaterial() == null
                    ? "Sin material"
                    : recolecta.getMaterial()
                        .getTipo_material();

            this.peso =
                recolecta.getPeso() == null
                    ? BigDecimal.ZERO
                    : recolecta.getPeso();

            this.puntos =
                recolecta.getPuntos_generados() == null
                    ? BigDecimal.ZERO
                    : recolecta.getPuntos_generados();

            this.estado =
                recolecta.getEstado_validacion() == null
                    ? "sin_estado"
                    : recolecta.getEstado_validacion()
                        .name();
        }

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
