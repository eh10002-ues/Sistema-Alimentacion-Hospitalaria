-- Serializa las comprobaciones y cambios de administradores, incluso con solicitudes simultáneas.
CREATE TABLE administracion_usuarios_lock (
    id TINYINT NOT NULL PRIMARY KEY,
    CONSTRAINT ck_administracion_usuarios_lock CHECK (id = 1)
) ENGINE=InnoDB;
INSERT INTO administracion_usuarios_lock (id) VALUES (1);
