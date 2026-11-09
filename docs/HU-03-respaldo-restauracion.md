# HU-03 — Copias de seguridad y restauración MySQL

Este procedimiento conserva esquema y datos de una base del sistema y comprueba
su recuperación en otra base vacía. Usa los clientes oficiales del contenedor
MySQL 8.4, Bash y Python 3 (biblioteca estándar). No cambia el código Java ni las
migraciones. El historial de Flyway forma parte del respaldo.

## Requisitos y límites

- Docker Engine/Desktop y Docker Compose disponibles; ejecutar desde la raíz del repositorio.
- Python 3.9 o posterior y Bash en el equipo; no es necesario instalar MySQL localmente.
- Contenedor MySQL 8.4 iniciado con `MYSQL_USER` y `MYSQL_PASSWORD` configurados.
- Usuario con permisos de lectura y `SHOW VIEW`/`TRIGGER` para respaldar, y los
  permisos DDL/DML necesarios para importar en su base destino. No se emplea root.
- Todas las tablas deben ser InnoDB. No ejecutar migraciones ni cambios de
  estructura durante el respaldo; `--single-transaction` no aísla esos cambios.
- Las tablas y los disparadores se exportan. Si se detectan rutinas o eventos,
  se rechaza la operación para no omitirlos; su soporte requiere ampliar y probar
  el procedimiento. El modelo actual no los define. El usuario de respaldo debe
  poder ver todos los objetos de la base.
- La recuperación se limita a MySQL local al contenedor, base vacía y entorno
  declarado `local` o `prueba`. No hay opción para sobrescribir bases existentes.
  Mantener el destino exclusivo y sin aplicaciones escribiendo durante la importación.
- Un respaldo es SQL ejecutable: importar solo archivos de origen confiable.
  SHA-256 detecta alteración/truncamiento respecto a una suma conservada por separado;
  no acredita autoría si un tercero puede reemplazar ambos archivos.
- No se respaldan usuarios/permisos internos de MySQL, variables de entorno,
  archivos del equipo ni configuración de Docker. Las cuentas de la aplicación,
  incluidos sus hashes, sí son datos necesarios para recuperar su autenticación.

## Crear un respaldo

Con el servicio `db` disponible y la configuración local ya establecida:

```bash
docker compose config --quiet
docker compose up -d db
bash scripts/mysql/respaldo.sh --base alimentacion_hospitalaria
```

El resultado se guarda en `backups/` con un nombre UTC como
`alimentacion_hospitalaria_20261107_153000_123456Z.sql` y un archivo
`.sql.sha256`. El directorio se puede cambiar:

```bash
bash scripts/mysql/respaldo.sh --base alimentacion_hospitalaria --directorio /tmp/respaldos-hospital
```

También se admite `HU03_BACKUP_DIR`. Para otro despliegue Compose, utilizar
`--compose ARCHIVO --proyecto NOMBRE --servicio db`; para un contenedor explícito,
`--contenedor NOMBRE`. `--host` y `--puerto` se interpretan **desde el contenedor**:
el valor habitual es `127.0.0.1:3306`, aunque en el equipo se publique 3307.
`--usuario` permite seleccionar otra cuenta únicamente si utiliza la contraseña
ya configurada en `MYSQL_PASSWORD` dentro de ese contenedor.

El script no carga `.env` como código shell ni solicita copiar su contraseña.
Compose proporciona la configuración existente. Dentro del contenedor se genera
un archivo temporal de opciones MySQL con permisos 0600 y se elimina al terminar;
el secreto no se incorpora a argumentos ni mensajes. Contraseñas con saltos de
línea no están soportadas. No ejecutar los scripts con trazas de shell (`bash -x`).

El dump se genera primero en un directorio temporal protegido, se comprueba su
finalización, se calcula SHA-256 y se publican los dos archivos sin sobrescrituras.
Una interrupción externa puede dejar un archivo sin su suma: no se considerará
restaurable. Para comprobar manualmente la integridad, desde el directorio del archivo:

```bash
shasum -a 256 -c NOMBRE_DEL_RESPALDO.sql.sha256
```

Los respaldos operativos pueden contener datos sensibles: mantenerlos fuera de
Git, con acceso restringido y almacenamiento protegido. `backups/` está excluido
de Git y Docker. Conservar la suma también en una ubicación confiable.

## Restaurar en una base nueva

Preparar mediante la administración de MySQL una base **vacía**, con codificación
`utf8mb4`, un usuario autorizado y contenedor configurado con sus variables.
Los scripts no crean ni eliminan bases. Ejemplo con un contenedor local preparado
llamado `mysql-recuperacion` y una base `hospital_recuperacion`:

```bash
bash scripts/mysql/restaurar.sh \
  --contenedor mysql-recuperacion \
  --base hospital_recuperacion \
  --entorno local \
  --archivo /tmp/respaldos-hospital/NOMBRE_DEL_RESPALDO.sql
```

Antes de importar se verifica el SHA-256, se muestra contenedor/servicio, host,
puerto, base y entorno, y se solicita escribir `hospital_recuperacion`. Si la suma
está en otro lugar, indicar `--suma ARCHIVO`. En una ejecución automatizada de
prueba, la confirmación se expresa con `--confirmar hospital_recuperacion`.

