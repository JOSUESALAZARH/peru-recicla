
package com.perurecicla.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PERU RECICLA
 *
 * Entidad Usuario
 *
 * Tabla MySQL: Usuarios
 *
 * Compatible con Java 21,
 * Spring Boot 3.4.2,
 * JPA e Hibernate 6.
 */

@Entity
@Table(name = "Usuarios")
public class Usuario {

    // =====================================
    // IDENTIFICADOR DEL USUARIO
    // =====================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id_usuario;

    // =====================================
    // DNI - TIPO CHAR(8) EN MYSQL
    // =====================================

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "dni",
        nullable = false,
        unique = true,
        length = 8,
        columnDefinition = "CHAR(8)"
    )
    private String dni;

    // =====================================
    // DATOS PERSONALES
    // =====================================

    @Column(
        name = "nombre",
        nullable = false,
        length = 100
    )
    private String nombre;

    @Column(
        name = "apellido",
        nullable = false,
        length = 100
    )
    private String apellido;

    @Column(
        name = "correo",
        nullable = false,
        unique = true,
        length = 150
    )
    private String correo;

    @Column(
        name = "contrasena",
        nullable = false,
        length = 255
    )
    private String contrasena;

    @Column(
        name = "telefono",
        length = 15
    )
    private String telefono;

    // =====================================
    // ROLES DEL SISTEMA
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    public enum Rol {
        ROLE_vecino,
        ROLE_recolector,
        ROLE_administrador
    }

    // =====================================
    // PUNTOS ACUMULADOS
    // =====================================

    @Column(
        name = "puntos_acumulados",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal puntos_acumulados =
            BigDecimal.ZERO;

    // =====================================
    // FECHA DE REGISTRO
    // =====================================

    @Column(
        name = "fecha_registro",
        insertable = false,
        updatable = false
    )
    private LocalDateTime fecha_registro;

    // =====================================
    // ESTADO DEL USUARIO
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.activo;

    public enum Estado {
        activo,
        inactivo
    }

    // =====================================
    // CONSTRUCTOR VACIO
    // =====================================

    public Usuario() {
    }

    // =====================================
    // GETTERS Y SETTERS
    // =====================================

    public Integer getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(Integer id_usuario) {
        this.id_usuario = id_usuario;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public String getPassword() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public BigDecimal getPuntos_acumulados() {
        return puntos_acumulados;
    }

    public void setPuntos_acumulados(
            BigDecimal puntos_acumulados) {
        this.puntos_acumulados = puntos_acumulados;
    }

    public LocalDateTime getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(
            LocalDateTime fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
}
