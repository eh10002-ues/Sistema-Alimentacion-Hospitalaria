"""Pruebas sin Docker: integridad antes de conexión y publicación sin sobrescritura."""
import argparse
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('operaciones',
    Path(__file__).resolve().parents[2] / 'scripts/mysql/operaciones.py')
ops = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ops)


class Origen:
    def comprobar(self):
        pass

    def consultar(self, consulta):
        return '0' if 'ENGINE <>' in consulta or 'ROUTINES' in consulta else '1'

    def ejecutar(self, herramienta, opciones, salida):
        # Emula un hijo que escribe directamente al descriptor, sin buffers de Python.
        os.write(salida.fileno(), b'-- SQL sintetico\n' + b'-- contenido\n' * 20 +
                 b'-- Dump completed on 2026-01-01\n')


class Destino:
    def comprobar(self):
        raise AssertionError('No debe conectarse antes de validar la integridad.')


class OperacionesTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.ruta = Path(self.tmp.name).resolve()
        self.args = argparse.Namespace(base='hu03_prueba', directorio=self.tmp.name,
                                        nombre='alimentacion_hospitalaria_prueba.sql')

    def respaldo(self):
        ops.respaldar(self.args, Origen())
        return self.ruta / self.args.nombre

    def test_encabezado_precede_la_salida_del_cliente_y_no_sobrescribe(self):
        archivo = self.respaldo()
        contenido = archivo.read_bytes()
        self.assertTrue(contenido.startswith(b'-- HU-03: respaldo de una sola base MySQL 8.4\n'))
        with self.assertRaises(ops.ErrorOperacion) as error:
            self.respaldo()
        self.assertEqual(error.exception.codigo, 4)
        self.assertEqual(archivo.read_bytes(), contenido)

    def test_fallo_de_dump_no_publica_archivos(self):
        with patch.object(Origen, 'ejecutar', side_effect=ops.ErrorOperacion('Fallo simulado')):
            with self.assertRaises(ops.ErrorOperacion):
                self.respaldo()
        self.assertEqual(list(self.ruta.iterdir()), [])

    def test_colision_concurrente_preserva_el_archivo_ajeno(self):
        original = os.link
        archivo = self.ruta / self.args.nombre

        def colision(origen, destino):
            if Path(destino) == archivo:
                archivo.write_bytes(b'contenido ajeno')
            original(origen, destino)

        with patch.object(ops.os, 'link', side_effect=colision):
            with self.assertRaises(FileExistsError):
                self.respaldo()
        self.assertEqual(archivo.read_bytes(), b'contenido ajeno')
        self.assertFalse(Path(str(archivo) + '.sha256').exists())

    def test_nombre_no_puede_escapar_del_directorio(self):
        self.args.nombre = '../archivo.sql'
        with self.assertRaises(ops.ErrorOperacion):
            self.respaldo()
        self.assertEqual(list(self.ruta.iterdir()), [])

    def test_truncamiento_se_rechaza_antes_de_conectar(self):
        archivo = self.respaldo()
        archivo.write_bytes(archivo.read_bytes()[:-10])
        args = argparse.Namespace(host='127.0.0.1', base='hu03_prueba', archivo=str(archivo), suma=None)
        with self.assertRaises(ops.ErrorOperacion) as error:
            ops.restaurar(args, Destino())
        self.assertEqual(error.exception.codigo, 4)

    def test_cliente_recibe_el_archivo_desde_el_primer_byte(self):
        archivo = self.respaldo()
        args = argparse.Namespace(host='127.0.0.1', puerto=3306, base='hu03_prueba',
            entorno='prueba', archivo=str(archivo), suma=None, confirmar='hu03_prueba')
        recibidos = []

        class Importador:
            destino = 'prueba'

            def comprobar(self):
                pass

            def consultar(self, consulta):
                return '0'

            def ejecutar(self, herramienta, opciones, entrada):
                recibidos.append(os.read(entrada.fileno(), 4096))

        ops.restaurar(args, Importador())
        self.assertEqual(recibidos[0], archivo.read_bytes())


if __name__ == '__main__':
    unittest.main()