Solo se aceptan respaldos generados por `respaldo.sh`. El dump no incluye
`CREATE DATABASE` ni `USE`, por lo que se aplica a la base explícita de destino.
La importación deshabilita archivos locales y comandos interactivos del cliente.
No importar mientras otra aplicación modifica esa base.

Después de importar:

1. Comparar tablas, conteos, valores y relaciones con el origen de referencia.
2. Configurar una instancia separada de la aplicación contra la base recuperada.
3. Verificar arranque, validación de Flyway/Hibernate, login y una consulta funcional.
4. Conservar el respaldo y el informe antes de limpiar únicamente recursos de prueba.

La prueba automatizada siguiente ejecuta estos pasos sin utilizar la base local.
No cambiar el destino de la aplicación habitual para probar una recuperación.

Las protecciones de archivos también se pueden verificar sin Docker:

```bash
python3 -m unittest discover -s tests/hu03 -p 'test_*.py' -v
bash -n scripts/mysql/respaldo.sh scripts/mysql/restaurar.sh scripts/mysql/prueba-recuperacion.sh
```

## Prueba reproducible independiente

```bash
bash scripts/mysql/prueba-recuperacion.sh
```

La prueba construye la imagen del código actual, crea una red y dos servidores
`mysql:8.4` independientes, sin publicar MySQL ni reutilizar el volumen de
desarrollo. Publica la aplicación solo en loopback y en un puerto libre asignado
por Docker. Las credenciales aleatorias se mantienen en archivos temporales
0600 fuera del repositorio y se eliminan al finalizar.

Flyway prepara el origen y el perfil de inicialización crea una cuenta sintética.
El fixture agrega pacientes, datos clínicos ficticios, menús, dietas, ingredientes,
asignaciones, prescripciones y bitácora, aunque no exista todavía una pantalla de
registro de pacientes. Se genera un respaldo y se recupera en el otro servidor.
Se comparan todas las filas de todas las tablas y las claves foráneas, y la
aplicación recuperada debe autenticar al usuario y consultar la proyección de insumos.

También se comprueban nombres ocupados, credenciales incorrectas, bases ausentes
y vacías, suma ausente, archivo truncado, confirmación equivocada/ausente,
destino remoto/productivo y destino no vacío. Cada rechazo debe tener código no
cero y no debe cargar el destino. Al finalizar se eliminan solo los contenedores
con la etiqueta aleatoria de esa ejecución y sus volúmenes descartables.

La evidencia queda en `backups/hu03-prueba/IDENTIFICADOR/`: SQL sintético, SHA-256
y `resultado.json`. Para otra ubicación usar `--salida DIRECTORIO`. Los hashes
del fixture pertenecen a cuentas efímeras, sin contraseñas de demostración fijas;
no son credenciales de usuarios reales. La evidencia seleccionada para la entrega
se conserva en `docs/evidencias/hu03/`, siempre revisada antes de versionarla.

## Errores y recuperación

| Código | Significado y acción |
| --- | --- |
| 0 | Operación completada; todavía verificar la funcionalidad recuperada. |
| 2 | Argumentos, entorno o confirmación ausente inválidos. Consultar `--help`. |
| 3 | Docker/MySQL, permisos, compatibilidad o E/S fallaron. Revisar disponibilidad y configuración local sin compartir secretos. |
| 4 | Nombre ocupado, suma inválida o respaldo alterado. Conservar el original y regenerar desde una fuente confiable. |
| 5 | Confirmación incorrecta o destino no vacío. Elegir una base vacía y confirmar su nombre. |
| 130 | Interrupción del operador. Revisar archivos y estado del destino. |

La importación de DDL no es atómica. Si falla a mitad, el script informa que el
destino puede estar parcialmente cargado y rechaza reutilizarlo como si estuviera
vacío. Inspeccionar el problema y preparar otra base descartable; nunca borrar una
base compartida para reintentar. Los mensajes de los clientes se resumen para
evitar exponer consultas, datos o secretos.

Si la imagen no arranca por una migración del proyecto, la prueba falla antes de
declarar éxito. No reparar migraciones ni borrar el volumen de desarrollo desde
estos scripts. La V10 de conversión de merma debe evaluarse por separado al
actualizar una base que ya contenga porcentajes históricos.

## Trazabilidad de aceptación

| Criterios | Implementación/evidencia |
| --- | --- |
| CA-03.1–4 | `respaldo.sh`, esquema/datos, fecha UTC, destino configurable y publicación exclusiva. |
| CA-03.5 | Configuración existente, opciones temporales 0600, exclusiones Git/Docker y errores sin secretos. |
| CA-03.6–7 | `restaurar.sh`, destino explícito, vacío, local/de prueba y confirmación. |
| CA-03.8–11 | Prueba real en dos servidores, comparación completa, login y consulta con la aplicación restaurada. |
| CA-03.12 | SHA-256 obligatorio y rechazo de copia alterada. |
| CA-03.13 | Respaldo sintético, suma e informe de la ejecución conservados. |

## Referencias técnicas

- [MySQL 8.4: mysqldump y respaldo consistente](https://dev.mysql.com/doc/refman/8.4/en/mysqldump.html).
- [MySQL 8.4: protección de contraseñas y archivos de opciones](https://dev.mysql.com/doc/refman/8.4/en/password-security-user.html).

Se emplean las herramientas oficiales; los scripts y la prueba se implementaron
para las convenciones y el esquema de este repositorio.
