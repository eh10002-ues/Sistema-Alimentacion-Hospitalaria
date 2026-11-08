# Sistema de alimentación hospitalaria

## Ejecución local con Docker

Requisito: Docker Engine o Docker Desktop en ejecución y Docker Compose disponible.
No es necesario instalar Java, Maven ni MySQL en el equipo para este arranque.

1. Crear la configuración local:

   ```bash
   cp .env.example .env
   ```

2. Editar `.env` y completar `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD` con
   contraseñas locales distintas. Usar comillas simples si contienen `$` o `#`.
   `.env` está excluido de Git y del contexto de construcción.

3. Validar la configuración y construir e iniciar los servicios:

   ```bash
   docker compose config --quiet
   docker compose up -d --build
   docker compose ps
   docker compose logs -f app
   ```

4. Abrir <http://localhost:8080/login> (o el puerto definido en `APP_PORT`).
   La autenticación consulta los usuarios de MySQL: ya no existe el usuario
   temporal de Spring Boot. Para una base vacía, ejecutar primero el mecanismo
   del primer administrador descrito en el paso 3.

MySQL 8.4 queda accesible desde el equipo en `127.0.0.1:3307` y desde la
aplicación en `db:3306`. La aplicación espera a que MySQL acepte consultas con
su usuario antes de arrancar. Se pueden cambiar `APP_PORT` y `MYSQL_PORT` en
`.env` si los puertos están ocupados. Ambos servicios se publican solo en la
interfaz local del equipo.

La imagen compila con Java 21 y el Maven Wrapper del repositorio; la ejecución
usa Java 21 y un usuario sin privilegios. No incorpora configuración local ni
archivos de apoyo del agente. Flyway conserva la gestión del esquema y
Hibernate la validación. La migración `V1__HU01_crear_usuarios.sql` crea la tabla
de usuarios al iniciar una versión de la aplicación que la incluya; no inserta
cuentas ni credenciales iniciales.

## HU-01 — Paso 1: persistencia de usuarios

El modelo `Usuario` almacena un nombre de usuario único, normalizado en
minúsculas y sin espacios, un hash de contraseña, nombre completo, un único
`Rol` y el estado activo. Los cinco valores de `Rol` se almacenan como texto
estable; MySQL restringe los valores admitidos y la unicidad del identificador
sin distinguir mayúsculas. Los límites de almacenamiento son 100 caracteres
para el identificador, 150 para el nombre completo y 255 para el hash.

Las fechas se reutilizan de `AuditableEntity`: `createdAt` y `updatedAt`
representan creación y actualización, acompañadas de `createdBy` y `updatedBy`.
No se duplican como otros campos de fecha. El repositorio ofrece búsquedas y
listado paginado, sin operaciones de eliminación física.

El código que cree usuarios deberá entregar a la entidad un hash generado por
el servicio centralizado de contraseñas descrito en el paso 2. La administración
y la revocación de sesiones se describen en el paso 4.

Prueba específica con MySQL 8.4 descartable mediante Testcontainers, sin utilizar
la base del servicio Compose:

```bash
./mvnw -Dtest=UsuarioRepositoryTests test
```

## HU-01 — Paso 2: autenticación desde MySQL

`UsuarioDetailsService` carga el identificador sin distinguir mayúsculas,
el hash, el rol y el estado activo. Spring Security verifica la contraseña
(sensible a mayúsculas) y rechaza cuentas inexistentes o inactivas.
El formulario conserva el mensaje genérico de error y la protección CSRF.
El usuario autenticado recibe exactamente la autoridad almacenada en su cuenta.
La autorización por módulos y la navegación por rol se describen en el paso 5.

`PoliticaContrasena` centraliza la longitud de 12 a 72 caracteres Unicode,
mayúscula, minúscula, número, confirmación y rechazo de espacios exteriores.
La comparación con el nombre de usuario tampoco distingue mayúsculas.
`ServicioContrasenas` valida y codifica para su reutilización en creación,
restablecimiento e inicialización. No registra ni incluye los valores recibidos
en sus errores. Estas operaciones aún no tienen formularios ni rutas.

