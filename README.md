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

4. Abrir <http://localhost:8080/login>. La configuración de seguridad actual
   utiliza el usuario temporal `user` y una contraseña generada que aparece en
   el registro de arranque de `app`. Esta autenticación provisional se sustituirá
   al implementar usuarios y roles. No compartir registros que contengan esa contraseña.

MySQL 8.4 queda accesible desde el equipo en `127.0.0.1:3307` y desde la
aplicación en `db:3306`. La aplicación espera a que MySQL acepte consultas con
su usuario antes de arrancar. Se pueden cambiar `APP_PORT` y `MYSQL_PORT` en
`.env` si los puertos están ocupados. Ambos servicios se publican solo en la
interfaz local del equipo.

La imagen compila con Java 21 y el Maven Wrapper del repositorio; la ejecución
usa Java 21 y un usuario sin privilegios. No incorpora configuración local ni
archivos de apoyo del agente. Flyway conserva la gestión del esquema y
Hibernate la validación; actualmente no hay migraciones SQL ni datos iniciales.

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
