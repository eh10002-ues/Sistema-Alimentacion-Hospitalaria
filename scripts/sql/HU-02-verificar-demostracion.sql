-- HU-02: comprobación de solo lectura de la carga sintética.
-- Para repetir la verificación con otra fecha: SET @hu02_fecha = 'AAAA-MM-DD';
SET NAMES utf8mb4;
SET @hu02_fecha = COALESCE(@hu02_fecha,
    DATE(DATE_SUB((SELECT MIN(created_at) FROM asignaciones_menu WHERE created_by = 'HU02-DEMO'), INTERVAL 6 HOUR)));
SELECT DATABASE() AS base_verificada, @hu02_fecha AS fecha_base_reportes;

SELECT estado, COUNT(*) AS pacientes_demo
FROM pacientes WHERE codigo LIKE 'HU02-D%' GROUP BY estado ORDER BY estado;

SELECT COUNT(*) AS asignaciones_demo_vigentes
FROM asignaciones_menu a JOIN pacientes p ON p.id = a.paciente_id JOIN menus m ON m.id = a.menu_id
WHERE a.created_by = 'HU02-DEMO' AND a.activa = 1 AND a.aprobado_por_id IS NOT NULL AND a.fecha_aprobacion IS NOT NULL
    AND p.estado = 'INGRESADO' AND m.estado = 'ACTIVO' AND a.fecha_inicio <= @hu02_fecha
    AND (a.fecha_fin IS NULL OR a.fecha_fin >= @hu02_fecha);

SELECT COUNT(*) AS filas_menus_programados_demo
FROM menu_detalles d JOIN menus m ON m.id = d.menu_id
WHERE m.nombre LIKE 'HU02 DEMO - %' AND EXISTS (
    SELECT 1 FROM asignaciones_menu a JOIN pacientes p ON p.id = a.paciente_id
    WHERE a.menu_id = m.id AND a.activa = 1 AND a.aprobado_por_id IS NOT NULL AND a.fecha_aprobacion IS NOT NULL
        AND p.estado = 'INGRESADO' AND m.estado = 'ACTIVO' AND a.fecha_inicio <= @hu02_fecha
        AND (a.fecha_fin IS NULL OR a.fecha_fin >= @hu02_fecha)
);

SELECT piso.codigo AS piso_demo, COUNT(*) AS camas,
    SUM(EXISTS(SELECT 1 FROM pacientes p WHERE p.cama_id = c.id AND p.estado = 'INGRESADO')) AS ocupadas,
    SUM(NOT EXISTS(SELECT 1 FROM pacientes p WHERE p.cama_id = c.id AND p.estado = 'INGRESADO')) AS disponibles,
    SUM((SELECT COUNT(*) FROM pacientes p WHERE p.cama_id = c.id AND p.estado = 'INGRESADO') > 1) AS inconsistentes
FROM camas c JOIN pisos piso ON piso.id = c.piso_id
WHERE piso.codigo IN ('HU02-D1','HU02-D2') GROUP BY piso.codigo ORDER BY piso.codigo;

SELECT p.codigo AS paciente_demo,
    CASE p.codigo WHEN 'HU02-D056' THEN 'Pendiente' WHEN 'HU02-D057' THEN 'Futura'
        WHEN 'HU02-D058' THEN 'Vencida' WHEN 'HU02-D059' THEN 'Inactiva' ELSE 'Menú inactivo' END AS caso,
    a.fecha_inicio, a.fecha_fin,
    IF(a.activa = 1 AND a.aprobado_por_id IS NOT NULL AND a.fecha_aprobacion IS NOT NULL
        AND p.estado = 'INGRESADO' AND m.estado = 'ACTIVO' AND a.fecha_inicio <= @hu02_fecha
        AND (a.fecha_fin IS NULL OR a.fecha_fin >= @hu02_fecha), 'SI', 'NO') AS aparece_en_fecha_base
FROM asignaciones_menu a JOIN pacientes p ON p.id = a.paciente_id JOIN menus m ON m.id = a.menu_id
WHERE p.codigo IN ('HU02-D056','HU02-D057','HU02-D058','HU02-D059','HU02-D060') ORDER BY p.codigo;

SELECT tipo_dieta, COUNT(*) AS menus_demo_activos_con_vigencia
FROM menus m WHERE m.nombre LIKE 'HU02 DEMO - %' AND EXISTS (
    SELECT 1 FROM asignaciones_menu a JOIN pacientes p ON p.id = a.paciente_id
    WHERE a.menu_id = m.id AND a.activa = 1 AND a.aprobado_por_id IS NOT NULL AND a.fecha_aprobacion IS NOT NULL
        AND p.estado = 'INGRESADO' AND m.estado = 'ACTIVO' AND a.fecha_inicio <= @hu02_fecha
        AND (a.fecha_fin IS NULL OR a.fecha_fin >= @hu02_fecha)
) GROUP BY tipo_dieta ORDER BY tipo_dieta;

SELECT 'pisos_previos_conservados' AS comprobacion, COUNT(*) AS cantidad FROM pisos WHERE codigo NOT IN ('HU02-D1','HU02-D2')
UNION ALL SELECT 'camas_previas_conservadas', COUNT(*) FROM camas c JOIN pisos p ON p.id = c.piso_id WHERE p.codigo NOT IN ('HU02-D1','HU02-D2')
UNION ALL SELECT 'usuarios_sin_modificar', COUNT(*) FROM usuarios;