El `PasswordEncoder` utiliza PBKDF2 de Spring Security, con sal aleatoria y
prefijo de algoritmo mediante `DelegatingPasswordEncoder`. Permite contraseñas
Unicode de hasta 72 caracteres sin imponer el límite de bytes de BCrypt.
La política se aplica al generar hashes; el login verifica el hash existente.

Pruebas específicas:

```bash
./mvnw -Dtest=SecurityConfigTests,ContrasenasTests test
```

## HU-01 — Paso 3: primer administrador

El perfil `inicializacion` registra un `ApplicationRunner` que obtiene las
variables `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` y `APP_ADMIN_FULL_NAME`.
No hay valores predeterminados. El servicio transaccional crea una cuenta activa
con `ROLE_ADMIN`, nombre de usuario normalizado y contraseña codificada por
`ServicioContrasenas`. La contraseña debe cumplir la política del paso 2.

Si ya existe cualquier administrador, incluso inactivo, la inicialización no
crea otra cuenta, no modifica la existente y no exige volver a proporcionar
credenciales. Si no hay administrador y faltan variables o son inválidas, el
arranque falla con un mensaje de configuración sin mostrar sus valores.
Un identificador ocupado por otro usuario se rechaza: no se convierte esa
cuenta en administrador.

Para inicializar el servicio local de Compose:

1. Agregar a tu `.env` existente las cuatro variables indicadas en
   `.env.example`: `SPRING_PROFILES_ACTIVE`, `APP_ADMIN_USERNAME`,
   `APP_ADMIN_PASSWORD` y `APP_ADMIN_FULL_NAME`. No sobrescribir la configuración
   de MySQL ni el puerto que ya estés usando.
2. Completar los tres valores administrativos de forma local y establecer
   `SPRING_PROFILES_ACTIVE=inicializacion`. No compartir ni versionar `.env`.
3. Construir e iniciar una única instancia de la aplicación:

   ```bash
   docker compose config --quiet
   docker compose up -d --build app
   docker compose ps
   ```

4. Iniciar sesión con la cuenta configurada, usando el puerto `APP_PORT`.
   Desde la opción «Administrar usuarios» del menú lateral se accede a la administración.
   El acceso correcto abre `/dashboard`, con el panel correspondiente al rol.
5. Después de confirmar el acceso, vaciar `SPRING_PROFILES_ACTIVE` y retirar los
   tres valores `APP_ADMIN_*` del archivo `.env`. Recrear la aplicación para
   retirar también esas variables del contenedor:

   ```bash
   docker compose up -d app
   ```

La cuenta y su hash permanecen en MySQL al retirar el perfil. El procedimiento
está destinado al arranque inicial con la única instancia `app` de este Compose.
No requiere borrar el volumen ni ejecutar SQL manualmente.

Pruebas con datos sintéticos y MySQL descartable (sin usar el perfil de
inicialización en la suite general):

```bash
./mvnw -Dtest=PrimerAdministradorTests,InicializacionAdministradorConfigTests test
```

## HU-01 — Paso 4: administración de usuarios

Solo el Administrador puede acceder a `/admin/usuarios`. La opción «Administrar usuarios»
se muestra en el menú lateral después de iniciar sesión con ese rol. Desde allí
puede crear cuentas, editar nombre completo, identificador y rol, activar o
desactivar cuentas y restablecer contraseñas. No hay registro público ni
eliminación de usuarios. El listado utiliza páginas de 20 registros, ordenadas
por nombre de usuario e identificador.

Las operaciones de modificación utilizan POST con CSRF. Las rutas `/admin/**`
requieren `ROLE_ADMIN` y el servicio comprueba también ese permiso. Cada
modificación confirma en MySQL que quien la solicita sigue siendo un
administrador activo. Los formularios usan DTO con campos permitidos y las
vistas reciben datos sin hashes. Las contraseñas y confirmaciones se vacían
antes de devolver formularios; la política de contraseñas permanece centralizada.

