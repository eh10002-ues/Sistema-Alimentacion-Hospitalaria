-- HU-03: ejecutar únicamente en el origen descartable creado por la prueba.
-- El hash se obtiene de la inicialización efímera; no hay contraseñas en este fixture.
INSERT INTO usuarios (nombre_usuario, contrasena_hash, nombre_completo, rol, activo,
    created_at, created_by, updated_at, updated_by)
SELECT 'hu03.medico', contrasena_hash, 'Médico sintético HU-03', 'ROLE_MEDICO', true,
    UTC_TIMESTAMP(6), 'hu03-prueba', UTC_TIMESTAMP(6), 'hu03-prueba'
FROM usuarios WHERE nombre_usuario = 'hu03.admin';
SET @medico = LAST_INSERT_ID();
INSERT INTO pisos (codigo, nombre) VALUES ('HU03', 'Piso sintético HU-03');
SET @piso = LAST_INSERT_ID();
INSERT INTO camas (piso_id, numero) VALUES (@piso, 'HU03-01');
SET @cama = LAST_INSERT_ID();
INSERT INTO pacientes (codigo, nombres, apellidos, fecha_nacimiento, peso_kg, altura_cm,
    estado, cama_id, medico_responsable_id, created_at, created_by, updated_at, updated_by)
VALUES ('HU03-PAC-001', 'Paciente sintético', 'Recuperación', '1990-01-01', 65, 170,
    'INGRESADO', @cama, @medico, UTC_TIMESTAMP(6), 'hu03-prueba', UTC_TIMESTAMP(6), 'hu03-prueba');
SET @paciente = LAST_INSERT_ID();
INSERT INTO paciente_enfermedades (paciente_id, nombre) VALUES (@paciente, 'Condición sintética');
INSERT INTO paciente_alergias (paciente_id, nombre, severidad) VALUES (@paciente, 'Alergia sintética', 'LEVE');
INSERT INTO paciente_medicamentos (paciente_id, nombre, dosis, frecuencia_horas, hora_primera_dosis)
VALUES (@paciente, 'Medicamento sintético', 'Dosis de prueba', 8, '06:00:00');
INSERT INTO insumos (nombre, unidad, merma, presentacion, cantidad_presentacion, costo_unitario)
VALUES ('Arroz sintético HU-03', 'g', 0.1000, 'Paquete sintético', 1000, 0.0020);
SET @insumo = LAST_INSERT_ID();
INSERT INTO menus (nombre, tipo, tipo_dieta, estado, created_at, created_by, updated_at, updated_by)
VALUES ('Menú sintético HU-03', 'GENERAL', 'NORMAL', 'ACTIVO', UTC_TIMESTAMP(6),
    'hu03-prueba', UTC_TIMESTAMP(6), 'hu03-prueba');
SET @menu = LAST_INSERT_ID();
INSERT INTO menu_detalles (menu_id, tiempo_comida, hora_servicio, platillo)
VALUES (@menu, 'ALMUERZO', '12:00:00', 'Platillo sintético');
SET @detalle = LAST_INSERT_ID();
INSERT INTO menu_detalle_insumos (menu_detalle_id, insumo_id, cantidad_por_racion)
VALUES (@detalle, @insumo, 80);
INSERT INTO asignaciones_menu (paciente_id, menu_id, asignado_por_id, fecha_inicio, fecha_fin,
    activa, aprobado_por_id, fecha_aprobacion, created_at, created_by, updated_at, updated_by)
VALUES (@paciente, @menu, @medico, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY),
    true, @medico, CURRENT_DATE, UTC_TIMESTAMP(6), 'hu03-prueba', UTC_TIMESTAMP(6), 'hu03-prueba');
INSERT INTO prescripciones_dieta (paciente_id, tipo_dieta, estado, autorizado_por_id,
    fecha_autorizacion, created_at, created_by, updated_at, updated_by)
VALUES (@paciente, 'NORMAL', 'APROBADO', @medico, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6),
    'hu03-prueba', UTC_TIMESTAMP(6), 'hu03-prueba');
INSERT INTO bitacora (accion, tabla_afectada, id_afectado, fecha_accion, usuario_id)
VALUES ('APROBAR', 'prescripciones_dieta', LAST_INSERT_ID(), UTC_TIMESTAMP(6), @medico);
