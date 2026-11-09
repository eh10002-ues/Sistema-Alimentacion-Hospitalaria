"""HU-03: respaldo de una base MySQL 8.4 y recuperación en una base vacía."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timezone

RAIZ = Path(__file__).resolve().parents[2]

# El secreto existente del contenedor solo se escribe en un archivo temporal 0600.
# No se interpola en argumentos, consultas, mensajes ni archivos del equipo.
CLIENTE = r'''set -eu
umask 077
config=$(mktemp /tmp/hu03-cliente.XXXXXX)
trap 'rm -f "$config"' EXIT HUP INT TERM
: "${MYSQL_PASSWORD:?}"
case "$MYSQL_PASSWORD" in *'
'*|*''*) exit 64;; esac
clave=$(printf '%s' "$MYSQL_PASSWORD" | sed 's/\\/\\\\/g; s/"/\\"/g')
printf '[client]\npassword="%s"\n' "$clave" > "$config"
unset clave
herramienta=$1
host=$2
puerto=$3
usuario=${4:-${MYSQL_USER:?}}
shift 4
"$herramienta" --defaults-extra-file="$config" --protocol=TCP \
    --host="$host" --port="$puerto" --user="$usuario" "$@"
'''


class ErrorOperacion(Exception):
    def __init__(self, mensaje, codigo=3):
        super().__init__(mensaje)
        self.codigo = codigo


def identificador(valor):
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]{0,63}', valor):
        raise argparse.ArgumentTypeError('Use un identificador de base simple, de hasta 64 caracteres.')
    if valor.lower() in {'mysql', 'sys', 'information_schema', 'performance_schema'}:
        raise argparse.ArgumentTypeError('No se permiten bases del sistema.')
    return valor


class Conexion:
    def __init__(self, args):
        self.args = args
        if args.contenedor:
            self.prefijo = ['docker', 'exec', '-i', args.contenedor]
            self.destino = f'contenedor={args.contenedor}'
        else:
            self.prefijo = ['docker', 'compose', '--project-directory', str(RAIZ)]
            if args.compose:
                self.prefijo += ['-f', str(Path(args.compose).resolve())]
            if args.proyecto:
                self.prefijo += ['-p', args.proyecto]
            self.prefijo += ['exec', '-T', args.servicio]
            self.destino = f'proyecto={args.proyecto or RAIZ.name}, servicio={args.servicio}'

    def comando(self, herramienta, opciones):
        return self.prefijo + ['sh', '-c', CLIENTE, 'hu03', herramienta,
                               self.args.host, str(self.args.puerto), self.args.usuario or '',
                               '--default-character-set=utf8mb4'] + opciones

    def ejecutar(self, herramienta, opciones, entrada=None, salida=None):
        try:
            resultado = subprocess.run(self.comando(herramienta, opciones), stdin=entrada,
                                       stdout=salida or subprocess.PIPE,
                                       stderr=subprocess.PIPE, timeout=1800)
        except (OSError, subprocess.TimeoutExpired):
            raise ErrorOperacion('No fue posible ejecutar Docker/MySQL o se agotó el tiempo.') from None
        if resultado.returncode:
            # Los errores originales pueden incluir consultas, rutas o valores sensibles.
            codigo = re.search(rb'ERROR (\d+)', resultado.stderr)
            detalle = ' Código MySQL: ' + codigo[1].decode() + '.' if codigo else ''
            raise ErrorOperacion('MySQL/Docker rechazó la operación. Compruebe servicio, credenciales, '
                                 'permisos, espacio y compatibilidad del respaldo.' + detalle)
        return resultado.stdout

    def consultar(self, consulta):
        return self.ejecutar('mysql', ['--batch', '--skip-column-names',
                            '--database=' + self.args.base, '--execute=' + consulta]).decode().strip()

    def comprobar(self):
        if not self.consultar('SELECT VERSION()').startswith('8.4.'):
            raise ErrorOperacion('Se requiere un servidor MySQL 8.4 para este procedimiento.')


def sha256(archivo):
    digest = hashlib.sha256()
    for bloque in iter(lambda: archivo.read(1024 * 1024), b''):
        digest.update(bloque)
    return digest.hexdigest()


def respaldar(args, conexion):
    conexion.comprobar()
    filtro = "TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE'"
    if conexion.consultar(f'SELECT COUNT(*) FROM information_schema.TABLES WHERE {filtro}') == '0':
        raise ErrorOperacion('La base origen no contiene tablas; no se generó un respaldo.')
    if conexion.consultar(f"SELECT COUNT(*) FROM information_schema.TABLES WHERE {filtro} AND ENGINE <> 'InnoDB'") != '0':
        raise ErrorOperacion('El procedimiento consistente requiere tablas InnoDB.')
    if conexion.consultar('SELECT (SELECT COUNT(*) FROM information_schema.ROUTINES '
                          'WHERE ROUTINE_SCHEMA=DATABASE()) + (SELECT COUNT(*) FROM '
                          'information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE())') != '0':
        raise ErrorOperacion('Se detectaron rutinas o eventos. Este procedimiento no los omite '
                             'silenciosamente: requieren ampliar y probar sus permisos de respaldo.')
    destino = Path(args.directorio).expanduser().resolve()
    destino.mkdir(parents=True, exist_ok=True, mode=0o700)
    nombre = args.nombre or ('alimentacion_hospitalaria_' +
                             datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S_%fZ') + '.sql')
    if not re.fullmatch(r'alimentacion_hospitalaria_[A-Za-z0-9_-]+\.sql', nombre):
        raise ErrorOperacion('Nombre inválido: use alimentacion_hospitalaria_IDENTIFICADOR.sql.', 2)
    final = destino / nombre
    suma = destino / (nombre + '.sha256')
    if final.exists() or final.is_symlink() or suma.exists() or suma.is_symlink():
        raise ErrorOperacion('Ya existe el respaldo o su suma. No se sobrescribió ningún archivo.', 4)
    publicados = []
    try:
        with tempfile.TemporaryDirectory(prefix='.hu03-', dir=destino) as temporal:
            sql = Path(temporal) / nombre
            with sql.open('xb') as salida:
                salida.write(b'-- HU-03: respaldo de una sola base MySQL 8.4\n')
                salida.flush()
                conexion.ejecutar('mysqldump', ['--single-transaction', '--quick', '--skip-lock-tables',
                    '--no-tablespaces', '--set-gtid-purged=OFF', '--hex-blob', '--triggers',
                    '--skip-add-drop-table', args.base], salida=salida)
            with sql.open('rb') as archivo:
                archivo.seek(max(0, sql.stat().st_size - 512))
                completo = b'-- Dump completed on ' in archivo.read()
            if sql.stat().st_size < 100 or not completo:
                raise ErrorOperacion('El respaldo está vacío o incompleto; no se publicó.')
            with sql.open('rb') as archivo:
                digest = sha256(archivo)
            checksum = Path(temporal) / suma.name
            checksum.write_text(f'{digest}  {nombre}\n', encoding='ascii')
            os.chmod(sql, 0o600)
            os.chmod(checksum, 0o600)
            # link es exclusivo y atómico: tampoco sobrescribe ante otra ejecución concurrente.
            os.link(sql, final)
            publicados.append(final)
            os.link(checksum, suma)
            publicados.append(suma)
    except BaseException:
        for archivo in publicados:
            archivo.unlink(missing_ok=True)
        raise
    print(f'Respaldo correcto: {final}\nSHA-256: {digest}\nSuma: {suma}')


def restaurar(args, conexion):
    if args.host not in {'127.0.0.1', 'localhost', '::1'}:
        raise ErrorOperacion('La restauración solo admite MySQL local al contenedor.', 2)
    if re.search(r'prod|production|produccion', args.base, re.IGNORECASE):
        raise ErrorOperacion('Destino identificado como productivo: restauración rechazada.', 2)
    origen = Path(args.archivo).expanduser().resolve()
    suma = Path(args.suma).expanduser().resolve() if args.suma else Path(str(origen) + '.sha256')
    try:
        contenido = suma.read_text(encoding='ascii').strip()
        esperado, nombre = contenido.split('  ', 1)
        if not re.fullmatch('[a-f0-9]{64}', esperado) or nombre != origen.name:
            raise ValueError
    except (OSError, UnicodeError, ValueError):
        raise ErrorOperacion('Suma SHA-256 ausente o con formato/nombre inválido.', 4) from None
    # Se valida y restaura la misma copia privada: no se vuelve a abrir el archivo recibido.
    # Sin buffer: el descriptor heredado por mysql debe empezar exactamente en byte 0.
    with tempfile.TemporaryFile(buffering=0) as copia:
        with origen.open('rb') as archivo:
            for bloque in iter(lambda: archivo.read(1024 * 1024), b''):
                copia.write(bloque)
        copia.seek(0)
        if sha256(copia) != esperado:
            raise ErrorOperacion('SHA-256 no coincide: archivo alterado o truncado. No se importó.', 4)
        copia.seek(0)
        if copia.readline() != b'-- HU-03: respaldo de una sola base MySQL 8.4\n':
            raise ErrorOperacion('Se requiere un respaldo generado por respaldo.sh.', 4)
        copia.seek(0)
        conexion.comprobar()
        print(f'Destino: {conexion.destino}, host={args.host}, puerto={args.puerto}, '
              f'base={args.base}, entorno={args.entorno}', flush=True)
        objetos = ('SELECT (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()) '
                   '+ (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE()) '
                   '+ (SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE())')
        if conexion.consultar(objetos) != '0':
            raise ErrorOperacion('La base destino no está vacía. No se eliminó ni reemplazó información.', 5)
        if args.confirmar is None:
            if not sys.stdin.isatty():
                raise ErrorOperacion('Confirme el destino con --confirmar NOMBRE_BASE o use una terminal.', 2)
            confirmacion = input('Escriba el nombre de la base destino para restaurar: ')
        else:
            confirmacion = args.confirmar
        if confirmacion != args.base:
            raise ErrorOperacion('Confirmación incorrecta; restauración cancelada.', 5)
        try:
            conexion.ejecutar('mysql', ['--binary-mode', '--local-infile=0',
                              '--database=' + args.base], entrada=copia)
        except ErrorOperacion as error:
            raise ErrorOperacion('Falló la importación; el destino puede estar parcialmente cargado. '
                                 'No reintente hasta revisar o sustituir la base descartable. ' +
                                 str(error)) from None
        print('Restauración correcta. Verifique registros y arranque antes de utilizar la base.')


def parser():
    raiz = argparse.ArgumentParser(description=__doc__)
    acciones = raiz.add_subparsers(dest='accion', required=True)
    for accion in ('respaldo', 'restaurar'):
        p = acciones.add_parser(accion)
        p.add_argument('--base', required=True, type=identificador)
        seleccion = p.add_mutually_exclusive_group()
        seleccion.add_argument('--contenedor', help='Contenedor MySQL explícito; alternativa a Compose.')
        seleccion.add_argument('--compose', help='Archivo Compose alternativo.')
        p.add_argument('--proyecto', help='Nombre del proyecto Compose.')
        p.add_argument('--servicio', default='db')
        p.add_argument('--host', default='127.0.0.1', help='Host visto desde el contenedor MySQL.')
        p.add_argument('--puerto', type=int, default=3306)
        p.add_argument('--usuario', help='Por defecto MYSQL_USER del contenedor; misma contraseña.')
        if accion == 'respaldo':
            p.add_argument('--directorio', default=os.environ.get('HU03_BACKUP_DIR', str(RAIZ / 'backups')))
            p.add_argument('--nombre', help='Nombre opcional, sin rutas; nunca se sobrescribe.')
        else:
            p.add_argument('--archivo', required=True)
            p.add_argument('--suma', help='Por defecto ARCHIVO.sha256.')
            p.add_argument('--entorno', required=True, choices=['local', 'prueba'])
            p.add_argument('--confirmar', help='Confirmación explícita para ejecución sin terminal.')
    return raiz


def main():
    os.umask(0o077)
    args = parser().parse_args()
    try:
        if not 1 <= args.puerto <= 65535:
            raise ErrorOperacion('Puerto inválido.', 2)
        funcion = respaldar if args.accion == 'respaldo' else restaurar
        funcion(args, Conexion(args))
        return 0
    except ErrorOperacion as error:
        print(f'Error HU-03: {error}', file=sys.stderr)
        return error.codigo
    except OSError:
        print('Error HU-03: no se pudo acceder al archivo/directorio o publicar sin sobrescribir.', file=sys.stderr)
        return 3
    except KeyboardInterrupt:
        print('Operación HU-03 interrumpida.', file=sys.stderr)
        return 130


if __name__ == '__main__':
    sys.exit(main())
