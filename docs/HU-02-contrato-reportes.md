# HU-02 — Paso 1: contrato de los cuatro reportes generales

Estado actual: contrato con D-01 a D-03 aprobadas; consultas, acceso HTTP y
pantallas del paso 5 implementados. Este documento define entradas, salidas,
permisos y verificaciones. La impresión del paso 6 está implementada; su exportación
y validación visual serán realizadas manualmente por el usuario. El cierre de
evidencia sigue pendiente.
Las decisiones D-01, D-02 y D-03 fueron aprobadas por el usuario el 8 de noviembre de 2026.

## Fuentes y modelo comprobado

- [HU-02 y contrato de permisos de HU-01](../historias.md) son la fuente de requisitos.
- [ReglasAcceso](../src/main/java/sv/edu/ues/alimentacion_hospitalaria/security/ReglasAcceso.java)
  ya autoriza las cuatro rutas de reporte por rol, pero no implementa sus consultas.
- [Paciente](../src/main/java/sv/edu/ues/alimentacion_hospitalaria/pacientes/domain/Paciente.java)
  contiene código, nombres, apellidos, estado y cama opcional.
- [Cama](../src/main/java/sv/edu/ues/alimentacion_hospitalaria/ubicaciones/domain/Cama.java)
  pertenece directamente a un piso. No existe una entidad de habitación.
- [V11](../src/main/resources/db/migration/V11__cardinalidad_cama_pacientes.sql)
  permite varios pacientes históricos asociados a una cama.
- [AsignacionMenu](../src/main/java/sv/edu/ues/alimentacion_hospitalaria/asignaciones/domain/AsignacionMenu.java)
  relaciona paciente y menú, con fechas de inicio y fin, indicador de actividad,
  asignador y aprobación opcional.
- [PrescripcionDieta](../src/main/java/sv/edu/ues/alimentacion_hospitalaria/prescripciones/domain/PrescripcionDieta.java)
  registra una autorización de régimen dietético separada de la asignación de menú.
- [Menu](../src/main/java/sv/edu/ues/alimentacion_hospitalaria/menus/domain/Menu.java)
  contiene nombre, tipo de dieta y estado, sin fecha de programación.
  [MenuDetalle](../src/main/java/sv/edu/ues/alimentacion_hospitalaria/menus/domain/MenuDetalle.java)
  aporta tiempo de comida, hora de servicio y platillo.

La inspección fue de código y migraciones; no se consultó una base de datos viva.

## Decisiones aprobadas para implementar consultas

| Decisión aprobada | Regla adoptada | Consecuencia y criterio afectado |
| --- | --- | --- |
| D-01: habitación ausente | Usar piso y cama como ubicación disponible. Omitir la columna habitación y explicar la limitación; no inventar su valor ni crear una entidad nueva. | Aplicar a CA-02.2 y CA-02.5 y a las columnas operativas de HU-01. |
| D-02: fuente de asignaciones de dieta | Basar el reporte en `AsignacionMenu` aprobada y vigente; obtener la dieta de `menu.tipoDieta`. No mezclar registros independientes de `PrescripcionDieta`. | Una prescripción sin asignación de menú no aparecería. Interpretación aprobada para CA-02.3. |
| D-03: fecha de menús programados | Derivar la oferta de una fecha de las asignaciones vigentes de menús activos y sus detalles. Una fila por fecha, menú y tiempo de comida, sin duplicar por paciente. | Un menú de catálogo sin asignaciones vigentes no aparecería. Interpretación aprobada para CA-02.4. |

Las secciones siguientes adoptan esas decisiones aprobadas. No se amplía el
modelo compartido. Si el equipo elige otra interpretación, actualizar primero el contrato.

## Acceso y rutas

Las rutas de los reportes coinciden con las autorizadas por HU-01. La portada
`/reportes` y los sufijos `/impresion` son rutas propuestas para implementación.

