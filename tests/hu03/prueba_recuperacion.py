"""HU-03: prueba real con dos MySQL independientes y la aplicación del repositorio."""
import argparse
from datetime import datetime, timezone
import hashlib
import html
import http.cookiejar
import importlib.util
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

RAIZ = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('hu03_operaciones', RAIZ / 'scripts/mysql/operaciones.py')
ops = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ops)


def ejecutar(cmd, **kwargs):
    resultado = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kwargs)
    if resultado.returncode:
        raise RuntimeError('Falló un comando de prueba. No se muestran argumentos ni errores sensibles.')
    return resultado.stdout.decode().strip()


class SinRedireccion(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args):
        return None


def peticion(cliente, url, datos=None):
    try:
        respuesta = cliente.open(urllib.request.Request(url,
            data=urllib.parse.urlencode(datos).encode() if datos is not None else None), timeout=5)
    except urllib.error.HTTPError as error:
        respuesta = error
    with respuesta:
        return respuesta.status, respuesta.read().decode(), respuesta.headers


def esperar(condicion, mensaje, limite=150):
    fin = time.monotonic() + limite
    while time.monotonic() < fin:
        try:
            if condicion():
                return
        except (ops.ErrorOperacion, RuntimeError, OSError):
            pass
        time.sleep(2)
    raise RuntimeError(mensaje)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--salida', default=str(RAIZ / 'backups/hu03-prueba'))
    args = parser.parse_args()
    os.umask(0o077)
    fecha = datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%SZ')
    identificador = 'hu03-' + secrets.token_hex(5)
    salida = Path(args.salida).resolve() / identificador
    salida.mkdir(parents=True)
    resultados = []
    recursos = []
    imagen = identificador + ':prueba'
    red = identificador

    def comprobar(nombre, condicion=True):
        resultados.append({'caso': nombre, 'correcto': bool(condicion)})
        print(('OK: ' if condicion else 'FALLO: ') + nombre, flush=True)
        if not condicion:
            raise RuntimeError(nombre)

    def conexion(contenedor, base):
        return ops.Conexion(argparse.Namespace(contenedor=contenedor, base=base,
                             host='127.0.0.1', puerto=3306, usuario=None))

    def cli(accion, contenedor, base, extras, codigo=0):
        comando = [sys.executable, str(RAIZ / 'scripts/mysql/operaciones.py'), accion,
                   '--contenedor', contenedor, '--base', base] + extras
        r = subprocess.run(comando, capture_output=True, timeout=120)
        if r.returncode != codigo:
            print(f'Código recibido: {r.returncode}; esperado: {codigo}', flush=True)
            for linea in r.stderr.decode(errors='replace').splitlines():
                if linea.startswith('Error HU-03:'):
                    print(linea, flush=True)
        comprobar(f'{accion}: código esperado {codigo}', r.returncode == codigo)
        return r

    try:
        # No utiliza Compose, .env, volúmenes o puertos del entorno de desarrollo.
        ejecutar(['docker', 'network', 'create', '--label', 'hu03.prueba=' + identificador, red])
        comprobar('Red exclusiva de prueba creada')
        print('Construyendo la aplicación actual para validar Flyway y Hibernate…', flush=True)
        ejecutar(['docker', 'build', '-t', imagen, str(RAIZ)], timeout=600)
        comprobar('Compilación de la imagen de aplicación')
        with tempfile.TemporaryDirectory(prefix='hu03-config-') as temporal:
            clave_db = secrets.token_hex(24)
            clave_admin = 'Aa1' + secrets.token_hex(24)
            env_db = Path(temporal) / 'mysql.env'
            env_app = Path(temporal) / 'app.env'
            for tipo, base in [('origen', 'hu03_origen'), ('destino', 'hu03_restaurada')]:
                nombre = identificador + '-' + tipo
                env_db.write_text(f'MYSQL_DATABASE={base}\nMYSQL_USER=hu03_app\n'
                                  f'MYSQL_PASSWORD={clave_db}\nMYSQL_ROOT_PASSWORD={secrets.token_hex(24)}\n')
                ejecutar(['docker', 'run', '-d', '--name', nombre, '--label', 'hu03.prueba=' + identificador,
                          '--network', red, '--env-file', str(env_db), 'mysql:8.4'])
                recursos.append(nombre)
                esperar(lambda: conexion(nombre, base).consultar('SELECT 1') == '1',
                        'MySQL descartable no quedó disponible.')
            origen = identificador + '-origen'
            destino = identificador + '-destino'
            comprobar('Dos servidores MySQL 8.4 independientes disponibles')

            def arrancar_app(nombre, servidor, base, inicializar=False):
                env_app.write_text(f'SPRING_DATASOURCE_URL=jdbc:mysql://{servidor}:3306/{base}\n'
                    f'SPRING_DATASOURCE_USERNAME=hu03_app\nSPRING_DATASOURCE_PASSWORD={clave_db}\n' +
                    (f'SPRING_PROFILES_ACTIVE=inicializacion\nAPP_ADMIN_USERNAME=hu03.admin\n'
                     f'APP_ADMIN_PASSWORD={clave_admin}\nAPP_ADMIN_FULL_NAME=Administrador sintético HU-03\n'
                     if inicializar else ''))
                ejecutar(['docker', 'run', '-d', '--name', nombre, '--label', 'hu03.prueba=' + identificador,
                          '--network', red, '--env-file', str(env_app), '-p', '127.0.0.1::8080', imagen])
                recursos.append(nombre)
                puerto = ejecutar(['docker', 'port', nombre, '8080/tcp']).rsplit(':', 1)[1]
                url = 'http://127.0.0.1:' + puerto
                esperar(lambda: peticion(urllib.request.build_opener(), url + '/login')[0] == 200,
                        'La aplicación descartable no arrancó (revise migraciones o validación).')
                return url

            inicial = identificador + '-app-origen'
            arrancar_app(inicial, origen, 'hu03_origen', True)
            ejecutar(['docker', 'stop', inicial])
            comprobar('Migraciones originales aplicadas por Flyway e inicialización sintética')
            con_origen = conexion(origen, 'hu03_origen')
            with (RAIZ / 'tests/hu03/datos_sinteticos.sql').open('rb') as fixture:
                con_origen.ejecutar('mysql', ['--database=hu03_origen'], entrada=fixture)
            comprobar('Fixture sintético completo insertado sin modificar migraciones')
            nombre = 'alimentacion_hospitalaria_' + fecha + '.sql'
            cli('respaldo', origen, 'hu03_origen', ['--directorio', str(salida), '--nombre', nombre])
            respaldo = salida / nombre
            checksum = Path(str(respaldo) + '.sha256')
            comprobar('Respaldo no vacío y permisos 0600', respaldo.stat().st_size > 0 and
                       respaldo.stat().st_mode & 0o777 == 0o600)
            comprobar('SHA-256 coincide', hashlib.sha256(respaldo.read_bytes()).hexdigest() ==
                       checksum.read_text().split()[0])
            cli('respaldo', origen, 'hu03_origen', ['--directorio', str(salida), '--nombre', nombre], 4)
            cli('respaldo', origen, 'hu03_origen', ['--directorio', str(salida), '--usuario', 'hu03_inexistente'], 3)
            cli('respaldo', origen, 'hu03_ausente', ['--directorio', str(salida)], 3)
            cli('respaldo', destino, 'hu03_restaurada', ['--directorio', str(salida)], 3)
            comprobar('Sin archivos parciales tras errores', not list(salida.glob('.hu03-*')))
            comun = ['--archivo', str(respaldo), '--entorno', 'prueba']
            cli('restaurar', destino, 'hu03_restaurada', comun + ['--confirmar', 'otra_base'], 5)
            cli('restaurar', destino, 'hu03_restaurada', comun, 2)
            cli('restaurar', destino, 'hu03_restaurada', comun + ['--host', 'servidor-remoto'], 2)
            cli('restaurar', destino, 'produccion', comun + ['--confirmar', 'produccion'], 2)
            alterado = salida / 'alterado.sql'
            alterado.write_bytes(respaldo.read_bytes()[:-100])
            suma_alterado = Path(str(alterado) + '.sha256')
            suma_alterado.write_text(checksum.read_text().replace(nombre, alterado.name))
            cli('restaurar', destino, 'hu03_restaurada', ['--archivo', str(alterado),
                '--entorno', 'prueba', '--confirmar', 'hu03_restaurada'], 4)
            suma_alterado.unlink()
            cli('restaurar', destino, 'hu03_restaurada', ['--archivo', str(alterado),
                '--entorno', 'prueba', '--confirmar', 'hu03_restaurada'], 4)
            alterado.unlink()
            con_destino = conexion(destino, 'hu03_restaurada')
            comprobar('Destino sigue vacío tras rechazos', con_destino.consultar(
                'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()') == '0')
            cli('restaurar', destino, 'hu03_restaurada', comun + ['--confirmar', 'hu03_restaurada'])
            cli('restaurar', destino, 'hu03_restaurada', comun + ['--confirmar', 'hu03_restaurada'], 5)
            tablas = con_origen.consultar("SELECT TABLE_NAME FROM information_schema.TABLES "
                "WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME").splitlines()
            comparar = {}
            for tabla in tablas:
                assert re.fullmatch(r'[A-Za-z0-9_]+', tabla)
                consulta = f'SELECT * FROM `{tabla}` ORDER BY 1'
                a = con_origen.consultar(consulta)
                b = con_destino.consultar(consulta)
                comprobar('Contenido idéntico: ' + tabla, a == b)
                comparar[tabla] = int(con_destino.consultar(f'SELECT COUNT(*) FROM `{tabla}`'))
            claves = "SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME " \
                     "FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() " \
                     "AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME,COLUMN_NAME"
            comprobar('Relaciones recuperadas', con_origen.consultar(claves) == con_destino.consultar(claves))
            codigo = "SELECT p.codigo,m.tipo_dieta,i.nombre FROM pacientes p " \
                     "JOIN asignaciones_menu a ON a.paciente_id=p.id JOIN menus m ON m.id=a.menu_id " \
                     "JOIN menu_detalles d ON d.menu_id=m.id JOIN menu_detalle_insumos x ON x.menu_detalle_id=d.id " \
                     "JOIN insumos i ON i.id=x.insumo_id"
            comprobar('Paciente, dieta, menú e insumo relacionados', 'HU03-PAC-001' in con_destino.consultar(codigo))
            url = arrancar_app(identificador + '-app-restaurada', destino, 'hu03_restaurada')
            comprobar('Aplicación restaurada arranca y Flyway valida su historial')
            cliente = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),
                                                   SinRedireccion())
            _, login, _ = peticion(cliente, url + '/login')
            token = None
            for campo in re.findall(r'<input\b[^>]*>', login):
                if 'name="_csrf"' in campo:
                    token = html.unescape(re.search(r'value="([^"]+)"', campo)[1])
            comprobar('Formulario restaurado conserva CSRF', token is not None)
            estado, _, cabeceras = peticion(cliente, url + '/login',
                {'username': 'hu03.admin', 'password': clave_admin, '_csrf': token})
            comprobar('Autenticación con el usuario recuperado', estado == 302 and
                       cabeceras.get('Location', '').endswith('/dashboard'))
            estado, pagina, _ = peticion(cliente, url + '/insumos/proyeccion')
            comprobar('Consulta funcional restaurada', estado == 200 and 'Arroz sintético HU-03' in pagina)
            evidencia = {'historia': 'HU-03', 'fecha_utc': fecha, 'mysql': '8.4',
                'respaldo': nombre, 'sha256': checksum.read_text().split()[0],
                'tablas_y_conteos': comparar, 'resultados': resultados}
            (salida / 'resultado.json').write_text(json.dumps(evidencia, ensure_ascii=False, indent=2) + '\n')
            print(f'Evidencia sintética conservada en {salida}', flush=True)
    finally:
        for recurso in reversed(recursos):
            # Solo eliminar recursos identificados con la etiqueta aleatoria de ESTA ejecución.
            etiqueta = subprocess.run(['docker', 'inspect', '--format',
                '{{ index .Config.Labels "hu03.prueba" }}', recurso], capture_output=True, text=True)
            if etiqueta.returncode == 0 and etiqueta.stdout.strip() == identificador:
                ejecutar(['docker', 'rm', '-f', '-v', recurso])
        subprocess.run(['docker', 'network', 'rm', red], capture_output=True)
        subprocess.run(['docker', 'image', 'rm', imagen], capture_output=True)


if __name__ == '__main__':
    try:
        main()
    except (Exception, KeyboardInterrupt) as error:
        print('Falló la prueba HU-03; tipo: ' + type(error).__name__ +
              '. Revise el último caso indicado. No se muestran secretos.', file=sys.stderr)
        if isinstance(error, (ops.ErrorOperacion, RuntimeError)):
            print(str(error), file=sys.stderr)
        sys.exit(1)
