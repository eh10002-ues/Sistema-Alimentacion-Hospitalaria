-- SAH-2: prescripción de dieta autorizada por un médico o nutricionista.
-- Cada autorización crea un registro nuevo: se conserva el historial del paciente.
-- tipo_dieta reutiliza los valores de menus.tipo_dieta (V7); en SAH-2 solo se prescriben
-- los regímenes corriente (NORMAL), blanda e hiposódica.
-- autorizado_por_id debe tener rol ROLE_MEDICO o ROLE_NUTRICIONISTA (se valida en el dominio).
CREATE TABLE prescripciones_dieta (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      paciente_id BIGINT NOT NULL,
                                      tipo_dieta VARCHAR(30) NOT NULL,
                                      estado VARCHAR(20) NOT NULL,
                                      autorizado_por_id BIGINT NOT NULL,
                                      fecha_autorizacion DATETIME(6) NOT NULL,
                                      created_at DATETIME(6) NOT NULL,
                                      created_by VARCHAR(100) NOT NULL,
                                      updated_at DATETIME(6) NOT NULL,
                                      updated_by VARCHAR(100) NOT NULL,
                                      PRIMARY KEY (id),
                                      CONSTRAINT fk_presc_dieta_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
                                      CONSTRAINT fk_presc_dieta_autorizador FOREIGN KEY (autorizado_por_id) REFERENCES usuarios (id),
                                      CONSTRAINT ck_presc_dieta_tipo CHECK (tipo_dieta IN ('NORMAL', 'BLANDA', 'HIPOSODICA')),
                                      CONSTRAINT ck_presc_dieta_estado CHECK (estado IN ('APROBADO')),
                                      INDEX idx_presc_dieta_paciente (paciente_id, fecha_autorizacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