| Recurso | Administración | Médico | Nutrición | Encargado de piso | Cocina |
| --- | :---: | :---: | :---: | :---: | :---: |
| `/reportes` | Sí | Sí | Sí | Sí | Sí, con sus dos opciones |
| `/reportes/pacientes` | Sí | Sí | Sí | Sí | No |
| `/reportes/asignaciones` | Sí | Sí | Sí | Sí | Sí |
| `/reportes/menus` | Sí | Sí | Sí | Sí | Sí |
| `/reportes/ocupacion` | Sí | Sí | Sí | Sí | No |

Consulta mediante GET y HEAD. Cada variante `/impresion` tendrá los mismos
permisos y filtros que su reporte. Un usuario anónimo deberá iniciar sesión;
un usuario autenticado sin permiso recibirá HTTP 403. El módulo será de lectura
y no concederá acciones de creación, edición, aprobación o eliminación.

## Entradas y comportamiento común propuesto

- `pagina`: entero desde cero, por defecto cero. Tamaño fijo inicial: 20 filas.
  Una página negativa o un parámetro mal formado mostrará un error comprensible.
- `buscar`: texto opcional, recortado, hasta 100 caracteres; coincidencia por
  código o nombre del paciente en los reportes que lo admitan.
- `pisoId`: identificador opcional de un piso existente. No se cargarán entidades
  clínicas completas para construir opciones de filtros.
- `fecha`: fecha ISO `yyyy-MM-dd`, por defecto el día actual en
  `America/El_Salvador`, para asignaciones y menús. Representará vigencia a esa
  fecha evaluada con el estado actual de los registros, no una reconstrucción
  histórica de ubicaciones o estados que el modelo no conserva.
- Los filtros de estado, dieta y tiempo de comida aceptarán exclusivamente los
  valores del modelo. Un filtro ausente no impondrá restricciones adicionales.
- Un filtro válido sin coincidencias mostrará «No hay resultados para los
  filtros seleccionados». No se confundirán filtros inválidos con listas vacías.
- Consultas parametrizadas, paginación en MySQL, transacciones de lectura y
  proyecciones o DTO explícitos; sin dependencias nuevas ni datos fijos en vistas.
- Cada orden incluirá un identificador interno para desempatar; ese identificador
  no tiene que mostrarse como columna técnica.

## Reporte de pacientes y ubicación — CA-02.2

Ruta: `/reportes/pacientes`.

- Unidad de resultado: un paciente registrado.
- Columnas: código, nombre completo, piso, cama y estado.
- Filtros: `buscar`, `pisoId`, `estado` de paciente y `pagina`.
- Orden: código ascendente e identificador de paciente ascendente.
- Sin cama: mostrar «Sin ubicación», sin excluir al paciente por un join interno.
- Paciente de alta: identificar su cama conservada como referencia histórica,
  no como ocupación actual. El filtro de piso se referirá a esa cama asociada.
- No entregar peso, altura, fecha de nacimiento, alergias, enfermedades ni
  medicamentos en el modelo de vista de este reporte, cualquiera que sea el rol.
- Pruebas previstas: ingresados, altas, sin cama, filtros combinados, búsqueda,
  orden, paginación y rechazo a Cocina en consulta e impresión.

## Reporte de asignaciones activas — CA-02.3

Ruta: `/reportes/asignaciones`. Depende de D-02.

Una asignación operativa vigente deberá satisfacer todas estas condiciones:

- `activa = true` y aprobación registrada.
- `fechaInicio <= fecha` y `fechaFin` nula o mayor o igual a `fecha`.
- Paciente actualmente `INGRESADO` y menú actualmente `ACTIVO`.

La aprobación deberá ser coherente con los campos de aprobador y fecha de
aprobación existentes. Las asignaciones pendientes, inactivas, futuras o vencidas
no serán resultados de este reporte. No se ofrecerá un filtro que eluda esas reglas.

