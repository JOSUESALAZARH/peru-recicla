
package com.perurecicla.model;

import jakarta.persistence.*;

/**
 * PERU RECICLA
 *
 * Entidad Recompensa
 *
 * Representa las recompensas disponibles
 * que los vecinos pueden obtener mediante
 * el canje de puntos acumulados.
 *
 * Tabla MySQL: Recompensas
 */

@Entity
@Table(name = "Recompensas")
public class Recompensa {

    // =====================================
    // IDENTIFICADOR DE RECOMPENSA
    // =====================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recompensa")
    private Integer id_recompensa;

    // =====================================
    // NOMBRE DE LA RECOMPENSA
    // =====================================

    @Column(
        name = "nombre",
        nullable = false,
        length = 100
    )
    private String nombre;

    // =====================================
    // CATEGORIA DE RECOMPENSA
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private Categoria categoria;

    public enum Categoria {
        alimentos,
        hogar,
        limpieza,
        tecnologia
    }

    // =====================================
    // DESCRIPCION DE LA RECOMPENSA
    // =====================================

    @Column(
        name = "descripcion",
        columnDefinition = "TEXT"
    )
    private String descripcion;

    // =====================================
    // COSTO EN PUNTOS
    // =====================================

    @Column(
        name = "costo_puntos",
        nullable = false
    )
    private Integer costo_puntos;

    // =====================================
    // STOCK DISPONIBLE
    // =====================================

    @Column(
        name = "stock",
        nullable = false
    )
    private Integer stock;

    // =====================================
    // IMAGEN DE LA RECOMPENSA
    // =====================================

    @Column(
        name = "url_imagen",
        length = 255
    )
    private String url_imagen;

    // =====================================
    // ESTADO DE LA RECOMPENSA
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoRecompensa estado =
            EstadoRecompensa.activo;

    public enum EstadoRecompensa {
        activo,
        inactivo
    }

    // =====================================
    // CONSTRUCTOR VACIO
    // =====================================

    public Recompensa() {
    }

    // =====================================
    // GETTERS Y SETTERS
    // =====================================

    public Integer getId_recompensa() {
        return id_recompensa;
    }

    public void setId_recompensa(
            Integer id_recompensa) {
        this.id_recompensa = id_recompensa;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCosto_puntos() {
        return costo_puntos;
    }

    public void setCosto_puntos(Integer costo_puntos) {
        this.costo_puntos = costo_puntos;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getUrl_imagen() {
        return url_imagen;
    }

    public void setUrl_imagen(String url_imagen) {
        this.url_imagen = url_imagen;
    }

    public EstadoRecompensa getEstado() {
        return estado;
    }

    public void setEstado(EstadoRecompensa estado) {
        this.estado = estado;
    }
}