No se permite desactivar la cuenta propia ni retirar el acceso del último
administrador activo. La migración `V2__HU01_serializar_administracion_usuarios.sql`
crea una fila de bloqueo. Las operaciones administrativas y la inicialización
la bloquean dentro de su transacción para que las comprobaciones y cambios
no se solapen. Una cuenta con un identificador ya ocupado se rechaza.

La desactivación, el cambio de rol, el cambio de identificador y el
restablecimiento de contraseña expiran todas las sesiones de la cuenta afectada
mediante `SessionRegistry`, después de confirmar la transacción. Un rollback no
expira sesiones. La siguiente solicitud de una sesión expirada se redirige a
`/login?expired`, antes de ejecutar la operación. Reactivar una cuenta no
restaura sesiones; se debe iniciar sesión nuevamente. El registro de sesiones
es local a la única instancia `app` de este Compose.

Para ejecutar la versión actualizada, una vez creada la cuenta administrativa:

```bash
docker compose up -d --build app
```

Después de autenticarte, abrir `/admin/usuarios` en el puerto de la aplicación
(por ejemplo, `http://localhost:8081/admin/usuarios` si `APP_PORT=8081`). Flyway
aplica la nueva migración al arrancar, conservando las cuentas existentes.

Pruebas con MySQL descartable, incluyendo accesos por rol, formularios,
concurrencia sobre el último administrador y revocación de múltiples sesiones:

```bash
./mvnw -Dtest=AdministracionUsuariosTests test
```

Este incremento cubre la administración y sus protecciones necesarias. El
dashboard y las reglas por módulo se describen en el paso 5.

## HU-01 — Paso 5: dashboard y autorización por módulos

Todos los inicios de sesión correctos redirigen a `/dashboard`. La raíz `/`
redirige al mismo panel. Un único controlador y una vista compartida muestran
el usuario, el rol, las responsabilidades autorizadas y los accesos funcionales
existentes. El Administrador dispone del acceso a usuarios; los demás roles
tienen un panel informativo propio. No se muestran enlaces a módulos todavía
inexistentes ni datos operativos simulados. El menú lateral permite regresar al
panel y cerrar sesión.

`ReglasAcceso` centraliza las reglas de la cadena HTTP. Las consultas admiten
GET y HEAD; las modificaciones requieren los roles indicados por la matriz y
conservan CSRF. Las pantallas de creación y edición también requieren el rol
que puede modificar el módulo. Aprobar y rechazar asignaciones se reserva a
Administrador y Nutricionista, con reglas anteriores a la regla general del
módulo. No se habilita eliminación de pacientes ni asignaciones porque esas
operaciones no están definidas en el contrato. Las rutas sin regla explícita
se deniegan; al integrar una nueva ruta debe incorporarse su autorización.

| Recurso | Consulta | Modificación |
| --- | --- | --- |
| Usuarios | Administrador | Administrador |
| Pacientes | Administrador, Médico, Nutricionista y Encargado de piso | Administrador y Médico |
| Ubicaciones | Administrador, Médico, Nutricionista y Encargado de piso | Administrador y Encargado de piso |
| Dietas y menús | Los cinco roles | Administrador y Nutricionista |
| Asignaciones | Administrador, Médico y Nutricionista | Administrador, Médico y Nutricionista; aprobación/rechazo solo Administrador y Nutricionista |
| Reportes de pacientes y ocupación | Administrador, Médico, Nutricionista y Encargado de piso | Sin modificación |
| Reportes de asignaciones y menús | Los cinco roles | Sin modificación |
| Consultas operativas de cocina (SAH-6: pacientes por régimen y menús por paciente) | Administrador, Nutricionista y Cocina | Sin modificación |

Un acceso no autorizado responde con HTTP 403 y presenta la vista de acceso
denegado, con un enlace para regresar al dashboard. El servicio de dashboard
rechaza cuentas sin un rol reconocido o con más de un rol del contrato.