| Destinatario | Campos permitidos en el modelo de vista |
| --- | --- |
| Administración, Médico y Nutrición | Código y nombre del paciente, piso, cama, tipo de dieta, menú, fecha de consulta, fecha de inicio y fin, estado de vigencia, responsable de asignación y tiempos de comida. |
| Encargado de piso | Código y nombre del paciente, piso, cama, tipo de dieta, estado de vigencia y menú. Sin fechas individuales, responsable ni tiempos de comida. |
| Cocina | Código y nombre del paciente, piso, cama, tipo de dieta, menú aprobado, fecha de consulta y tiempos de comida. Sin responsable ni historial individual. |

- Unidad de resultado: una asignación; agrupar sus tiempos de comida sin
  multiplicar las filas paginadas al unir detalles del menú.
- Filtros: `buscar`, `pisoId`, `tipoDieta`, `fecha` y `pagina`.
- Orden: código de paciente e identificador de asignación ascendentes.
- Responsable significa `asignadoPor`, no médico responsable del paciente ni
  aprobador. Fechas individuales solo para los roles que las tienen autorizadas.
- La fecha de consulta podrá mostrarse como filtro de cabecera para todos;
  no implica entregar las fechas de asignación al Encargado de piso.
- Separar los modelos por rol en el servidor; ocultar columnas no será suficiente.
- Pruebas previstas: límites inclusivos de fechas, fin abierto, aprobación,
  pacientes de alta, menús inactivos, varios tiempos de comida y campos por rol
  tanto en HTML como en el modelo de vista y la impresión.

## Reporte de menús programados — CA-02.4

Ruta: `/reportes/menus`. Depende de D-03 y de las reglas de vigencia de D-02.

- Unidad de resultado: fecha de consulta, menú y tiempo de comida.
- Columnas: fecha, tiempo de comida, hora de servicio, nombre del menú,
  tipo de dieta y estado del menú.
- Filtros: `fecha`, `tiempoComida`, `tipoDieta` y `pagina`.
- Incluir solo menús activos con al menos una asignación aprobada y vigente a
  un paciente ingresado. Un menú sin detalles no generará filas de comidas.
- Dos pacientes con el mismo menú no duplicarán su fila por tiempo de comida.
- Orden: secuencia operativa de los tiempos de comida (desayuno, colación
  matutina, almuerzo, colación vespertina, cena), nombre e identificador de menú
  e identificador de detalle. Se ordena antes de paginar en MySQL. Esta elección
  evita depender del desplazamiento de LocalTime persistido bajo JDBC UTC; las
  horas originales se conservan en la salida. No implica ordenar por hora exacta
  entre menús con horarios distintos.
- Pruebas previstas: mismo menú con varios pacientes, varios tiempos, ausencia
  de asignaciones, menús inactivos, fechas límite y acceso de los cinco roles.

## Reporte de camas y ocupación — CA-02.5

Ruta: `/reportes/ocupacion`.

- Unidad de paginación: una cama registrada, incluidas las camas sin paciente.
- Columnas: piso, cama, estado de ocupación y código/nombre del ocupante actual.
- Filtros: `pisoId`, `cama` por código, `ocupacion` y `pagina`.
- Estados de vista propuestos, no nuevos estados persistidos: «Disponible»
  cuando no hay ingresados, «Ocupada» cuando hay uno e «Inconsistente» cuando
  hay varios. Este último caso mostrará los ocupantes afectados sin elegir
  arbitrariamente uno ni corregir registros desde el reporte.
- Pacientes `ALTA` no ocupan camas aunque conserven la referencia histórica.
- Orden: código e identificador de piso, número e identificador de cama.
- Pruebas previstas: cama vacía, ocupada, con históricos de alta, ocupación
  múltiple inconsistente, filtros, paginación sin duplicar camas y rechazo a Cocina.

## Impresión y límites

