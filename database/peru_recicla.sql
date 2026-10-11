
-- ============================================================
--                 PERU RECICLA
--          BASE DE DATOS - MYSQL 8
-- ============================================================
--
-- Proyecto:
-- Sistema Web de Incentivos para el Reciclaje
--
-- Tecnologias:
-- Java 21
-- Spring Boot
-- Spring Data JPA
-- Hibernate
-- MySQL 8
-- Thymeleaf
--
-- Archivo:
-- database/peru_recicla.sql
--
-- CONTENIDO:
-- 01. Creacion de base de datos
-- 02. Tabla Usuarios
-- 03. Tabla Materiales
-- 04. Tabla Recolectas
-- 05. Tabla Recompensas
-- 06. Tabla Canjes
-- 07. Datos de demostracion - Materiales
-- 08. Datos de demostracion - Recompensas
-- 09. Consultas de verificacion
--
-- IMPORTANTE:
-- Este script se basa en la estructura conocida
-- de PERU RECICLA. Debe verificarse contra
-- la base de datos actual antes de utilizarlo
-- como instalador definitivo.
--
-- No incluye usuarios ni contrasenas reales.
-- No contiene instrucciones DROP.
--
-- ============================================================


-- ============================================================
-- 01. CREACION DE LA BASE DE DATOS
-- ============================================================

CREATE DATABASE IF NOT EXISTS PeruRecicla
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE PeruRecicla;


-- ============================================================
-- 02. TABLA USUARIOS
-- ============================================================
--
-- Almacena:
-- Vecinos
-- Recolectores
-- Administradores
--
-- Los puntos acumulados corresponden
-- al saldo del usuario.
--
-- Las contrasenas deben ser generadas
-- y almacenadas con BCrypt desde Java.
-- ============================================================

