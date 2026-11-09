-- Modulo insumos: catalogo de insumos alimentarios.
-- merma es un porcentaje (0 a 100). costo_unitario es por unidad base.
CREATE TABLE insumos (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         nombre VARCHAR(150) NOT NULL,
                         unidad VARCHAR(10) NOT NULL,
                         merma DECIMAL(5, 2) NOT NULL DEFAULT 0,
                         presentacion VARCHAR(50) NULL,
                         cantidad_presentacion DECIMAL(10, 2) NULL,
                         costo_unitario DECIMAL(10, 4) NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT uk_insumos_nombre UNIQUE (nombre),
                         CONSTRAINT ck_insumos_nombre CHECK (CHAR_LENGTH(nombre) > 0),
                         CONSTRAINT ck_insumos_merma CHECK (merma BETWEEN 0 AND 100),
                         CONSTRAINT ck_insumos_cantidad_presentacion CHECK (cantidad_presentacion IS NULL OR cantidad_presentacion >= 0),
                         CONSTRAINT ck_insumos_costo CHECK (costo_unitario IS NULL OR costo_unitario >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;