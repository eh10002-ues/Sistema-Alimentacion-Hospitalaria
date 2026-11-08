-- HU-02: datos exclusivamente sintéticos para reportes en el Compose local.
-- No ejecutar en una base compartida o productiva.
-- Requiere establecer en la MISMA conexión:
--   SET @hu02_base_confirmada = DATABASE();
--   SET @hu02_demo_confirmacion = 'CARGAR DEMO HU02 LOCAL';
-- No usar mysql --force: cualquier error debe cerrar la conexión y revertir la transacción.
-- Una segunda ejecución se rechaza: no se sobrescriben ni se duplican registros.
-- Los usuarios existentes se referencian, sin modificar sus cuentas o contraseñas.

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET @hu02_fecha = DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '-06:00'));
SET @hu02_medico = (SELECT MIN(id) FROM usuarios WHERE rol = 'ROLE_MEDICO' AND activo = 1);
SET @hu02_nutricionista = (SELECT MIN(id) FROM usuarios WHERE rol = 'ROLE_NUTRICIONISTA' AND activo = 1);

CREATE TEMPORARY TABLE hu02_demo_numeros (n INT NOT NULL PRIMARY KEY);
INSERT INTO hu02_demo_numeros (n)
WITH RECURSIVE numeros AS (
    SELECT 1 AS n
    UNION ALL SELECT n + 1 FROM numeros WHERE n < 80
) SELECT n FROM numeros;

CREATE TEMPORARY TABLE hu02_demo_control (
    correcto TINYINT NOT NULL,
    CONSTRAINT ck_hu02_demo_precondiciones_y_conteos CHECK (correcto = 1)
);

START TRANSACTION;

-- Comprobar autorización explícita, base, actores válidos y espacio de nombres libre.
INSERT INTO hu02_demo_control (correcto)
SELECT IF(
    @hu02_demo_confirmacion = 'CARGAR DEMO HU02 LOCAL'
    AND @hu02_base_confirmada = DATABASE()
    AND @hu02_medico IS NOT NULL AND @hu02_nutricionista IS NOT NULL
    AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()
        AND table_name IN ('pisos','camas','pacientes','menus','menu_detalles','asignaciones_menu')
        AND engine = 'InnoDB') = 6
    AND EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '11' AND success = 1)
    AND NOT EXISTS (SELECT 1 FROM pisos WHERE codigo IN ('HU02-D1', 'HU02-D2'))
    AND NOT EXISTS (SELECT 1 FROM pacientes WHERE codigo LIKE 'HU02-D%')
    AND NOT EXISTS (SELECT 1 FROM menus WHERE nombre LIKE 'HU02 DEMO - %'), 1, 0);

INSERT INTO pisos (codigo, nombre) VALUES
    ('HU02-D1', 'Pruebas HU02 - Piso 1 (sintético)'),
    ('HU02-D2', 'Pruebas HU02 - Piso 2 (sintético)');

INSERT INTO camas (piso_id, numero)
SELECT piso.id, CONCAT('D', LPAD(n.n, 3, '0'))
FROM hu02_demo_numeros n
JOIN pisos piso ON piso.codigo = IF(n.n <= 40, 'HU02-D1', 'HU02-D2')
ORDER BY n.n;

-- 001-060 ingresados; 061-065 altas con cama histórica; 066-068 altas sin cama.
INSERT INTO pacientes (codigo, nombres, apellidos, fecha_nacimiento, peso_kg, altura_cm,
    estado, cama_id, medico_responsable_id, created_at, created_by, updated_at, updated_by)
SELECT CONCAT('HU02-D', LPAD(n.n, 3, '0')),
    CONCAT('Paciente sintético ', LPAD(n.n, 3, '0')), 'Reportes HU02 DEMO',
    '1990-01-01', 55 + MOD(n.n, 35), 150 + MOD(n.n, 35),
    IF(n.n <= 60, 'INGRESADO', 'ALTA'), IF(n.n <= 65, cama.id, NULL), @hu02_medico,
    UTC_TIMESTAMP(6), 'HU02-DEMO', UTC_TIMESTAMP(6), 'HU02-DEMO'
FROM hu02_demo_numeros n
LEFT JOIN camas cama ON cama.numero = CONCAT('D', LPAD(IF(n.n BETWEEN 61 AND 65, n.n - 60, n.n), 3, '0'))
    AND cama.piso_id IN (SELECT id FROM pisos WHERE codigo IN ('HU02-D1', 'HU02-D2'))