CREATE TABLE IF NOT EXISTS Usuarios (

    id_usuario INT AUTO_INCREMENT PRIMARY KEY,

    dni CHAR(8) NOT NULL UNIQUE,

    nombre VARCHAR(100) NOT NULL,

    apellido VARCHAR(100) NOT NULL,

    correo VARCHAR(150) NOT NULL UNIQUE,

    contrasena VARCHAR(255) NOT NULL,

    telefono VARCHAR(15),

    rol ENUM(
        'ROLE_vecino',
        'ROLE_recolector',
        'ROLE_administrador'
    ) NOT NULL,

    puntos_acumulados DECIMAL(10,2)
        NOT NULL DEFAULT 0.00,

    fecha_registro TIMESTAMP
        DEFAULT CURRENT_TIMESTAMP,

    estado ENUM(
        'activo',
        'inactivo'
    ) DEFAULT 'activo',

    CONSTRAINT chk_usuario_puntos
        CHECK (puntos_acumulados >= 0)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 03. TABLA MATERIALES
-- ============================================================
--
-- Registra los materiales reciclables
-- y la cantidad de puntos por gramo.
--
-- Ejemplo:
-- 500 gramos de aluminio
-- 0.0800 puntos por gramo
--
-- Total = 40 puntos
-- ============================================================

CREATE TABLE IF NOT EXISTS Materiales (

    id_material INT AUTO_INCREMENT PRIMARY KEY,

    tipo_material VARCHAR(50) NOT NULL,

    puntos_por_gramo DECIMAL(10,4) NOT NULL,

    url_imagen VARCHAR(255),

    descripcion TEXT,

    CONSTRAINT chk_material_puntos
        CHECK (puntos_por_gramo >= 0)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 04. TABLA RECOLECTAS
-- ============================================================
--
-- Registra cada entrega de material.
--
-- Relaciona:
-- Vecino
-- Recolector
-- Material
--
-- Estados:
-- pendiente
-- validado
-- rechazado
--
-- La acreditacion de puntos se realiza
-- desde el servicio Java cuando
-- el administrador valida la recolecta.
-- ============================================================

CREATE TABLE IF NOT EXISTS Recolectas (

    id_recolecta INT AUTO_INCREMENT PRIMARY KEY,

    -- Usuario que entrega el material
    id_usuario INT NOT NULL,

    -- Recolector que registra el pesaje
    id_recolector INT NOT NULL,

    -- Material entregado
    id_material INT NOT NULL,

    peso_gramos DECIMAL(10,2) NOT NULL,

    puntos_generados DECIMAL(10,2) NOT NULL,

    fecha_recolecta TIMESTAMP
        DEFAULT CURRENT_TIMESTAMP,

    estado_validacion ENUM(
        'pendiente',
        'validado',
        'rechazado'
    ) DEFAULT 'pendiente',

    -- RELACIONES

    CONSTRAINT fk_recolecta_vecino
        FOREIGN KEY (id_usuario)
        REFERENCES Usuarios(id_usuario),

    CONSTRAINT fk_recolecta_recolector
        FOREIGN KEY (id_recolector)
        REFERENCES Usuarios(id_usuario),

    CONSTRAINT fk_recolecta_material
        FOREIGN KEY (id_material)
        REFERENCES Materiales(id_material),

    -- VALIDACIONES

    CONSTRAINT chk_recolecta_peso
        CHECK (peso_gramos > 0),

    CONSTRAINT chk_recolecta_puntos
        CHECK (puntos_generados >= 0)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 05. TABLA RECOMPENSAS
-- ============================================================
--
-- Contiene los productos del catalogo.
--
-- Ejemplos:
-- Aceite
-- Detergente
-- Fideos
-- Vales de descuento
--
-- El administrador podra registrar
-- y modificar estos productos desde
-- el modulo de gestion de recompensas.
--
-- El campo url_imagen almacenara la
-- ubicacion de la fotografia.
-- ============================================================

CREATE TABLE IF NOT EXISTS Recompensas (

    id_recompensa INT AUTO_INCREMENT PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL,

    categoria ENUM(
        'alimentos',
        'hogar',
        'limpieza',
        'tecnologia'
    ) NOT NULL,

    costo_puntos INT NOT NULL,

    stock INT NOT NULL DEFAULT 0,

    url_imagen VARCHAR(255),

    descripcion TEXT,

    estado ENUM(
        'activo',
        'inactivo'
    ) DEFAULT 'activo',

    -- VALIDACIONES

    CONSTRAINT chk_recompensa_costo
        CHECK (costo_puntos >= 0),

    CONSTRAINT chk_recompensa_stock
        CHECK (stock >= 0)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 06. TABLA CANJES
-- ============================================================
--
-- Registra las recompensas canjeadas
-- por los vecinos.
--
-- Relaciona:
-- Usuario
-- Recompensa
--
-- Estados:
-- pendiente
-- entregado
-- cancelado
--
-- El codigo_ticket identifica
-- cada operacion de canje.
-- ============================================================

CREATE TABLE IF NOT EXISTS Canjes (

    id_canje INT AUTO_INCREMENT PRIMARY KEY,

    id_usuario INT NOT NULL,

    id_recompensa INT NOT NULL,

    puntos_descontados INT NOT NULL,

    codigo_ticket VARCHAR(20)
        NOT NULL UNIQUE,

    fecha_canje TIMESTAMP
        DEFAULT CURRENT_TIMESTAMP,

    estado ENUM(
        'pendiente',
        'entregado',
        'cancelado'
    ) DEFAULT 'pendiente',

    -- RELACIONES

    CONSTRAINT fk_canje_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES Usuarios(id_usuario),

    CONSTRAINT fk_canje_recompensa
        FOREIGN KEY (id_recompensa)
        REFERENCES Recompensas(id_recompensa),

    -- VALIDACIONES

    CONSTRAINT chk_canje_puntos
        CHECK (puntos_descontados >= 0)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 07. DATOS DE DEMOSTRACION
--                  MATERIALES
-- ============================================================
--
-- Estos valores provienen de los
-- ejemplos iniciales del proyecto.
--
-- Se insertan solamente cuando
-- el nombre del material no existe.
--
-- No se modifican los materiales
-- que ya esten registrados.
-- ============================================================


-- ------------------------------------------------------------
-- MATERIAL 01 - PLASTICO PET
-- ------------------------------------------------------------

INSERT INTO Materiales (
    tipo_material,
    puntos_por_gramo,
    descripcion
)
SELECT
    'Plástico PET',
    0.0500,
    'Botellas de gaseosa y agua'
WHERE NOT EXISTS (
    SELECT 1
    FROM Materiales
    WHERE tipo_material = 'Plástico PET'
);


-- ------------------------------------------------------------
-- MATERIAL 02 - PAPEL BLANCO
-- ------------------------------------------------------------

INSERT INTO Materiales (
    tipo_material,
    puntos_por_gramo,
    descripcion
)
SELECT
    'Papel Blanco',
    0.0300,
    'Hojas bond, documentos y cuadernos'
WHERE NOT EXISTS (
    SELECT 1
    FROM Materiales
    WHERE tipo_material = 'Papel Blanco'
);


-- ------------------------------------------------------------
-- MATERIAL 03 - CARTON CORRUGADO
-- ------------------------------------------------------------

INSERT INTO Materiales (
    tipo_material,
    puntos_por_gramo,
    descripcion
)
SELECT
    'Cartón Corrugado',
    0.0400,
    'Cajas de embalaje de carton'
WHERE NOT EXISTS (
    SELECT 1
    FROM Materiales
    WHERE tipo_material = 'Cartón Corrugado'
);


-- ------------------------------------------------------------
-- MATERIAL 04 - LATAS DE ALUMINIO
-- ------------------------------------------------------------

INSERT INTO Materiales (
    tipo_material,
    puntos_por_gramo,
    descripcion
)
SELECT
    'Latas de Aluminio',
    0.0800,
    'Latas de bebidas de aluminio'
WHERE NOT EXISTS (
    SELECT 1
    FROM Materiales
    WHERE tipo_material = 'Latas de Aluminio'
);


-- ============================================================
-- 08. DATOS DE DEMOSTRACION
--                 RECOMPENSAS
-- ============================================================
--
-- Los productos y existencias son ejemplos
-- para probar el catalogo de recompensas.
--
-- Las imagenes se dejan en NULL hasta
-- implementar la carga de fotografias.
--
-- Los registros no se duplicaran si
-- ya existe una recompensa con igual nombre.
-- ============================================================


-- ------------------------------------------------------------
-- RECOMPENSA 01 - ACEITE PRIMOR 1L
-- ------------------------------------------------------------

INSERT INTO Recompensas (
    nombre,
    categoria,
    costo_puntos,
    stock,
    url_imagen,
    descripcion,
    estado
)
SELECT
    'Aceite Primor 1L',
    'alimentos',
    150,
    20,
    NULL,
    'Aceite vegetal de un litro.',
    'activo'
WHERE NOT EXISTS (
    SELECT 1
    FROM Recompensas
    WHERE nombre = 'Aceite Primor 1L'
);


-- ------------------------------------------------------------
-- RECOMPENSA 02 - DETERGENTE ARIEL 1KG
-- ------------------------------------------------------------

INSERT INTO Recompensas (
    nombre,
    categoria,
    costo_puntos,
    stock,
    url_imagen,
    descripcion,
    estado
)
SELECT
    'Detergente Ariel 1kg',
    'limpieza',
    120,
    15,
    NULL,
    'Detergente para lavado de ropa.',
    'activo'
WHERE NOT EXISTS (
    SELECT 1
    FROM Recompensas
    WHERE nombre = 'Detergente Ariel 1kg'
);


-- ------------------------------------------------------------
-- RECOMPENSA 03 - VALE DE DESCUENTO
-- ------------------------------------------------------------

INSERT INTO Recompensas (
    nombre,
    categoria,
    costo_puntos,
    stock,
    url_imagen,
    descripcion,
    estado
)
SELECT
    'Vale de Descuento S/10',
    'hogar',
    200,
    50,
    NULL,
    'Vale de descuento promocional de demostracion.',
    'activo'
WHERE NOT EXISTS (
    SELECT 1
    FROM Recompensas
    WHERE nombre = 'Vale de Descuento S/10'
);


-- ------------------------------------------------------------
-- RECOMPENSA 04 - FIDEOS DON VITTORIO
-- ------------------------------------------------------------

INSERT INTO Recompensas (
    nombre,
    categoria,
    costo_puntos,
    stock,
    url_imagen,
    descripcion,
    estado
)
SELECT
    'Fideos Don Vittorio',
    'alimentos',
    80,
    30,
    NULL,
    'Paquete de fideos de demostracion.',
    'activo'
WHERE NOT EXISTS (
    SELECT 1
    FROM Recompensas
    WHERE nombre = 'Fideos Don Vittorio'
);


-- ============================================================
-- 09. CONSULTAS DE VERIFICACION
-- ============================================================
--
-- Permiten comprobar las tablas
-- y sus cantidades de registros.
-- ============================================================

SELECT
    'Usuarios' AS tabla,
    COUNT(*) AS registros
FROM Usuarios

UNION ALL

SELECT
    'Materiales',
    COUNT(*)
FROM Materiales

UNION ALL

SELECT
    'Recolectas',
    COUNT(*)
FROM Recolectas

UNION ALL

SELECT
    'Recompensas',
    COUNT(*)
FROM Recompensas

UNION ALL

SELECT
    'Canjes',
    COUNT(*)
FROM Canjes;


-- ============================================================
-- MOSTRAR MATERIALES
-- ============================================================

SELECT
    id_material,
    tipo_material,
    puntos_por_gramo,
    descripcion
FROM Materiales
ORDER BY id_material;


-- ============================================================
-- MOSTRAR RECOMPENSAS
-- ============================================================

SELECT
    id_recompensa,
    nombre,
    categoria,
    costo_puntos,
    stock,
    estado
FROM Recompensas
ORDER BY id_recompensa;


-- ============================================================
-- FIN DEL SCRIPT
-- PERU RECICLA
-- ============================================================
