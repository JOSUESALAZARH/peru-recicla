
package com.perurecicla.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PERU RECICLA
 *
 * Entidad Recolecta
 *
 * Representa los registros de materiales
 * reciclados por los vecinos.
 *
 * Tabla MySQL: Recolectas
 *
 * Relaciones:
 * - Usuario (Vecino)
 * - Usuario (Recolector)
 * - Material
 */

@Entity
@Table(name = "Recolectas")
public class Recolecta {

    // =====================================
    // IDENTIFICADOR DE RECOLECTA
    // =====================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recolecta")
    private Integer id_recolecta;

    // =====================================
    // RELACION CON EL VECINO
    // =====================================

    @ManyToOne
    @JoinColumn(
        name = "id_usuario",
        nullable = false
    )
    private Usuario vecino;

    // =====================================
    // RELACION CON EL RECOLECTOR
    // =====================================

    @ManyToOne
    @JoinColumn(
        name = "id_recolector",
        nullable = false
    )
    private Usuario recolector;

    // =====================================
    // RELACION CON EL MATERIAL
    // =====================================

    @ManyToOne
    @JoinColumn(
        name = "id_material",
        nullable = false
    )
    private Material material;

    // =====================================
    // PESO DEL MATERIAL EN GRAMOS
    // =====================================

    @Column(
        name = "peso_gramos",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal peso;

    // =====================================
    // PUNTOS GENERADOS
    // =====================================

    @Column(
        name = "puntos_generados",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal puntos_generados =
            BigDecimal.ZERO;

    // =====================================
    // FECHA DE RECOLECCION
    // =====================================

    @Column(name = "fecha_recolecta")
    private LocalDateTime fecha_recolecta =
            LocalDateTime.now();

    // =====================================
    // ESTADO DE VALIDACION
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_validacion")
    private EstadoValidacion estado_validacion =
            EstadoValidacion.pendiente;

    public enum EstadoValidacion {
        pendiente,
        validado,
        rechazado
    }

    // =====================================
    // CONSTRUCTOR VACIO
    // =====================================

    public Recolecta() {
    }

    // =====================================
    // GETTERS Y SETTERS
    // =====================================

    public Integer getId_recolecta() {
        return id_recolecta;
    }

    public void setId_recolecta(
            Integer id_recolecta) {
        this.id_recolecta = id_recolecta;
    }

    public Usuario getVecino() {
        return vecino;
    }

    public void setVecino(Usuario vecino) {
        this.vecino = vecino;
    }

    public Usuario getRecolector() {
        return recolector;
    }

    public void setRecolector(Usuario recolector) {
        this.recolector = recolector;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public BigDecimal getPuntos_generados() {
        return puntos_generados;
    }

    public void setPuntos_generados(
            BigDecimal puntos_generados) {
        this.puntos_generados = puntos_generados;
    }

    public LocalDateTime getFecha_recolecta() {
        return fecha_recolecta;
    }

    public void setFecha_recolecta(
            LocalDateTime fecha_recolecta) {
        this.fecha_recolecta = fecha_recolecta;
    }

    public EstadoValidacion getEstado_validacion() {
        return estado_validacion;
    }

    public void setEstado_validacion(
            EstadoValidacion estado_validacion) {
        this.estado_validacion = estado_validacion;
    }
}