Cada reporte ofrecerá título, fecha de generación, filtros aplicados, encabezados
repetibles, estado vacío y tablas legibles sin navegación lateral. Se usará HTML
imprimible y la función de guardar PDF del navegador.

Límite implementado: imprimir todos los resultados filtrados hasta 1000 unidades
del reporte. Consultar primero el total; si excede el límite, solicitar filtros
más específicos sin generar una salida parcial presentada como completa.
No usar la paginación visible como recorte silencioso de la impresión. Evaluar
el límite con datos sintéticos representativos antes de cerrar CA-02.10 y CA-02.12.

## Trazabilidad y verificación posterior

| Criterios | Trabajo previsto | Evidencia requerida para declararlos cubiertos |
| --- | --- | --- |
| CA-02.1 | Portada y navegación por rol | Pruebas web y demostración de opciones autorizadas. |
| CA-02.2–CA-02.5 | Los cuatro contratos anteriores | Consultas y columnas verificadas con MySQL descartable; resolución de D-01 a D-03. |
| CA-02.6 | Repositorios sobre el modelo persistido | Resultados derivados de registros sintéticos insertados en MySQL. |
| CA-02.7–CA-02.9 | Filtros, validación, vacíos y orden | Pruebas positivas, negativas, vacías, nulos y desempates. |
| CA-02.10 | Cuatro vistas imprimibles | Pruebas de renderizado y comprobación real de impresión/PDF multipágina. |
| CA-02.11 | Reglas de rutas y DTO por rol | Acceso permitido/403 y ausencia de campos no autorizados en consulta e impresión. |
| CA-02.12 | Proyecciones, paginación y límites | Conteo correcto, páginas estables y consultas sin crecimiento N+1 con datos representativos. |
| CA-02.13 | Pruebas de repositorio, servicio y web | Pruebas específicas y `./mvnw verify` satisfactorios. |
| CA-02.14 | Escenarios de demostración | Instrucciones y evidencia con datos exclusivamente sintéticos. |

Estado de este paso: contrato documental elaborado; ninguna de las verificaciones
funcionales anteriores se ha ejecutado por crear este documento. La implementación
del módulo, las pruebas y la aprobación de las decisiones correspondían
a los pasos siguientes al cerrar el paso 1. No se requiere una migración para este paso documental.

## Registro del paso 2: base por capas

Se incorporaron `reportes.web.ReportesController`,
`reportes.service.CatalogoReportesService` y los DTO `TipoReporte`,
`OpcionReporteVista` y `FiltrosReporte`. La portada `/reportes` y `/reportes/`
está protegida y muestra el catálogo autorizado por rol, con las consultas
marcadas como pendientes y sin enlaces a resultados aún no implementados.

Los filtros tipados preparan validación local de páginas, identificadores y
longitudes. No comprueban todavía existencia de pisos, no completan fechas
omitidas ni están conectados a consultas o formularios. Al terminar este paso, los DTO de filas y las proyecciones de repositorio
quedaron para el paso 3, después de resolver las decisiones sobre su contenido. No se crearon repositorios
vacíos ni entidades duplicadas.

CA-02.1 y CA-02.11 tienen avance parcial: existe entrada autorizada al módulo,
pero todavía no se pueden seleccionar ni ejecutar reportes. Los demás criterios
funcionales quedaron pendientes. D-01, D-02 y D-03 estaban sin aprobación
al finalizar el paso 2; su aprobación posterior consta al inicio del documento.

Verificación del paso 2 realizada el 8 de noviembre de 2026 con Java 21 y MySQL
8.4 descartable mediante Testcontainers:

- `./mvnw -Dtest=FiltrosReporteTests,CatalogoReportesWebTests test`: 16 pruebas
  aprobadas, sin errores ni omisiones.
- `./mvnw verify`: compilación, pruebas y empaquetado correctos; 647 casos,
  646 aprobados y uno omitido (`LogoutBrowserTests`, prueba opcional de navegador).
- `git diff --check`: sin errores de formato.

