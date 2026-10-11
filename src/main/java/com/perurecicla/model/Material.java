package com.perurecicla.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * PERU RECICLA - PASO 82
 *
 * Entidad de materiales reciclables.
 *
 * Conserva:
 * - Nombre
 * - Puntos por gramo
 * - Imagen
 * - Descripcion
 *
 * Incorpora estado activo/inactivo.
 */
@Entity
@Table(name = "Materiales")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Integer id_material;

    @Column(
        name = "tipo_material",
        nullable = false,
        length = 50
    )
    private String tipo_material;

    @Column(
        name = "puntos_por_gramo",
        nullable = false,
        precision = 10,
        scale = 4
    )
    private BigDecimal puntos_por_gramo;

    @Column(
        name = "url_imagen",
        length = 255
    )
    private String url_imagen;

    @Column(
        name = "descripcion",
        columnDefinition = "TEXT"
    )
    private String descripcion;

    // NUEVO: ESTADO DEL MATERIAL

    @Enumerated(EnumType.STRING)
    @Column(
        name = "estado",
        nullable = false,
        columnDefinition = "ENUM('activo','inactivo')"
    )
    private EstadoMaterial estado =
            EstadoMaterial.activo;

    public enum EstadoMaterial {
        activo,
        inactivo
    }

    public Material() {
    }

    public Integer getId_material() {
        return id_material;
    }

    public void setId_material(Integer id_material) {
        this.id_material = id_material;
    }

    public String getTipo_material() {
        return tipo_material;
    }

    public void setTipo_material(String tipo_material) {
        this.tipo_material = tipo_material;
    }

    public BigDecimal getPuntos_por_gramo() {
        return puntos_por_gramo;
    }

    public void setPuntos_por_gramo(
            BigDecimal puntos_por_gramo) {

        this.puntos_por_gramo = puntos_por_gramo;
    }

    public String getUrl_imagen() {
        return url_imagen;
    }

    public void setUrl_imagen(String url_imagen) {
        this.url_imagen = url_imagen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoMaterial getEstado() {
        return estado;
    }

    public void setEstado(EstadoMaterial estado) {
        this.estado = estado;
    }
}