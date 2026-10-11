package com.perurecicla.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PERU RECICLA
 *
 * Entidad Canje
 *
 * Representa las operaciones de canje
 * realizadas por los vecinos utilizando
 * sus puntos acumulados.
 *
 * Tabla MySQL: Canjes
 *
 * Relaciones:
 * - Usuario (Vecino)
 * - Recompensa
 */

@Entity
@Table(name = "Canjes")
public class Canje {

    // =====================================
    // IDENTIFICADOR DEL CANJE
    // =====================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_canje")
    private Integer id_canje;

    // =====================================
    // RELACION CON EL USUARIO
    // =====================================

    @ManyToOne
    @JoinColumn(
        name = "id_usuario",
        nullable = false
    )
    private Usuario usuario;

    // =====================================
    // RELACION CON LA RECOMPENSA
    // =====================================

    @ManyToOne
    @JoinColumn(
        name = "id_recompensa",
        nullable = false
    )
    private Recompensa recompensa;

    // =====================================
    // PUNTOS DESCONTADOS
    // =====================================

    @Column(
        name = "puntos_descontados",
        nullable = false
    )
    private Integer puntos_descontados;

    // =====================================
    // CODIGO UNICO DEL TICKET
    // =====================================

    @Column(
        name = "codigo_ticket",
        nullable = false,
        unique = true,
        length = 20
    )
    private String codigo_ticket;

    // =====================================
    // FECHA DEL CANJE
    // =====================================

    @Column(name = "fecha_canje")
    private LocalDateTime fecha_canje =
            LocalDateTime.now();

    // =====================================
    // ESTADO DEL CANJE
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoCanje estado =
            EstadoCanje.pendiente;

    public enum EstadoCanje {
        pendiente,
        entregado,
        cancelado
    }

    // =====================================
    // AUDITORIA DE CANCELACION - PASO 83
    // Solo se completa si fue cancelado.
    // =====================================

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivo_cancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fecha_cancelacion;

    @Column(name = "id_administrador_cancelacion")
    private Integer id_administrador_cancelacion;

    // =====================================
    // CONSTRUCTOR VACIO
    // =====================================

    public Canje() {
    }

    // =====================================
    // GETTERS Y SETTERS
    // =====================================

    public Integer getId_canje() {
        return id_canje;
    }

    public void setId_canje(Integer id_canje) {
        this.id_canje = id_canje;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Recompensa getRecompensa() {
        return recompensa;
    }

    public void setRecompensa(Recompensa recompensa) {
        this.recompensa = recompensa;
    }

    public Integer getPuntos_descontados() {
        return puntos_descontados;
    }

    public void setPuntos_descontados(
            Integer puntos_descontados) {
        this.puntos_descontados = puntos_descontados;
    }

    public String getCodigo_ticket() {
        return codigo_ticket;
    }

    public void setCodigo_ticket(
            String codigo_ticket) {
        this.codigo_ticket = codigo_ticket;
    }

    public LocalDateTime getFecha_canje() {
        return fecha_canje;
    }

    public void setFecha_canje(
            LocalDateTime fecha_canje) {
        this.fecha_canje = fecha_canje;
    }

    public EstadoCanje getEstado() {
        return estado;
    }

    public void setEstado(
            EstadoCanje estado) {
        this.estado = estado;
    }

    public String getMotivo_cancelacion() {
        return motivo_cancelacion;
    }

    public void setMotivo_cancelacion(String motivo_cancelacion) {
        this.motivo_cancelacion = motivo_cancelacion;
    }

    public LocalDateTime getFecha_cancelacion() {
        return fecha_cancelacion;
    }

    public void setFecha_cancelacion(LocalDateTime fecha_cancelacion) {
        this.fecha_cancelacion = fecha_cancelacion;
    }

    public Integer getId_administrador_cancelacion() {
        return id_administrador_cancelacion;
    }

    public void setId_administrador_cancelacion(Integer id_administrador_cancelacion) {
        this.id_administrador_cancelacion = id_administrador_cancelacion;
    }
}