Estas verificaciones cubren el catálogo por rol, GET/HEAD, rechazo de escrituras
con CSRF, acceso anónimo o no autorizado y validación local de filtros. No
acreditan las consultas, resultados ni impresión de los cuatro reportes.

## Avance parcial del paso 3: validación común

Se añadió `ValidacionConsultaReportes` con validación Bean Validation de los
filtros, comprobación de existencia del piso seleccionado y preparación de
páginas de tamaño fijo (20 filas). Los errores se distinguen de una consulta
válida sin resultados. Esta preparación todavía no está conectada a consultas
de reportes ni a formularios HTTP.

Al cerrar este avance parcial, las consultas quedaron pendientes de confirmar
D-01 a D-03. El usuario aprobó posteriormente las tres reglas; el avance completo
del paso 3 se registra a continuación.

Verificación realizada el 8 de noviembre de 2026:

- `./mvnw -Dtest=FiltrosReporteTests,ValidacionConsultaReportesTests test`:
  siete pruebas aprobadas.
- `./mvnw verify`: correcto; 651 casos, 650 aprobados y una prueba opcional de
  navegador omitida. Se utilizó MySQL descartable mediante Testcontainers.
- `git diff --check`: sin errores de formato.

Este avance prepara CA-02.7 y CA-02.12; no los completa ni acredita resultados
de los cuatro reportes.

## Paso 3: consultas implementadas después de aprobar D-01 a D-03

El usuario aprobó expresamente las tres reglas el 8 de noviembre de 2026.
`ConsultasReportesRepository` realiza consultas HQL parametrizadas sobre las
entidades existentes; proyecta columnas escalares, cuenta resultados y pagina
antes de cargar colecciones. `ConsultaReportesService` valida filtros, aplica
permisos por método, completa la fecha de consulta y devuelve `FilasReporte`.
`AccesoReportes` comparte la comprobación de un único rol entre catálogo y consultas.

- Pacientes: filtros de estado, piso y texto, búsqueda literal de `%` y `_`,
  inclusión de pacientes sin cama y marcador de ubicación histórica para altas.
- Asignaciones: solo activas, aprobadas, vigentes en la fecha solicitada, con
  paciente ingresado y menú activo. Los tiempos se cargan en un lote para los
  menús de la página; los DTO de piso y Cocina carecen de los campos no autorizados.
- Menús: un resultado por detalle del menú con al menos una asignación vigente;
  `exists` evita multiplicar filas por paciente. Se ordena por secuencia operativa
  de comida antes de paginar, conservando la hora de servicio leída por Hibernate.
- Ocupación: una unidad paginada por cama, contando solo ingresados; se cargan los
  ocupantes de las camas de la página en un lote. Las inconsistencias se muestran
  en los datos de salida sin alterar la base ni descartar ocupantes.

Las consultas y DTO se verificaron con MySQL 8.4 descartable. Las pruebas no
representan verificación de vistas de resultados ni PDF, que siguen pendientes.
CA-02.2 a CA-02.9, CA-02.11 a CA-02.14 tienen evidencia de la capa de datos y
servicio según corresponda, pero no se declaran completos sin la integración
HTTP, presentación y demostraciones restantes. CA-02.10 sigue pendiente.

Verificación del paso 3 completada el 8 de noviembre de 2026:

- `./mvnw -Dtest=ConsultaReportesTests test`: 20 pruebas aprobadas, sin errores.
- `./mvnw verify`: compilación, pruebas y empaquetado correctos; 671 casos,
  670 aprobados y uno omitido (`LogoutBrowserTests`, prueba opcional de navegador).
- Las pruebas de estadísticas con páginas de 20 filas comprobaron tres sentencias
  SQL para asignaciones y tres para ocupación, sin cargar entidades JPA. La
  validación de un piso explícito añade una comprobación de existencia constante.
- `git diff --check`: sin errores de formato. Los enlaces locales de la
  documentación se comprobaron y existen.

