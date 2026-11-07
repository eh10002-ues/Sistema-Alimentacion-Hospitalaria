ALTER TABLE pacientes DROP FOREIGN KEY fk_pacientes_cama;
ALTER TABLE pacientes DROP INDEX uk_pacientes_cama;
ALTER TABLE pacientes ADD INDEX idx_pacientes_cama_id (cama_id);
ALTER TABLE pacientes ADD CONSTRAINT fk_pacientes_cama
    FOREIGN KEY (cama_id) REFERENCES camas (id);
