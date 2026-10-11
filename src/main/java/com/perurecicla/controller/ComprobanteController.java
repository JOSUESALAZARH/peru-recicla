
package com.perurecicla.controller;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.CanjeRepository;

import com.perurecicla.service.ReporteService;
import com.perurecicla.service.UsuarioService;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.CacheControl;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.io.ByteArrayInputStream;
import java.util.Objects;

/**
 * PERU RECICLA
 *
 * Controlador de comprobantes PDF.
 *
 * Funcionalidades:
 *
 * 1. Identificar al usuario autenticado.
 * 2. Verificar que sea un vecino activo.
 * 3. Buscar el canje registrado en MySQL.
 * 4. Comprobar que pertenece al usuario.
 * 5. Generar el PDF mediante JasperReports.
 * 6. Descargar el comprobante.
 *
 * Arquitectura:
 *
 * Controller -> Repository -> MySQL
 * Controller -> ReporteService -> JasperReports
 *
 * Java 21
 * Spring Boot 3.4.2
 */
@Controller
public class ComprobanteController {

    // =====================================
    // SERVICIOS Y REPOSITORIOS
    // =====================================

    private final UsuarioService usuarioService;

    private final CanjeRepository canjeRepository;

    private final ReporteService reporteService;

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public ComprobanteController(
            UsuarioService usuarioService,
            CanjeRepository canjeRepository,
            ReporteService reporteService) {

        this.usuarioService = usuarioService;
        this.canjeRepository = canjeRepository;
        this.reporteService = reporteService;
    }

    // =====================================
    // DESCARGAR COMPROBANTE PDF
    // =====================================

    @GetMapping("/vecino/canjes/{id}/ticket")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarTicket(
            @PathVariable("id") Integer id,
            Principal principal) {

        // =================================
        // VERIFICAR SESION
        // =================================

        if (principal == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesion"
            );
        }

        // =================================
        // BUSCAR USUARIO AUTENTICADO
        // =================================

        Usuario usuario =
                usuarioService.buscarPorCorreo(
                        principal.getName()
                );

        if (usuario == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no encontrado"
            );
        }

        // =================================
        // VERIFICAR ROL Y ESTADO
        // =================================

        if (usuario.getRol()
                != Usuario.Rol.ROLE_vecino
                || usuario.getEstado()
                != Usuario.Estado.activo) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso exclusivo para vecinos activos"
            );
        }

        // =================================
        // VALIDAR IDENTIFICADOR
        // =================================

        if (id == null || id <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Comprobante no encontrado"
            );
        }

        // =================================
        // CONSULTAR CANJE EN MYSQL
        // =================================

        Canje canje = canjeRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Comprobante no encontrado"
                        )
                );

        // =================================
        // VERIFICAR PROPIETARIO
        // =================================

        // Un vecino no puede descargar
        // comprobantes de otros usuarios.

        if (canje.getUsuario() == null
                || !Objects.equals(
                        canje.getUsuario().getId_usuario(),
                        usuario.getId_usuario()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Comprobante no encontrado"
            );
        }

        // =================================
        // GENERAR PDF CON JASPERREPORTS
        // =================================

        ByteArrayInputStream archivo =
                reporteService.generarTicketCanjePdf(
                        canje
                );

        byte[] contenidoPdf = archivo.readAllBytes();

        if (contenidoPdf.length == 0) {

            throw new IllegalStateException(
                    "El comprobante PDF esta vacio"
            );
        }

        // =================================
        // DEFINIR NOMBRE DE ARCHIVO
        // =================================

        String nombreArchivo =
                "PERU_RECICLA_CANJE_"
                + id
                + ".pdf";

        ContentDisposition disposicion =
                ContentDisposition.attachment()
                        .filename(nombreArchivo)
                        .build();

        // =================================
        // DEVOLVER PDF AL NAVEGADOR
        // =================================

        return ResponseEntity.ok()

                .contentType(
                        MediaType.APPLICATION_PDF
                )

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposicion.toString()
                )

                .cacheControl(
                        CacheControl.noStore()
                )

                .contentLength(
                        contenidoPdf.length
                )

                .body(contenidoPdf);
    }
}