## Paso 4: autorización y validación en las rutas HTTP

`ReportesController` conecta las cuatro consultas existentes mediante GET/HEAD
con o sin barra final, manteniendo controladores delgados y DTO por rol. Los
filtros se enlazan mediante constructor y Bean Validation. La página omitida se
normaliza a cero; errores de conversión (incluidas fechas imposibles y enums
inválidos) responden HTTP 400 sin exponer el valor rechazado ni trazas.

`FiltroReporteInvalidoException` distingue los errores de entrada conocidos de
los errores internos de consultas. Solo los primeros se muestran como mensajes
comprensibles; no se captura de forma general `IllegalArgumentException` para
presentar su contenido al usuario. La autorización se mantiene en la cadena de
seguridad y en los servicios; `AccesoReportes` rechaza roles desconocidos o
múltiples antes de enlazar los filtros del controlador.

La vista `reportes/consulta` muestra título, total, fecha cuando corresponde y
estado vacío. La tabla de filas, los formularios, la selección desde las tarjetas
pendientes y la impresión no forman parte de este incremento. Las filas del
modelo ya están restringidas al DTO de cada rol; no se entregan entidades JPA.

CA-02.7, CA-02.8, CA-02.11 y CA-02.13 tienen evidencia HTTP de validación y
permisos. La presentación y selección completas de reportes, así como las vistas
de impresión, siguen pendientes de los siguientes pasos.

Verificación del paso 4 realizada el 8 de noviembre de 2026 con MySQL descartable:

- `./mvnw -Dtest=ReportesHttpTests,FiltrosReporteTests test`: 21 pruebas aprobadas.
  La primera ejecución detectó el enlace incorrecto de `pagina` omitida; se
  corrigió para usar cero y se repitieron las verificaciones satisfactoriamente.
- `./mvnw verify`: compilación, pruebas y empaquetado correctos; 689 casos,
  688 aprobados y uno omitido (`LogoutBrowserTests`, prueba opcional de navegador).
- Se verificaron GET/HEAD con y sin barra final, los cinco roles, rechazo de
  escrituras con CSRF válido, DTO por rol, filtros inválidos y ausencia de SQL
  cuando Cocina solicita pacientes u ocupación.
- `git diff --check` sin errores y enlaces locales de documentación válidos.

No se verificaron tablas ni impresión, todavía pendientes de implementación.

## Paso 5: tablas, filtros y navegación

Las tarjetas de `/reportes` enlazan a las cuatro pantallas autorizadas. Las vistas
`reportes/consulta`, `reportes/filtros` y `reportes/tablas` presentan los DTO de los
servicios, sin introducir datos clínicos adicionales. Los selectores de pisos
usan una consulta de catálogo que proyecta identificador, código y nombre; no
cargan entidades ni relaciones de pacientes.

- Formularios GET con etiquetas, límites de búsqueda y opciones de los enums
  existentes; las selecciones y la fecha efectiva se mantienen al renderizar.
- Tablas adaptadas al ancho mediante Bootstrap, con encabezados de columna,
  captions y estados vacíos. Los nombres y valores se escapan con Thymeleaf.
- Columnas de asignaciones adecuadas al DTO del rol. Piso no ve responsable,
  fechas individuales ni tiempos; Cocina no ve responsable, inicio, fin ni estado
  administrativo. Los permisos del servidor y las formas de DTO se conservan.
- Ubicación histórica identificada para pacientes de alta y advertencia explícita
  para camas con varios ingresados, mostrando los ocupantes afectados.
- Enlaces Anterior/Siguiente con los filtros tipados, la fecha efectiva y valores
  codificados; la búsqueda con acentos, espacios, `+` y `&` se conserva. Aplicar
  o limpiar filtros reinicia la página. Una página fuera de rango permite volver
  a la primera sin perder los filtros.

