package com.perurecicla.service.impl;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Usuario;
import com.perurecicla.model.Recompensa;
import com.perurecicla.service.ReporteService;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * PERU RECICLA - PASO 85.1
 *
 * COMPROBANTE DE CANJE EN FORMATO A6.
 *
 * Dimensiones:
 * - Ancho: 298 puntos
 * - Alto: 420 puntos
 * - Aproximadamente 10.5 x 14.8 cm
 *
 * CONSERVA:
 * - Encabezado institucional
 * - Codigo de ticket
 * - Nombre del vecino
 * - Recompensa
 * - Puntos originales
 * - Fecha del canje
 * - Estado con color
 * - Aviso segun estado
 * - Mensaje de agradecimiento
 *
 * No modifica MySQL ni las operaciones
 * de canje y devolucion de puntos.
 */
@Service
public class ReporteServiceImpl implements ReporteService {

    // =====================================
    // FORMATO DE FECHA
    // =====================================

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm"
        );

    // =====================================
    // PLANTILLA JASPERREPORTS A6
    //
    // Pagina: 298 x 420
    // Margenes: 12 horizontales
    // Margenes: 10 verticales
    // Area util: 274 x 400
    // =====================================

    private static final String PLANTILLA_JRXML = """
        <?xml version="1.0" encoding="UTF-8"?>

        <jasperReport
            xmlns="http://jasperreports.sourceforge.net/jasperreports"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
            name="TicketA6PeruRecicla"
            pageWidth="298"
            pageHeight="420"
            columnWidth="274"
            leftMargin="12"
            rightMargin="12"
            topMargin="10"
            bottomMargin="10">

            <!-- PARAMETROS DEL COMPROBANTE -->

            <parameter name="ticket"
                       class="java.lang.String"/>

            <parameter name="vecino"
                       class="java.lang.String"/>

            <parameter name="recompensa"
                       class="java.lang.String"/>

            <parameter name="puntos"
                       class="java.lang.String"/>

            <parameter name="fecha"
                       class="java.lang.String"/>

            <parameter name="estado"
                       class="java.lang.String"/>

            <parameter name="aviso"
                       class="java.lang.String"/>

            <detail>

                <band height="398">

                    <!-- =========================
                         ENCABEZADO VERDE
                    ========================= -->

                    <rectangle radius="12">
                        <reportElement
                            x="0"
                            y="0"
                            width="274"
                            height="58"
                            mode="Opaque"
                            backcolor="#10633E"/>

                        <graphicElement>
                            <pen lineWidth="0.0"/>
                        </graphicElement>
                    </rectangle>

                    <!-- LOGOTIPO -->

                    <ellipse>
                        <reportElement
                            x="12"
                            y="13"
                            width="32"
                            height="32"
                            mode="Opaque"
                            backcolor="#FFFFFF"/>

                        <graphicElement>
                            <pen lineWidth="0.0"/>
                        </graphicElement>
                    </ellipse>

                    <staticText>
                        <reportElement
                            x="14"
                            y="15"
                            width="28"
                            height="27"
                            forecolor="#10633E"/>

                        <textElement
                            textAlignment="Center"
                            verticalAlignment="Middle">

                            <font
                                fontName="SansSerif"
                                size="11"
                                isBold="true"/>
                        </textElement>

                        <text><![CDATA[PR]]></text>
                    </staticText>

                    <!-- TITULO -->

                    <staticText>
                        <reportElement
                            x="53"
                            y="8"
                            width="212"
                            height="24"
                            forecolor="#FFFFFF"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="16"
                                isBold="true"/>
                        </textElement>

                        <text><![CDATA[PERÚ RECICLA]]></text>
                    </staticText>

                    <!-- SUBTITULO -->

                    <staticText>
                        <reportElement
                            x="54"
                            y="32"
                            width="208"
                            height="12"
                            forecolor="#FFFFFF"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="7"
                                isBold="true"/>
                        </textElement>

                        <text><![CDATA[ECOSISTEMA DIGITAL DE INCENTIVOS]]></text>
                    </staticText>

                    <staticText>
                        <reportElement
                            x="54"
                            y="44"
                            width="208"
                            height="9"
                            forecolor="#DBF0E4"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="6"
                                isBold="false"/>
                        </textElement>

                        <text><![CDATA[Recicla, acumula puntos y canjea recompensas]]></text>
                    </staticText>

                    <!-- =========================
                         COMPROBANTE Y CODIGO
                    ========================= -->

                    <rectangle radius="9">
                        <reportElement
                            x="0"
                            y="65"
                            width="274"
                            height="48"
                            mode="Opaque"
                            backcolor="#E6F5EB"/>

                        <graphicElement>
                            <pen lineWidth="0.0"/>
                        </graphicElement>
                    </rectangle>

                    <staticText>
                        <reportElement
                            x="10"
                            y="70"
                            width="250"
                            height="18"
                            forecolor="#105A38"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="11"
                                isBold="true"/>
                        </textElement>

                        <text><![CDATA[COMPROBANTE DE CANJE]]></text>
                    </staticText>

                    <staticText>
                        <reportElement
                            x="10"
                            y="92"
                            width="48"
                            height="14"
                            forecolor="#587565"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="8"/>
                        </textElement>

                        <text><![CDATA[Ticket:]]></text>
                    </staticText>

                    <textField textAdjust="ScaleFont">
                        <reportElement
                            x="56"
                            y="91"
                            width="208"
                            height="17"
                            forecolor="#105A38"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="10"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA[$P{ticket}]]></textFieldExpression>
                    </textField>

                    <!-- =========================
                         DETALLE DEL CANJE
                    ========================= -->

                    <rectangle radius="9">
                        <reportElement
                            x="0"
                            y="121"
                            width="274"
                            height="179"
                            mode="Opaque"
                            backcolor="#F5F8F6"/>

                        <graphicElement>
                            <pen
                                lineWidth="0.6"
                                lineColor="#E0EAE3"/>
                        </graphicElement>
                    </rectangle>

                    <staticText>
                        <reportElement
                            x="10"
                            y="127"
                            width="252"
                            height="17"
                            forecolor="#105A38"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="10"
                                isBold="true"/>
                        </textElement>

                        <text><![CDATA[DETALLE DE LA OPERACIÓN]]></text>
                    </staticText>

                    <!-- VECINO -->

                    <staticText>
                        <reportElement
                            x="10"
                            y="153"
                            width="78"
                            height="21"
                            forecolor="#587565"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="8"/>
                        </textElement>

                        <text><![CDATA[Vecino]]></text>
                    </staticText>

                    <textField textAdjust="ScaleFont">
                        <reportElement
                            x="96"
                            y="146"
                            width="168"
                            height="27"
                            forecolor="#193728"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="9"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA[$P{vecino}]]></textFieldExpression>
                    </textField>

                    <line>
                        <reportElement
                            x="10"
                            y="176"
                            width="254"
                            height="1"/>

                        <graphicElement>
                            <pen
                                lineWidth="0.5"
                                lineColor="#DDE8E0"/>
                        </graphicElement>
                    </line>

                    <!-- RECOMPENSA -->

                    <staticText>
                        <reportElement
                            x="10"
                            y="185"
                            width="78"
                            height="22"
                            forecolor="#587565"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="8"/>
                        </textElement>

                        <text><![CDATA[Recompensa]]></text>
                    </staticText>

                    <textField textAdjust="ScaleFont">
                        <reportElement
                            x="96"
                            y="180"
                            width="168"
                            height="29"
                            forecolor="#193728"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="9"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA[$P{recompensa}]]></textFieldExpression>
                    </textField>

                    <line>
                        <reportElement
                            x="10"
                            y="212"
                            width="254"
                            height="1"/>

                        <graphicElement>
                            <pen
                                lineWidth="0.5"
                                lineColor="#DDE8E0"/>
                        </graphicElement>
                    </line>

                    <!-- PUNTOS -->

                    <staticText>
                        <reportElement
                            x="10"
                            y="221"
                            width="78"
                            height="21"
                            forecolor="#587565"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="8"/>
                        </textElement>

                        <text><![CDATA[Puntos usados]]></text>
                    </staticText>

                    <textField>
                        <reportElement
                            x="96"
                            y="217"
                            width="168"
                            height="23"
                            forecolor="#16804D"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="12"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA[$P{puntos} + " pts"]]></textFieldExpression>
                    </textField>

                    <line>
                        <reportElement
                            x="10"
                            y="243"
                            width="254"
                            height="1"/>

                        <graphicElement>
                            <pen
                                lineWidth="0.5"
                                lineColor="#DDE8E0"/>
                        </graphicElement>
                    </line>

                    <!-- FECHA -->

                    <staticText>
                        <reportElement
                            x="10"
                            y="252"
                            width="78"
                            height="19"
                            forecolor="#587565"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="8"/>
                        </textElement>

                        <text><![CDATA[Fecha]]></text>
                    </staticText>

                    <textField>
                        <reportElement
                            x="96"
                            y="249"
                            width="168"
                            height="23"
                            forecolor="#193728"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="9"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA[$P{fecha}]]></textFieldExpression>
                    </textField>

                    <line>
                        <reportElement
                            x="10"
                            y="274"
                            width="254"
                            height="1"/>

                        <graphicElement>
                            <pen
                                lineWidth="0.5"
                                lineColor="#DDE8E0"/>
                        </graphicElement>
                    </line>

                    <!-- ESTADO -->

                    <staticText>
                        <reportElement
                            x="10"
                            y="281"
                            width="78"
                            height="17"
                            forecolor="#587565"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="8"/>
                        </textElement>

                        <text><![CDATA[Estado]]></text>
                    </staticText>

                    <!-- PENDIENTE: AMARILLO -->

                    <textField>
                        <reportElement
                            x="96"
                            y="278"
                            width="168"
                            height="20"
                            forecolor="#825400">

                            <printWhenExpression><![CDATA["pendiente".equals($P{estado})]]></printWhenExpression>
                        </reportElement>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="10"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA["PENDIENTE"]]></textFieldExpression>
                    </textField>

                    <!-- ENTREGADO: VERDE -->

                    <textField>
                        <reportElement
                            x="96"
                            y="278"
                            width="168"
                            height="20"
                            forecolor="#12633A">

                            <printWhenExpression><![CDATA["entregado".equals($P{estado})]]></printWhenExpression>
                        </reportElement>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="10"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA["ENTREGADO"]]></textFieldExpression>
                    </textField>

                    <!-- CANCELADO: ROJO -->

                    <textField>
                        <reportElement
                            x="96"
                            y="278"
                            width="168"
                            height="20"
                            forecolor="#A52828">

                            <printWhenExpression><![CDATA["cancelado".equals($P{estado})]]></printWhenExpression>
                        </reportElement>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="10"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA["CANCELADO"]]></textFieldExpression>
                    </textField>

                    <!-- OTROS ESTADOS -->

                    <textField textAdjust="ScaleFont">
                        <reportElement
                            x="96"
                            y="278"
                            width="168"
                            height="20"
                            forecolor="#60736A">

                            <printWhenExpression><![CDATA[!"pendiente".equals($P{estado}) && !"entregado".equals($P{estado}) && !"cancelado".equals($P{estado})]]></printWhenExpression>
                        </reportElement>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="9"
                                isBold="true"/>
                        </textElement>

                        <textFieldExpression><![CDATA[$P{estado}]]></textFieldExpression>
                    </textField>

                    <!-- =========================
                         AVISO DEL TICKET
                    ========================= -->

                    <staticText>
                        <reportElement
                            x="7"
                            y="307"
                            width="260"
                            height="16"
                            forecolor="#105A38"/>

                        <textElement verticalAlignment="Middle">
                            <font
                                fontName="SansSerif"
                                size="9"
                                isBold="true"/>
                        </textElement>

                        <text><![CDATA[IMPORTANTE]]></text>
                    </staticText>

                    <textField textAdjust="ScaleFont">
                        <reportElement
                            x="7"
                            y="325"
                            width="260"
                            height="29"
                            forecolor="#60736A"/>

                        <textElement verticalAlignment="Top">
                            <font
                                fontName="SansSerif"
                                size="8"/>
                        </textElement>

                        <textFieldExpression><![CDATA[$P{aviso}]]></textFieldExpression>
                    </textField>

                    <!-- =========================
                         PIE DEL COMPROBANTE
                    ========================= -->

                    <line>
                        <reportElement
                            x="0"
                            y="359"
                            width="274"
                            height="1"/>

                        <graphicElement>
                            <pen
                                lineWidth="0.6"
                                lineColor="#DDE8E0"/>
                        </graphicElement>
                    </line>

                    <staticText>
                        <reportElement
                            x="0"
                            y="367"
                            width="274"
                            height="17"
                            forecolor="#105A38"/>

                        <textElement
                            textAlignment="Center"
                            verticalAlignment="Middle">

                            <font
                                fontName="SansSerif"
                                size="9"
                                isBold="true"/>
                        </textElement>

                        <text><![CDATA[¡Gracias por contribuir al reciclaje!]]></text>
                    </staticText>

                    <staticText>
                        <reportElement
                            x="0"
                            y="385"
                            width="274"
                            height="11"
                            forecolor="#60736A"/>

                        <textElement
                            textAlignment="Center"
                            verticalAlignment="Middle">

                            <font
                                fontName="SansSerif"
                                size="7"/>
                        </textElement>

                        <text><![CDATA[PERÚ RECICLA | Comprobante electrónico]]></text>
                    </staticText>

                </band>

            </detail>

        </jasperReport>
        """;

    // =====================================
    // GENERAR COMPROBANTE PDF
    // =====================================

    @Override
    public ByteArrayInputStream generarTicketCanjePdf(
            Canje canje) {

        // =================================
        // 1. VALIDAR CANJE
        // =================================

        if (canje == null) {

            throw new IllegalArgumentException(
                "El canje es obligatorio"
            );
        }

        if (canje.getUsuario() == null) {

            throw new IllegalArgumentException(
                "El canje no tiene usuario"
            );
        }

        if (canje.getRecompensa() == null) {

            throw new IllegalArgumentException(
                "El canje no tiene recompensa"
            );
        }

        if (canje.getCodigo_ticket() == null
                || canje.getCodigo_ticket().isBlank()) {

            throw new IllegalArgumentException(
                "El canje no tiene codigo de ticket"
            );
        }

        // =================================
        // 2. RECUPERAR DATOS
        // =================================

        Usuario usuario =
            canje.getUsuario();

        Recompensa recompensa =
            canje.getRecompensa();

        String nombreVecino = (
            textoSeguro(usuario.getNombre())
            + " "
            + textoSeguro(usuario.getApellido())
        ).trim();

        String nombreRecompensa =
            textoSeguro(
                recompensa.getNombre()
            );

        // PUNTOS ORIGINALES DEL TICKET.
        // NO UTILIZAR EL PRECIO ACTUAL
        // DE LA RECOMPENSA.

        String puntosUtilizados =
            canje.getPuntos_descontados() == null
                ? "0"
                : canje.getPuntos_descontados()
                    .toString();

        String fechaCanje =
            canje.getFecha_canje() == null
                ? "Sin fecha"
                : canje.getFecha_canje()
                    .format(FORMATO_FECHA);

        String estadoCanje =
            canje.getEstado() == null
                ? "Sin estado"
                : canje.getEstado().name();

        // =================================
        // 3. MENSAJE SEGUN ESTADO
        //
        // Textos mas compactos para A6,
        // conservando su significado.
        // =================================

        String aviso = switch (estadoCanje) {

            case "pendiente" ->
                "Canje registrado. La recompensa "
                + "aun no ha sido entregada.";

            case "entregado" ->
                "La entrega fisica de la recompensa "
                + "ya fue confirmada.";

            case "cancelado" ->
                "Canje cancelado. Se devolvieron "
                + "los puntos y una unidad de stock.";

            default ->
                "Estado actual del canje "
                + "registrado en PERU RECICLA.";
        };

        // =================================
        // 4. PARAMETROS DE JASPERREPORTS
        // =================================

        Map<String, Object> parametros =
            new HashMap<>();

        parametros.put(
            "ticket",
            canje.getCodigo_ticket()
        );

        parametros.put(
            "vecino",
            nombreVecino
        );

        parametros.put(
            "recompensa",
            nombreRecompensa
        );

        parametros.put(
            "puntos",
            puntosUtilizados
        );

        parametros.put(
            "fecha",
            fechaCanje
        );

        parametros.put(
            "estado",
            estadoCanje
        );

        parametros.put(
            "aviso",
            aviso
        );

        // =================================
        // 5. COMPILAR Y GENERAR PDF
        // =================================

        try {

            byte[] bytesPlantilla =
                PLANTILLA_JRXML.getBytes(
                    StandardCharsets.UTF_8
                );

            JasperReport reporte =
                JasperCompileManager.compileReport(
                    new ByteArrayInputStream(
                        bytesPlantilla
                    )
                );

            JasperPrint documento =
                JasperFillManager.fillReport(
                    reporte,
                    parametros,
                    new JREmptyDataSource(1)
                );

            byte[] pdf =
                JasperExportManager.exportReportToPdf(
                    documento
                );

            return new ByteArrayInputStream(
                pdf
            );

        } catch (JRException e) {

            throw new IllegalStateException(
                "No se pudo generar el ticket PDF",
                e
            );
        }
    }

    // =====================================
    // EVITAR VALORES NULOS
    // =====================================

    private String textoSeguro(
            String valor) {

        return valor == null
            ? ""
            : valor;
    }
}