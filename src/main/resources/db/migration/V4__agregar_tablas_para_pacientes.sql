-- Modulo pacientes: pacientes y sus datos clinicos.
-- Depende de V3 (camas) y de usuarios (medico responsable).
CREATE TABLE pacientes (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           codigo VARCHAR(20) NOT NULL,
                           nombres VARCHAR(100) NOT NULL,
                           apellidos VARCHAR(100) NOT NULL,
                           fecha_nacimiento DATE NOT NULL,
                           peso_kg DECIMAL(5, 2) NOT NULL,
                           altura_cm DECIMAL(5, 2) NOT NULL,
                           estado VARCHAR(20) NOT NULL,
                           cama_id BIGINT NULL,
                           medico_responsable_id BIGINT NULL,
                           created_at DATETIME(6) NOT NULL,
                           created_by VARCHAR(100) NOT NULL,
                           updated_at DATETIME(6) NOT NULL,
                           updated_by VARCHAR(100) NOT NULL,
                           PRIMARY KEY (id),
                           CONSTRAINT uk_pacientes_codigo UNIQUE (codigo),
                           CONSTRAINT uk_pacientes_cama UNIQUE (cama_id),
                           CONSTRAINT fk_pacientes_cama FOREIGN KEY (cama_id) REFERENCES camas (id),
                           CONSTRAINT fk_pacientes_medico FOREIGN KEY (medico_responsable_id) REFERENCES usuarios (id),
                           CONSTRAINT ck_pacientes_codigo CHECK (CHAR_LENGTH(codigo) > 0),
                           CONSTRAINT ck_pacientes_peso CHECK (peso_kg > 0),
                           CONSTRAINT ck_pacientes_estado CHECK (estado IN ('INGRESADO', 'ALTA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE paciente_enfermedades (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       paciente_id BIGINT NOT NULL,
                                       nombre VARCHAR(150) NOT NULL,
                                       PRIMARY KEY (id),
                                       CONSTRAINT fk_pac_enf_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE paciente_alergias (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   paciente_id BIGINT NOT NULL,
                                   nombre VARCHAR(100) NOT NULL,
                                   severidad VARCHAR(20) NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_pac_ale_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
                                   CONSTRAINT ck_pac_ale_severidad CHECK (severidad IN ('LEVE', 'MODERADA', 'SEVERA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE paciente_medicamentos (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       paciente_id BIGINT NOT NULL,
                                       nombre VARCHAR(150) NOT NULL,
                                       dosis VARCHAR(50) NOT NULL,
                                       frecuencia_horas SMALLINT NOT NULL,
                                       hora_primera_dosis TIME NOT NULL,
                                       PRIMARY KEY (id),
                                       CONSTRAINT fk_pac_med_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
                                       CONSTRAINT ck_pac_med_frecuencia CHECK (frecuencia_horas > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;