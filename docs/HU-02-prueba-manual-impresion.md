# HU-02 — Prueba manual de impresión y PDF

Estado: procedimiento preparado. La exportación y la inspección visual serán
realizadas manualmente por el usuario; no se registran aquí como pruebas ejecutadas.

## Preparación

- Iniciar la aplicación siguiendo el README y usar únicamente datos sintéticos.
- Iniciar sesión con un rol autorizado. Administración, Médico, Nutrición y piso
  consultan los cuatro reportes; Cocina solo asignaciones y menús.
- Para comprobar varias páginas, elegir un conjunto con suficientes coincidencias.
  Si no hay suficientes datos de demostración, registrar ese caso como pendiente.

## Repetir para los cuatro reportes

1. Abrir **Reportes generales**, seleccionar un reporte y aplicar filtros que
   produzcan resultados. Anotar los filtros y el total de coincidencias.
2. Si hay varias páginas de consulta, avanzar a la segunda y pulsar **Imprimir / PDF**.
3. En la pestaña nueva comprobar:
   - Título del reporte, fecha y hora de generación y filtros aplicados.
   - Total igual al de la consulta, sin limitarlo a las 20 filas de la página visible.
   - Ausencia de menú lateral, nombre de sesión, cierre de sesión y paginación.
   - Datos y columnas correspondientes al rol activo.
4. Pulsar **Imprimir o guardar PDF**. En el diálogo del navegador seleccionar
   **Guardar como PDF**, papel **A4**, orientación **horizontal** y escala **100 %**.
   La hoja de estilos propone esos valores; comprobarlos en la vista previa.
5. Revisar la vista previa y guardar el archivo. Abrir el PDF guardado y comprobar:
   - Texto legible, acentos correctos y columnas completas, sin solapamientos.
   - Encabezados de columnas repetidos cuando la tabla ocupa varias páginas.
   - Presencia de la primera y la última fila esperadas; ninguna fila omitida.
   - Ausencia de botones y enlaces de navegación en el papel.
6. Usar **Volver al reporte**: deben conservarse los filtros y regresar a la primera
   página de consulta.

## Comprobaciones por reporte y rol

| Reporte o rol | Resultado esperado |
| --- | --- |
| Pacientes | Código, nombre, piso, cama y estado. Las ubicaciones conservadas de altas se identifican como históricas. |
| Asignaciones: Administración, Médico y Nutrición | Incluye responsable, inicio, fin, estado y tiempos de comida, además de los datos operativos. |
| Asignaciones: piso | No incluye responsable, fechas individuales ni tiempos de comida. |
| Asignaciones: Cocina | Incluye fecha de consulta y tiempos; no incluye responsable, inicio, fin ni historial. |
| Menús | Fecha, tiempo de comida, hora, menú, dieta y estado; no duplica un menú por paciente. |
| Ocupación | Camas disponibles y ocupadas; si existen inconsistencias sintéticas, muestra todos sus ocupantes ingresados. Las altas no ocupan camas. |

Ningún reporte debe contener medicamentos, enfermedades, alergias completas ni
otros datos clínicos no incluidos en el contrato.

## Casos adicionales

- **Sin coincidencias:** aplicar filtros válidos sin resultados y abrir impresión.
  Debe mostrar total cero y un estado vacío comprensible, sin error técnico.
- **Permisos:** con Cocina, intentar las rutas `/reportes/pacientes/impresion` y
  `/reportes/ocupacion/impresion`. Deben responder con acceso denegado (HTTP 403).
- **Filtros inválidos:** por ejemplo, una fecha imposible en el parámetro `fecha`
  debe responder HTTP 400 con un mensaje comprensible.
- **Límite:** hasta 1000 registros se imprimen completos. Si existen más de 1000
  coincidencias sintéticas, la ruta responde HTTP 422, solicita reducir filtros
  y no genera una impresión parcial. Este límite ya tiene prueba automatizada;
  no es necesario crear 1001 registros manualmente para comprobar el diseño visual.

## Registro de resultados

Por cada PDF probado, registrar fecha, navegador y versión, rol, reporte, filtros,
total de registros, número de páginas y resultado observado. Marcar cada caso
como aprobado, fallido o pendiente. Si falla, describir la página y columna
afectadas. Conservar únicamente evidencia sintética y sin credenciales.

La HU-02 no debe declararse terminada solo por disponer de estas instrucciones:
la validación visual del PDF y el cierre de evidencia del paso 7 siguen pendientes.
