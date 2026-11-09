# HU-02 — Datos sintéticos cargados para probar reportes

## Entorno y resultado observado

El 8 de noviembre de 2026 se ejecutó
[HU-02-datos-demostracion.sql](../scripts/sql/HU-02-datos-demostracion.sql)
mediante el cliente MySQL sobre `alimentacion_hospitalaria` del servicio `db`
del Compose local. La aplicación local se encontró publicada en
`http://localhost:8081` y la migración V11 estaba aplicada.

La carga se confirmó en una transacción, con comprobaciones de autorización,
prefijos libres, usuarios activos, motor InnoDB y conteos. No modificó los
registros previos ni creó o alteró cuentas. Usó el médico y el nutricionista
activos existentes como responsables de asignación y aprobación.

| Datos nuevos | Cantidad |
| --- | ---: |
| Pisos | 2 |
| Camas | 80 |
| Pacientes | 68 |
| Menús | 33 |
| Detalles o tiempos de comida | 165 |
| Asignaciones de menú | 60 |

Se conservaron los dos pisos, las tres camas y las cinco cuentas que existían
antes de la carga. No se agregaron medicamentos, alergias ni enfermedades.
Los nombres y datos personales insertados son exclusivamente sintéticos.

## Cómo identificarlos en la aplicación

- Pisos: **HU02-D1** y **HU02-D2**, con nombres que indican pruebas sintéticas.
- Pacientes: códigos **HU02-D001** a **HU02-D068**; se pueden buscar con `HU02-D`.
- Menús: nombres **HU02 DEMO - Menú 01** a **HU02 DEMO - Menú 33**.
- Camas: **D001** a **D080**, dentro de los dos pisos de demostración.

## Resultados esperados para el 08/11/2026

Estos conteos se comprobaron ejecutando
[HU-02-verificar-demostracion.sql](../scripts/sql/HU-02-verificar-demostracion.sql),
no mediante pruebas de navegador o PDF.

| Reporte o filtro | Resultado esperado de la carga |
| --- | --- |
| Pacientes, búsqueda `HU02-D` | 68: 60 ingresados y 8 de alta. |
| Asignaciones, fecha 08/11/2026 y búsqueda `HU02-D` | 55 vigentes. |
| Menús, fecha 08/11/2026 sin filtros adicionales | 150 filas: 30 menús con cinco tiempos cada uno. |
| Menús, un tiempo de comida, misma fecha | 30 filas. |
| Menús, un tipo de dieta, misma fecha | 25 filas: cinco menús y cinco tiempos por menú. |
| Ocupación, piso HU02-D1 | 40 camas, todas ocupadas. |
| Ocupación, piso HU02-D2 | 40 camas: 20 ocupadas y 20 disponibles. |

Si no se filtra por piso, el reporte de ocupación también incluye las tres camas
previas: el total observado tras la carga es 83 camas, 60 ocupadas y 23 disponibles.
No se introdujeron ocupaciones inconsistentes.

Los reportes con más de 20 coincidencias permiten probar paginación y una
impresión que incluya varias páginas. Las fechas y estados se evalúan con las
reglas actuales; para reproducir exactamente estos resultados usar la fecha
indicada, aunque la fecha actual del sistema sea otra. Ediciones posteriores de
los registros pueden cambiar los conteos.

## Casos especiales

| Paciente | Caso insertado | Resultado el 08/11/2026 |
| --- | --- | --- |
| HU02-D056 | Asignación pendiente de aprobación | No aparece como vigente. |
| HU02-D057 | Inicio 15/11/2026 | No aparece; permite probar una fecha futura. |
| HU02-D058 | Finalizó 07/11/2026 | No aparece; permite probar una fecha anterior. |
| HU02-D059 | Asignación inactiva | No aparece como vigente. |
| HU02-D060 | Menú inactivo | No aparece como vigente. |
| HU02-D061 a HU02-D065 | Alta con cama histórica | Figuran como altas; no aumentan la ocupación. |
| HU02-D066 a HU02-D068 | Alta sin cama | Figuran sin ubicación. |

Los menús 31 (inactivo), 32 (borrador) y 33 (activo sin asignaciones) no aparecen
en la programación de la fecha base. Las seis dietas y los cinco tiempos de
comida disponen de ejemplos en los menús vigentes.

## Uso de los scripts

La carga ya se ejecutó: **no es necesario volver a insertarla**. El script rechaza
otra carga si encuentra los códigos reservados, sin sobrescribir ni actualizar
los registros existentes.

Para una futura base local vacía que cumpla los requisitos, revisar primero el
destino en un cliente SQL y establecer en esa misma conexión:

```sql
SET @hu02_base_confirmada = 'alimentacion_hospitalaria';
SET @hu02_demo_confirmacion = 'CARGAR DEMO HU02 LOCAL';
```

Después ejecutar el archivo completo de carga sin opciones que continúen tras
errores (`--force`). No usarlo en una base compartida o productiva. No desactivar
las restricciones de clave foránea. No contiene procedimientos de borrado.

El script de verificación es de solo lectura. Sin una fecha explícita utiliza
la fecha local de creación del conjunto; se puede cambiar en la misma conexión:

```sql
SET @hu02_fecha = '2026-11-08';
```

La prueba de impresión y PDF continúa a cargo del usuario, siguiendo la
[guía manual](HU-02-prueba-manual-impresion.md).
