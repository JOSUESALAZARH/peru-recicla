
package com.perurecicla.service;

import com.perurecicla.model.Canje;

import java.io.ByteArrayInputStream;

/**
 * PERU RECICLA
 *
 * Servicio de Reportes
 *
 * Define las operaciones relacionadas
 * con la generacion de documentos PDF
 * utilizando JasperReports.
 *
 * Funcionalidades:
 *
 * 1. Generar comprobantes de canje.
 * 2. Obtener informacion del canje.
 * 3. Generar tickets en formato PDF.
 * 4. Permitir la descarga de tickets.
 *
 * Tecnologias:
 *
 * Java 21
 * Spring Boot 3.4.2
 * JasperReports
 * JPA / Hibernate
 * MySQL
 *
 * Arquitectura:
 * Controller -> Service -> Repository
 */

public interface ReporteService {

    // =====================================
    // GENERAR TICKET PDF DE CANJE
    // =====================================

    ByteArrayInputStream generarTicketCanjePdf(
        Canje canje
    );

}
