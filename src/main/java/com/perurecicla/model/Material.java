
package com.perurecicla.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * PERU RECICLA
 *
 * Entidad Material
 *
 * Representa los materiales reciclables
 * registrados en la base de datos MySQL.
 *
 * Tabla: Materiales
 */

@Entity
@Table(name = "Materiales")
public class Material {

    // IDENTIFICADOR DEL MATERIAL

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Integer id_material;

    // TIPO DE MATERIAL

    @Column(
        name = "tipo_material",
        nullable = false,
        length = 50
    )
    private String tipo_material;

    // PUNTOS GENERADOS POR GRAMO

    @Column(
        name = "puntos_por_gramo",
        nullable = false,
        precision = 10,
        scale = 4
    )
    private BigDecimal puntos_por_gramo;

    // IMAGEN DEL MATERIAL

    @Column(
        name = "url_imagen",
        length = 255
    )
    private String url_imagen;

    // DESCRIPCION DEL MATERIAL

    @Column(
        name = "descripcion",
        columnDefinition = "TEXT"
    )
    private String descripcion;

    // CONSTRUCTOR VACIO

    public Material() {
    }

    // GETTERS Y SETTERS

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
}