CA-02.1 a CA-02.9 y CA-02.11 a CA-02.13 cuentan con implementación de consulta,
servicio y presentación según su alcance. La aprobación de D-01 permite omitir
habitación. No se declara terminada HU-02: faltan CA-02.10 (impresión/PDF) y el
cierre de demostración y evidencia de CA-02.14. La prueba de renderizado HTTP no
sustituye la comprobación visual de navegador ni la exportación de PDF.

Verificación del paso 5 realizada el 8 de noviembre de 2026 con MySQL descartable:

- `./mvnw -Dtest=CatalogoReportesWebTests,ReportesHttpTests,EnlacesConsultaReportesTests test`:
  37 pruebas aprobadas. Incluye selección por rol, renderizado de tablas y filtros,
  conservación de selecciones, camas inconsistentes y navegación real entre
  peticiones HTTP con texto de búsqueda que contiene acentos y caracteres especiales.
- La prueba de navegación detectó una diferencia en la interpretación de espacios
  codificados como `+`; los enlaces ahora usan `%20`, conservando `+` literal como
  `%2B`. La repetición de las pruebas fue satisfactoria.
- `./mvnw verify`: compilación, pruebas y empaquetado correctos; 695 casos,
  694 aprobados y uno omitido (`LogoutBrowserTests`, prueba opcional de navegador).
- `git diff --check` sin errores y enlaces locales de documentación válidos.

La verificación fue de renderizado HTTP y modelos con datos sintéticos; no se
realizó una inspección visual en navegador ni se generaron capturas o PDF.

## Paso 6: vistas imprimibles y guardado desde el navegador

Cada reporte ofrece una ruta `/impresion`, con o sin barra final, que reutiliza
las tablas y DTO autorizados. El enlace **Imprimir / PDF** conserva los filtros,
incluida la fecha efectiva, y omite la página visible. La salida incluye el total
filtrado, título, fecha de generación y resumen de filtros, sin menú lateral ni
controles de paginación. El botón **Imprimir o guardar PDF** invoca la impresión
del navegador. **Volver al reporte** conserva los filtros y usa la primera página.

El límite de 1000 unidades se define en `ConsultaReportesService`. El repositorio
cuenta primero y aplica el máximo recibido antes de recuperar filas. Hasta 1000
se incluyen todos los registros; si hay más, `LimiteResultadosException` produce
HTTP 422 con una indicación para reducir filtros, sin entregar una tabla parcial.
La impresión usa transacciones de lectura con aislamiento `REPEATABLE_READ` para
mantener coherentes el conteo, las filas y las cargas por lotes.

La hoja `reportes-impresion.css` propone A4 horizontal, encabezados de tabla
repetidos, ajuste de palabras y ocultamiento de herramientas al imprimir.
No se añadió una dependencia de PDF a producción ni se cambió el esquema.

Verificación del paso 6 realizada el 8 de noviembre de 2026 con MySQL descartable:

- `./mvnw -Dtest=ReportesHttpTests test`: 29 pruebas aprobadas, incluidas impresión
  por rol, GET/HEAD, rechazo de escrituras, filtros, estado vacío y límite.
- Se comprobó que 1000 filas incluyen la primera y la última; 1001 responden 422
  con una sola sentencia de conteo, sin cargar filas ni entregar `pagina` al modelo.
- `./mvnw clean verify`: compilación, pruebas y empaquetado correctos; 703 casos,
  702 aprobados y uno omitido (`LogoutBrowserTests`, prueba opcional de navegador).
- `git diff --check` sin errores y enlaces locales de documentación válidos.

Por indicación expresa del usuario, la exportación y la inspección del PDF se
realizarán manualmente. No se ejecutó la exportación automática preparada y se
retiraron sus archivos de prueba. No se generaron PDFs ni capturas. Seguir la
[guía manual de impresión](HU-02-prueba-manual-impresion.md) y registrar resultados
antes de acreditar la validación visual de CA-02.10 y el cierre de HU-02.