WHERE n.n <= 68
ORDER BY n.n;

-- 01-30 activos con asignaciones vigentes; 31 inactivo; 32 borrador;
-- 33 activo sin asignaciones (no debe aparecer en menús programados).
INSERT INTO menus (nombre, tipo, tipo_dieta, estado, created_at, created_by, updated_at, updated_by)
SELECT CONCAT('HU02 DEMO - Menú ', LPAD(n.n, 2, '0')), 'GENERAL',
    CASE MOD(n.n - 1, 6) WHEN 0 THEN 'NORMAL' WHEN 1 THEN 'BLANDA' WHEN 2 THEN 'LIQUIDA'
        WHEN 3 THEN 'HIPOSODICA' WHEN 4 THEN 'HIPOCALORICA' ELSE 'PAPILLA' END,
    CASE n.n WHEN 31 THEN 'INACTIVO' WHEN 32 THEN 'BORRADOR' ELSE 'ACTIVO' END,
    UTC_TIMESTAMP(6), 'HU02-DEMO', UTC_TIMESTAMP(6), 'HU02-DEMO'
FROM hu02_demo_numeros n WHERE n.n <= 33 ORDER BY n.n;

-- Cinco tiempos por menú; horas de reloj para la aplicación Compose con JVM UTC.
INSERT INTO menu_detalles (menu_id, tiempo_comida, hora_servicio, platillo)
SELECT menu.id, tiempo.nombre, tiempo.hora,
    CONCAT('Platillo sintético HU02 - ', tiempo.etiqueta)
FROM menus menu
CROSS JOIN (
    SELECT 'DESAYUNO' AS nombre, CAST('07:00:00' AS TIME) AS hora, 'desayuno' AS etiqueta
    UNION ALL SELECT 'COLACION_MATUTINA', '10:00:00', 'colación matutina'
    UNION ALL SELECT 'ALMUERZO', '12:00:00', 'almuerzo'
    UNION ALL SELECT 'COLACION_VESPERTINA', '15:00:00', 'colación vespertina'
    UNION ALL SELECT 'CENA', '18:00:00', 'cena'
) tiempo
WHERE menu.nombre LIKE 'HU02 DEMO - %'
ORDER BY menu.id, tiempo.hora;

-- 55 asignaciones operativas vigentes: algunas sin fin y otras con fin futuro.
INSERT INTO asignaciones_menu (paciente_id, menu_id, asignado_por_id, fecha_inicio, fecha_fin,
    activa, aprobado_por_id, fecha_aprobacion, created_at, created_by, updated_at, updated_by)
SELECT paciente.id, menu.id, @hu02_medico, DATE_SUB(@hu02_fecha, INTERVAL 7 DAY),
    IF(MOD(n.n, 2) = 0, DATE_ADD(@hu02_fecha, INTERVAL 30 DAY), NULL),
    1, @hu02_nutricionista, DATE_SUB(@hu02_fecha, INTERVAL 7 DAY),
    UTC_TIMESTAMP(6), 'HU02-DEMO', UTC_TIMESTAMP(6), 'HU02-DEMO'
FROM hu02_demo_numeros n
JOIN pacientes paciente ON paciente.codigo = CONCAT('HU02-D', LPAD(n.n, 3, '0'))
JOIN menus menu ON menu.nombre = CONCAT('HU02 DEMO - Menú ', LPAD(MOD(n.n - 1, 30) + 1, 2, '0'))
WHERE n.n <= 55 ORDER BY n.n;

-- 056 pendiente, 057 futura, 058 vencida, 059 inactiva, 060 con menú inactivo.
-- Ninguna debe figurar como asignación operativa vigente en la fecha de carga.
INSERT INTO asignaciones_menu (paciente_id, menu_id, asignado_por_id, fecha_inicio, fecha_fin,
    activa, aprobado_por_id, fecha_aprobacion, created_at, created_by, updated_at, updated_by)
