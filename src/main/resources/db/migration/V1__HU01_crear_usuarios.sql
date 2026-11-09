-- HU-01: persistencia mínima de usuarios; no incluye cuentas ni credenciales iniciales.
CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_usuario VARCHAR(100) COLLATE utf8mb4_0900_as_ci NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    rol VARCHAR(40) NOT NULL,
    activo BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT ck_usuarios_nombre_usuario CHECK (
        CHAR_LENGTH(nombre_usuario) > 0
        AND NOT REGEXP_LIKE(nombre_usuario, '[[:space:]]')
        AND CAST(nombre_usuario AS BINARY) = CAST(LOWER(nombre_usuario) AS BINARY)
    ),
    CONSTRAINT ck_usuarios_rol CHECK (
        CAST(rol AS BINARY) IN ('ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_NUTRICIONISTA',
                       'ROLE_ENCARGADO_PISO', 'ROLE_COCINA')
    ),
    CONSTRAINT ck_usuarios_activo CHECK (activo IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