Las pruebas `DashboardPermisosTests` verifican login y renderizado para los
cinco roles, navegación, respuestas 403 y permisos por método HTTP. Los
endpoints de módulos pendientes se representan mediante un controlador
exclusivo de pruebas: las solicitudes denegadas no llegan a ejecutarlo.
Este controlador no se incorpora al artefacto de producción.

```bash
./mvnw -Dtest=DashboardPermisosTests,SecurityConfigTests,AdministracionUsuariosTests test
```

Las reglas HTTP no implementan los módulos operativos ni los cuatro reportes.
Cuando se integren, deberán aplicar sus filtros de estado y los DTO específicos
por rol para impedir la exposición de datos clínicos o campos no autorizados.
En particular, CA-01.26 requiere la futura implementación del reporte de
asignaciones: no se considera cumplido por estas pruebas de rutas.

## HU-01 — Paso 6: cierre de sesión y respuestas HTTP

El logout invalida la sesión, limpia la autenticación y expira la cookie
`JSESSIONID`. Las páginas protegidas conservan las cabeceras `no-store` de
Spring Security. Las pruebas HTTP con Tomcat real verifican los cinco roles,
rechazo de CSRF ausente o inválido, retirada del registro de sesiones,
rechazo del identificador anterior y posibilidad de iniciar una nueva sesión.

La vista de acceso denegado acepta los reenvíos del método original para
conservar HTTP 403 también en POST. Un CSRF inválido antes del login devuelve
403 sin redirigir a una página que requiera autenticación.

```bash
./mvnw -Dtest=SecurityConfigTests,LogoutHttpTests test
```

Las pruebas por capturas quedan fuera del alcance actual. Existe una prueba
opcional `LogoutBrowserTests` que comprueba el historial en Chrome sin guardar
imágenes, vídeo ni trazas. Requiere Node, Chrome y Playwright ya instalado;
se habilita definiendo `HU01_PLAYWRIGHT_MODULE` con la ruta del paquete
Playwright. Sin esa variable se omite. Su ejecución está pendiente y no se
considera evidencia de un comportamiento ya verificado en navegador.

## Mantenimiento del catálogo de insumos

Administración y Nutrición pueden consultar, buscar, registrar y editar insumos
desde **Abastecimiento → Catálogo de insumos**. Cocina conserva acceso de solo
lectura a la proyección. Cada insumo requiere nombre, unidad base y merma
porcentual; la presentación y su cantidad son opcionales, pero se ingresan en
pareja. El costo por unidad base también es opcional.

La pantalla recibe la merma como porcentaje y la guarda como fracción (`10 %`
se almacena como `0.1000`). La cantidad de presentación se expresa en la unidad
base del insumo (por ejemplo, `1000 g` para una bolsa de `1 kg`). Un insumo
asociado a un detalle de menú no se puede eliminar; se puede editar para
mantener la referencia. Las altas, ediciones y eliminaciones se registran en la
bitácora.

## SC-03.3 — Proyección semanal y mensual de insumos

La opción «Proyección de insumos» está disponible para Administración,
Nutrición y Cocina. Calcula los siguientes 7 o 30 días calendario, incluyendo
hoy, y permite ajustar el margen de abastecimiento (10 % por defecto).
El margen se captura como porcentaje y se envía al cálculo como fracción
(`0.10` representa 10 %). La fecha inicial se obtiene en la zona
`America/El_Salvador`; el período mensual finaliza 29 días calendario después.

La proyección usa las asignaciones actuales: cuenta una ración por paciente
ingresado y por día en que coincidan una asignación activa y aprobada, un menú
activo y el período consultado. Suma `cantidad_por_racion × días vigentes` por
insumo (`neto`), divide por `1 − merma` (`bruto`) y aplica el margen
(`total_con_margen`). La cantidad de compra es el techo de
`total_con_margen / cantidad_presentacion`; los cálculos intermedios conservan
precisión decimal. Si falta merma, se toma como cero y se muestra una
advertencia. Si falta una cantidad de presentación válida, se advierte que no
se puede calcular la cantidad de compra.

