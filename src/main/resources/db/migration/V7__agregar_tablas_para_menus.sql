-- Modulo menus: menus, comidas de cada menu y gramaje de insumos por racion.
CREATE TABLE menus (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       nombre VARCHAR(150) NOT NULL,
                       tipo VARCHAR(20) NOT NULL,
                       tipo_dieta VARCHAR(30) NOT NULL,
                       estado VARCHAR(20) NOT NULL,
                       created_at DATETIME(6) NOT NULL,
                       created_by VARCHAR(100) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       updated_by VARCHAR(100) NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT ck_menus_nombre CHECK (CHAR_LENGTH(nombre) > 0),
                       CONSTRAINT ck_menus_tipo CHECK (tipo IN ('GENERAL', 'PERSONALIZADO')),
                       CONSTRAINT ck_menus_tipo_dieta CHECK (tipo_dieta IN
                                                             ('NORMAL', 'BLANDA', 'LIQUIDA', 'HIPOSODICA', 'HIPOCALORICA', 'PAPILLA')),
                       CONSTRAINT ck_menus_estado CHECK (estado IN ('BORRADOR', 'ACTIVO', 'INACTIVO')),
                       INDEX idx_menus_tipo_dieta (tipo_dieta, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

-- Una comida del menu (desayuno, almuerzo...) con su hora de servicio.
CREATE TABLE menu_detalles (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               menu_id BIGINT NOT NULL,
                               tiempo_comida VARCHAR(30) NOT NULL,
                               hora_servicio TIME NOT NULL,
                               platillo VARCHAR(150) NOT NULL,
                               PRIMARY KEY (id),
                               CONSTRAINT uk_menu_detalle UNIQUE (menu_id, tiempo_comida),
                               CONSTRAINT fk_menu_det_menu FOREIGN KEY (menu_id) REFERENCES menus (id),
                               CONSTRAINT ck_menu_det_tiempo CHECK (tiempo_comida IN
                                                                    ('DESAYUNO', 'COLACION_MATUTINA', 'ALMUERZO', 'COLACION_VESPERTINA', 'CENA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

-- Insumos y gramaje por racion de cada comida (base del calculo semanal/mensual).
CREATE TABLE menu_detalle_insumos (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      menu_detalle_id BIGINT NOT NULL,
                                      insumo_id BIGINT NOT NULL,
                                      cantidad_por_racion DECIMAL(10, 2) NOT NULL,
                                      PRIMARY KEY (id),
                                      CONSTRAINT uk_menu_det_insumo UNIQUE (menu_detalle_id, insumo_id),
                                      CONSTRAINT fk_mdi_detalle FOREIGN KEY (menu_detalle_id) REFERENCES menu_detalles (id),
                                      CONSTRAINT fk_mdi_insumo FOREIGN KEY (insumo_id) REFERENCES insumos (id),
                                      CONSTRAINT ck_mdi_cantidad CHECK (cantidad_por_racion > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;