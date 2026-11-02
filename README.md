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
y la revocación de sesiones corresponden a pasos posteriores.

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
La autorización por módulos y la navegación por rol se incorporarán en su etapa.

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
   La administración de usuarios y la navegación por rol se implementarán en
   los siguientes pasos; este incremento permite autenticar la cuenta creada.
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

## Detención y persistencia

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