La tabla muestra cantidades con formato local, convierte gramos a kilogramos y
mililitros a litros desde 1000 unidades, y conserva los valores crudos en los
atributos de datos de cada celda. El costo estimado se calcula usando el costo
por unidad base del insumo y su total proyectado con margen. Si falta un costo,
se indica en la fila y el presupuesto total se marca como no disponible. El
resumen también muestra pacientes incluidos y el promedio de raciones diarias,
calculado como raciones-día dividido entre los días del período.

La merma se almacena como fracción entre 0 y 1 (por ejemplo, `0.10` equivale a
10 %). La migración V9 convierte los porcentajes ya almacenados. Los insumos
con merma histórica de 100 % deben corregirse antes de migrar porque no existe
un bruto finito para esa merma. Esta proyección se basa en asignaciones; no es
un pronóstico aprendido del consumo histórico real.

## Interfaz provisional con Bootstrap

Las vistas utilizan Bootstrap 5.3.8 y ajustes propios en `static/css/styles.css`.
Los archivos compilados CSS y JavaScript se sirven desde
`static/vendor/bootstrap/`; no se requiere conexión con un CDN al abrir la
aplicación. La descarga se verificó con los hashes SHA-384 publicados en la
[documentación oficial de Bootstrap](https://getbootstrap.com/docs/5.3/getting-started/download/).
Se conservan las licencias MIT de Bootstrap y de Popper, incluido en el bundle.

El fragmento `templates/fragments/layout.html` define el menú lateral compartido.
En escritorio permanece a la izquierda; por debajo de 992 px se abre con el
botón «Menú», mediante el componente offcanvas de Bootstrap. La navegación
incluye Inicio, proyección de insumos para Administración, Nutrición y Cocina,
administración de usuarios exclusivamente para Administrador, la cuenta actual
y el cierre de sesión. La opción actual se resalta. Los accesos de navegación
del dashboard se concentran en este menú lateral.

La proyección exporta PDF con jsPDF/AutoTable y Excel con ExcelJS. Los bundles
y sus licencias se sirven localmente desde `static/vendor/exportacion/`; no se
usan CDN. Para regenerarlos después de actualizar las dependencias, ejecutar
`npm ci` y `npm run copiar:librerias-exportacion`.

Login, dashboard, formularios, listado de usuarios y páginas de error comparten
una presentación sencilla: fondo claro, tarjetas, formularios adaptables,
alertas, tabla con desplazamiento horizontal y botones consistentes. Es una
base visual provisional. Las reglas de autorización y las validaciones siguen
aplicándose en el servidor; las pruebas por capturas continúan fuera de alcance.

## Detención y persistencia

Para crear respaldos y restaurarlos en una base local vacía, consultar
[HU-03 — Respaldo y restauración MySQL](docs/HU-03-respaldo-restauracion.md).
El procedimiento incluye SHA-256 y una prueba independiente de recuperación
con datos sintéticos; no requiere borrar el volumen de desarrollo.

```bash
docker compose down
```

Este comando conserva los datos en el volumen `mysql_data`. No usar
`docker compose down -v` si se necesitan conservar: elimina el volumen.
Las variables de inicialización de MySQL solo se aplican cuando el volumen
está vacío. Cambiar contraseñas o el nombre de la base en `.env` no modifica
una base ya inicializada.

Tras modificar código, reconstruir la aplicación:

```bash
docker compose up -d --build app
```

## Pruebas

La construcción de la imagen omite la ejecución de pruebas porque las pruebas
actuales usan Testcontainers y requieren acceso a Docker. Ejecutarlas desde
el equipo con Java 21 y Docker disponibles:

```bash
./mvnw verify
```

El arranque con Compose no sustituye estas pruebas ni demuestra el cumplimiento
de las historias de usuario.
