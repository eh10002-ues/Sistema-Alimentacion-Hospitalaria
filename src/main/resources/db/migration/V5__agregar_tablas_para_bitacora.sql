-- Modulo bitacora: registro de acciones sobre los datos del sistema.
CREATE TABLE bitacora (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          accion VARCHAR(50) NOT NULL,
                          tabla_afectada VARCHAR(60) NOT NULL,
                          id_afectado BIGINT NULL,
                          fecha_accion DATETIME(6) NOT NULL,
                          usuario_id BIGINT NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT fk_bitacora_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
                          INDEX idx_bitacora_registro (tabla_afectada, id_afectado),
                          INDEX idx_bitacora_fecha (fecha_accion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;