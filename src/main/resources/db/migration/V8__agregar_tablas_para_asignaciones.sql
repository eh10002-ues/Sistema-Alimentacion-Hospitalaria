-- Modulo asignaciones: menu asignado a un paciente.
-- Alimenta las consultas "pacientes por menu" y "menus por paciente".
-- aprobado_por_id debe tener rol ROLE_MEDICO o ROLE_NUTRICIONISTA (se valida en el service).
CREATE TABLE asignaciones_menu (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   paciente_id BIGINT NOT NULL,
                                   menu_id BIGINT NOT NULL,
                                   asignado_por_id BIGINT NOT NULL,
                                   fecha_inicio DATE NOT NULL,
                                   fecha_fin DATE NULL,
                                   activa BOOLEAN NOT NULL,
                                   aprobado_por_id BIGINT NULL,
                                   fecha_aprobacion DATE NULL,
                                   created_at DATETIME(6) NOT NULL,
                                   created_by VARCHAR(100) NOT NULL,
                                   updated_at DATETIME(6) NOT NULL,
                                   updated_by VARCHAR(100) NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_asig_menu_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
                                   CONSTRAINT fk_asig_menu_menu FOREIGN KEY (menu_id) REFERENCES menus (id),
                                   CONSTRAINT fk_asig_menu_asignador FOREIGN KEY (asignado_por_id) REFERENCES usuarios (id),
                                   CONSTRAINT fk_asig_menu_aprobador FOREIGN KEY (aprobado_por_id) REFERENCES usuarios (id),
                                   CONSTRAINT ck_asig_menu_activa CHECK (activa IN (0, 1)),
                                   CONSTRAINT ck_asig_menu_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
                                   CONSTRAINT ck_asig_menu_aprobacion CHECK (
                                       (aprobado_por_id IS NULL AND fecha_aprobacion IS NULL)
                                           OR (aprobado_por_id IS NOT NULL AND fecha_aprobacion IS NOT NULL)),
                                   INDEX idx_asig_menu_menu (menu_id, activa),
                                   INDEX idx_asig_menu_paciente (paciente_id, activa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;