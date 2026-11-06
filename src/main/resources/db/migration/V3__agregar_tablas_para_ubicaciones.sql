-- Modulo ubicaciones: pisos, camas y encargado de piso por turno.
CREATE TABLE pisos (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       codigo VARCHAR(10) NOT NULL,
                       nombre VARCHAR(100) NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_pisos_codigo UNIQUE (codigo),
                       CONSTRAINT ck_pisos_codigo CHECK (CHAR_LENGTH(codigo) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE camas (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       piso_id BIGINT NOT NULL,
                       numero VARCHAR(10) NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_camas_piso_numero UNIQUE (piso_id, numero),
                       CONSTRAINT fk_camas_piso FOREIGN KEY (piso_id) REFERENCES pisos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

-- Un solo encargado por piso y turno. usuario_id debe tener rol ROLE_ENCARGADO_PISO
-- (se valida en el service).
CREATE TABLE asignaciones_piso (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   piso_id BIGINT NOT NULL,
                                   usuario_id BIGINT NOT NULL,
                                   turno VARCHAR(20) NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT uk_asig_piso_turno UNIQUE (piso_id, turno),
                                   CONSTRAINT fk_asig_piso_piso FOREIGN KEY (piso_id) REFERENCES pisos (id),
                                   CONSTRAINT fk_asig_piso_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
                                   CONSTRAINT ck_asig_piso_turno CHECK (turno IN ('MATUTINO', 'VESPERTINO', 'NOCTURNO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;