SELECT paciente.id, menu.id, @hu02_medico,
    CASE n.n WHEN 57 THEN DATE_ADD(@hu02_fecha, INTERVAL 7 DAY)
        WHEN 58 THEN DATE_SUB(@hu02_fecha, INTERVAL 30 DAY) ELSE DATE_SUB(@hu02_fecha, INTERVAL 5 DAY) END,
    IF(n.n = 58, DATE_SUB(@hu02_fecha, INTERVAL 1 DAY), NULL),
    IF(n.n = 59, 0, 1), IF(n.n = 56, NULL, @hu02_nutricionista),
    IF(n.n = 56, NULL, DATE_SUB(@hu02_fecha, INTERVAL 5 DAY)),
    UTC_TIMESTAMP(6), 'HU02-DEMO', UTC_TIMESTAMP(6), 'HU02-DEMO'
FROM hu02_demo_numeros n
JOIN pacientes paciente ON paciente.codigo = CONCAT('HU02-D', LPAD(n.n, 3, '0'))
JOIN menus menu ON menu.nombre = CONCAT('HU02 DEMO - Menú ', LPAD(IF(n.n = 60, 31, n.n - 30), 2, '0'))
WHERE n.n BETWEEN 56 AND 60 ORDER BY n.n;

-- Afirmaciones dentro de la transacción: ante un error de conteo no se confirma la carga.
INSERT INTO hu02_demo_control (correcto)
SELECT IF(
    (SELECT COUNT(*) FROM pisos WHERE codigo IN ('HU02-D1', 'HU02-D2')) = 2
    AND (SELECT COUNT(*) FROM camas c JOIN pisos p ON p.id = c.piso_id WHERE p.codigo IN ('HU02-D1', 'HU02-D2')) = 80
    AND (SELECT COUNT(*) FROM pacientes WHERE codigo LIKE 'HU02-D%') = 68
    AND (SELECT COUNT(*) FROM pacientes WHERE codigo LIKE 'HU02-D%' AND estado = 'INGRESADO' AND cama_id IS NOT NULL) = 60
    AND (SELECT COUNT(*) FROM pacientes WHERE codigo LIKE 'HU02-D%' AND estado = 'ALTA') = 8
    AND (SELECT COUNT(*) FROM menus WHERE nombre LIKE 'HU02 DEMO - %') = 33
    AND (SELECT COUNT(*) FROM menu_detalles d JOIN menus m ON m.id = d.menu_id WHERE m.nombre LIKE 'HU02 DEMO - %') = 165
    AND (SELECT COUNT(*) FROM asignaciones_menu WHERE created_by = 'HU02-DEMO') = 60
    AND (SELECT COUNT(*) FROM asignaciones_menu a JOIN pacientes p ON p.id = a.paciente_id JOIN menus m ON m.id = a.menu_id
        WHERE a.created_by = 'HU02-DEMO' AND a.activa = 1 AND a.aprobado_por_id IS NOT NULL AND a.fecha_aprobacion IS NOT NULL
        AND p.estado = 'INGRESADO' AND m.estado = 'ACTIVO' AND a.fecha_inicio <= @hu02_fecha
        AND (a.fecha_fin IS NULL OR a.fecha_fin >= @hu02_fecha)) = 55
    AND NOT EXISTS (
        SELECT p.cama_id FROM pacientes p JOIN camas c ON c.id = p.cama_id JOIN pisos piso ON piso.id = c.piso_id
        WHERE piso.codigo IN ('HU02-D1', 'HU02-D2') AND p.estado = 'INGRESADO'
        GROUP BY p.cama_id HAVING COUNT(*) > 1
    ), 1, 0);

COMMIT;

SELECT @hu02_fecha AS fecha_base_reportes, DATABASE() AS base_destino;
SELECT 'pisos_demo' AS conjunto, COUNT(*) AS cantidad FROM pisos WHERE codigo IN ('HU02-D1', 'HU02-D2')
UNION ALL SELECT 'camas_demo', COUNT(*) FROM camas c JOIN pisos p ON p.id = c.piso_id WHERE p.codigo IN ('HU02-D1', 'HU02-D2')
UNION ALL SELECT 'pacientes_demo', COUNT(*) FROM pacientes WHERE codigo LIKE 'HU02-D%'
UNION ALL SELECT 'menus_demo', COUNT(*) FROM menus WHERE nombre LIKE 'HU02 DEMO - %'
UNION ALL SELECT 'detalles_menu_demo', COUNT(*) FROM menu_detalles d JOIN menus m ON m.id = d.menu_id WHERE m.nombre LIKE 'HU02 DEMO - %'
UNION ALL SELECT 'asignaciones_demo', COUNT(*) FROM asignaciones_menu WHERE created_by = 'HU02-DEMO';
