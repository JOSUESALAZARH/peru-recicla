
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
 * PERU RECICLA - PASO 49
 * Comprobante institucional de canje mediante JasperReports.
 * Java 21 / Spring Boot 3.4.2 / JasperReports 6.21.3.
 *
 * El PDF refleja el estado real del canje; si esta pendiente,
 * no debe interpretarse como constancia de entrega.
 */
@Service
public class ReporteServiceImpl implements ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Diseno A4: se genera en memoria; no requiere archivo .jrxml.
    // Los datos vienen de parametros, no de texto concatenado al XML.
    private static final String PLANTILLA_JRXML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <jasperReport
            xmlns="http://jasperreports.sourceforge.net/jasperreports"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
            name="ComprobanteCanjePeruRecicla"
            pageWidth="595" pageHeight="842"
            columnWidth="515" leftMargin="40" rightMargin="40"
            topMargin="30" bottomMargin="30">

            <parameter name="ticket" class="java.lang.String"/>
            <parameter name="vecino" class="java.lang.String"/>
            <parameter name="recompensa" class="java.lang.String"/>
            <parameter name="puntos" class="java.lang.String"/>
            <parameter name="fecha" class="java.lang.String"/>
            <parameter name="estado" class="java.lang.String"/>

            <detail>
                <band height="720">

                    <!-- ENCABEZADO VERDE -->
                    <rectangle radius="18">
                        <reportElement x="0" y="0" width="515" height="132"
                            mode="Opaque" backcolor="#10633E"/>
                        <graphicElement><pen lineWidth="0.0"/></graphicElement>
                    </rectangle>

                    <!-- SIMBOLO INSTITUCIONAL SIN FUENTES EXTERNAS -->
                    <ellipse>
                        <reportElement x="26" y="29" width="55" height="55"
                            mode="Opaque" backcolor="#FFFFFF"/>
                        <graphicElement><pen lineWidth="0.0"/></graphicElement>
                    </ellipse>
                    <staticText>
                        <reportElement x="32" y="42" width="43" height="26"
                            forecolor="#10633E"/>
                        <textElement textAlignment="Center" verticalAlignment="Middle">
                            <font fontName="SansSerif" size="17" isBold="true"/>
                        </textElement>
                        <text><![CDATA[PR]]></text>
                    </staticText>
                    <staticText>
                        <reportElement x="100" y="25" width="390" height="45"
                            forecolor="#FFFFFF"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="26" isBold="true"/>
                        </textElement>
                        <text><![CDATA[PERÚ RECICLA]]></text>
                    </staticText>
                    <staticText>
                        <reportElement x="101" y="70" width="385" height="20"
                            forecolor="#FFFFFF"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10" isBold="true"/>
                        </textElement>
                        <text><![CDATA[ECOSISTEMA DIGITAL DE INCENTIVOS]]></text>
                    </staticText>
                    <staticText>
                        <reportElement x="101" y="91" width="385" height="18"
                            forecolor="#DBF0E4"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10"/>
                        </textElement>
                        <text><![CDATA[Recicla, acumula puntos y canjea recompensas]]></text>
                    </staticText>

                    <!-- IDENTIFICACION DEL COMPROBANTE -->
                    <rectangle radius="12">
                        <reportElement x="0" y="149" width="515" height="84"
                            mode="Opaque" backcolor="#E6F5EB"/>
                        <graphicElement><pen lineWidth="0.0"/></graphicElement>
                    </rectangle>
                    <staticText>
                        <reportElement x="22" y="163" width="460" height="26"
                            forecolor="#105A38"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="15" isBold="true"/>
                        </textElement>
                        <text><![CDATA[COMPROBANTE DE CANJE]]></text>
                    </staticText>
                    <staticText>
                        <reportElement x="22" y="193" width="110" height="20"
                            forecolor="#587565"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10"/>
                        </textElement>
                        <text><![CDATA[Código de ticket:]]></text>
                    </staticText>
                    <textField isStretchWithOverflow="true">
                        <reportElement x="135" y="192" width="354" height="23"
                            forecolor="#105A38"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="12" isBold="true"/>
                        </textElement>
                        <textFieldExpression><![CDATA[$P{ticket}]]></textFieldExpression>
                    </textField>

                    <!-- DATOS DEL CANJE -->
                    <rectangle radius="12">
                        <reportElement x="0" y="249" width="515" height="315"
                            mode="Opaque" backcolor="#F5F8F6"/>
                        <graphicElement>
                            <pen lineWidth="0.8" lineColor="#E0EAE3"/>
                        </graphicElement>
                    </rectangle>
                    <staticText>
                        <reportElement x="23" y="260" width="460" height="24"
                            forecolor="#105A38"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="13" isBold="true"/>
                        </textElement>
                        <text><![CDATA[DETALLE DE LA OPERACIÓN]]></text>
                    </staticText>

                    <!-- FILA 1: VECINO -->
                    <staticText>
                        <reportElement x="23" y="297" width="165" height="23"
                            forecolor="#587565"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10"/>
                        </textElement>
                        <text><![CDATA[Vecino]]></text>
                    </staticText>
                    <textField isStretchWithOverflow="true">
                        <reportElement x="190" y="296" width="293" height="30"
                            forecolor="#193728"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="11" isBold="true"/>
                        </textElement>
                        <textFieldExpression><![CDATA[$P{vecino}]]></textFieldExpression>
                    </textField>
                    <line>
                        <reportElement x="23" y="335" width="469" height="1"/>
                        <graphicElement><pen lineWidth="0.7" lineColor="#DDE8E0"/></graphicElement>
                    </line>

                    <!-- FILA 2: RECOMPENSA -->
                    <staticText>
                        <reportElement x="23" y="346" width="165" height="23"
                            forecolor="#587565"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10"/>
                        </textElement>
                        <text><![CDATA[Recompensa]]></text>
                    </staticText>
                    <textField isStretchWithOverflow="true">
                        <reportElement x="190" y="345" width="293" height="31"
                            forecolor="#193728"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="11" isBold="true"/>
                        </textElement>
                        <textFieldExpression><![CDATA[$P{recompensa}]]></textFieldExpression>
                    </textField>
                    <line>
                        <reportElement x="23" y="384" width="469" height="1"/>
                        <graphicElement><pen lineWidth="0.7" lineColor="#DDE8E0"/></graphicElement>
                    </line>

                    <!-- FILA 3: PUNTOS -->
                    <staticText>
                        <reportElement x="23" y="395" width="165" height="23"
                            forecolor="#587565"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10"/>
                        </textElement>
                        <text><![CDATA[Puntos utilizados]]></text>
                    </staticText>
                    <textField>
                        <reportElement x="190" y="392" width="293" height="30"
                            forecolor="#16804D"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="15" isBold="true"/>
                        </textElement>
                        <textFieldExpression><![CDATA[$P{puntos} + " pts"]]></textFieldExpression>
                    </textField>
                    <line>
                        <reportElement x="23" y="433" width="469" height="1"/>
                        <graphicElement><pen lineWidth="0.7" lineColor="#DDE8E0"/></graphicElement>
                    </line>

                    <!-- FILA 4: FECHA -->
                    <staticText>
                        <reportElement x="23" y="444" width="165" height="23"
                            forecolor="#587565"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10"/>
                        </textElement>
                        <text><![CDATA[Fecha del canje]]></text>
                    </staticText>
                    <textField>
                        <reportElement x="190" y="443" width="293" height="25"
                            forecolor="#193728"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="11" isBold="true"/>
                        </textElement>
                        <textFieldExpression><![CDATA[$P{fecha}]]></textFieldExpression>
                    </textField>
                    <line>
                        <reportElement x="23" y="482" width="469" height="1"/>
                        <graphicElement><pen lineWidth="0.7" lineColor="#DDE8E0"/></graphicElement>
                    </line>

                    <!-- FILA 5: ESTADO -->
                    <staticText>
                        <reportElement x="23" y="494" width="165" height="23"
                            forecolor="#587565"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="10"/>
                        </textElement>
                        <text><![CDATA[Estado del canje]]></text>
                    </staticText>
                    <textField>
                        <reportElement x="190" y="493" width="293" height="26"
                            forecolor="#105A38"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="11" isBold="true"/>
                        </textElement>
                        <textFieldExpression><![CDATA[$P{estado}]]></textFieldExpression>
                    </textField>

                    <!-- AVISO DE ENTREGA -->
                    <staticText>
                        <reportElement x="10" y="582" width="495" height="20"
                            forecolor="#105A38"/>
                        <textElement verticalAlignment="Middle">
                            <font fontName="SansSerif" size="11" isBold="true"/>
                        </textElement>
                        <text><![CDATA[IMPORTANTE]]></text>
                    </staticText>
                    <staticText>
                        <reportElement x="10" y="607" width="495" height="40"
                            forecolor="#5F7469"/>
                        <textElement>
                            <font fontName="SansSerif" size="9"/>
                        </textElement>
                        <text><![CDATA[Este comprobante acredita el registro del canje en PERÚ RECICLA. Si el estado es pendiente, no confirma la entrega física de la recompensa.]]></text>
                    </staticText>
                    <line>
                        <reportElement x="0" y="659" width="515" height="1"/>
                        <graphicElement><pen lineWidth="0.8" lineColor="#DDE8E0"/></graphicElement>
                    </line>
                    <staticText>
                        <reportElement x="0" y="670" width="515" height="23"
                            forecolor="#105A38"/>
                        <textElement textAlignment="Center" verticalAlignment="Middle">
                            <font fontName="SansSerif" size="11" isBold="true"/>
                        </textElement>
                        <text><![CDATA[¡Gracias por contribuir al reciclaje!]]></text>
                    </staticText>
                    <staticText>
                        <reportElement x="0" y="695" width="515" height="18"
                            forecolor="#60736A"/>
                        <textElement textAlignment="Center" verticalAlignment="Middle">
                            <font fontName="SansSerif" size="8"/>
                        </textElement>
                        <text><![CDATA[PERÚ RECICLA  |  Comprobante electrónico de canje]]></text>
                    </staticText>

                </band>
            </detail>
        </jasperReport>
        """;

    @Override
    public ByteArrayInputStream generarTicketCanjePdf(Canje canje) {

        if (canje == null) {
            throw new IllegalArgumentException("El canje es obligatorio");
        }
        if (canje.getUsuario() == null) {
            throw new IllegalArgumentException("El canje no tiene usuario");
        }
        if (canje.getRecompensa() == null) {
            throw new IllegalArgumentException("El canje no tiene recompensa");
        }
        if (canje.getCodigo_ticket() == null
                || canje.getCodigo_ticket().isBlank()) {
            throw new IllegalArgumentException("El canje no tiene codigo de ticket");
        }

        Usuario usuario = canje.getUsuario();
        Recompensa recompensa = canje.getRecompensa();

        String nombreVecino = (
                textoSeguro(usuario.getNombre()) + " "
                + textoSeguro(usuario.getApellido())
        ).trim();

        String nombreRecompensa = textoSeguro(recompensa.getNombre());

        String puntosUtilizados = canje.getPuntos_descontados() == null
                ? "0"
                : canje.getPuntos_descontados().toString();

        String fechaCanje = canje.getFecha_canje() == null
                ? "Sin fecha"
                : canje.getFecha_canje().format(FORMATO_FECHA);

        String estadoCanje = canje.getEstado() == null
                ? "Sin estado"
                : canje.getEstado().name();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ticket", canje.getCodigo_ticket());
        parametros.put("vecino", nombreVecino);
        parametros.put("recompensa", nombreRecompensa);
        parametros.put("puntos", puntosUtilizados);
        parametros.put("fecha", fechaCanje);
        parametros.put("estado", estadoCanje);

        try {
            byte[] bytesPlantilla = PLANTILLA_JRXML.getBytes(StandardCharsets.UTF_8);

            JasperReport reporte = JasperCompileManager.compileReport(
                    new ByteArrayInputStream(bytesPlantilla)
            );

            JasperPrint documento = JasperFillManager.fillReport(
                    reporte,
                    parametros,
                    new JREmptyDataSource(1)
            );

            byte[] pdf = JasperExportManager.exportReportToPdf(documento);
            return new ByteArrayInputStream(pdf);

        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el ticket PDF", e);
        }
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
