# HU-03 — Evidencia de recuperación

Ejecución UTC: `20261107_070059Z`. Resultado: **satisfactorio**.

- MySQL 8.4 en dos servidores descartables y una red independiente.
- 45 comprobaciones satisfactorias; 17 tablas con contenido idéntico.
- Claves foráneas y relaciones de paciente, dieta, menú e insumo recuperadas.
- Flyway/Hibernate validaron la aplicación restaurada; login y consulta de proyección satisfactorios.
- SHA-256: `8f88c58568c36cfd20a83b81ffd1038e5091b072694a1e4444e64c1627a0db02`.
- Suite Java: 592 casos, cero fallos, cero errores y uno omitido (prueba opcional de navegador).
- Scripts HU-03: seis pruebas unitarias correctas; sintaxis Bash y revisión del diff correctas.

El archivo SQL contiene exclusivamente datos sintéticos creados en esta prueba,
incluidos hashes de cuentas efímeras. No contiene contraseñas en texto plano ni
datos de la base de desarrollo. Las credenciales aleatorias y los contenedores
se eliminaron al terminar. `resultado.json` conserva casos y conteos.

Las ejecuciones iniciales detectaron problemas de posición del descriptor de
archivo y de codificación del cliente. Ambos se corrigieron antes de esta
recuperación satisfactoria; la suite local cubre la entrega del archivo desde
el primer byte. No se modificaron migraciones para superar la prueba.

Reproducir desde la raíz:

```bash
bash scripts/mysql/prueba-recuperacion.sh
python3 -m unittest discover -s tests/hu03 -p 'test_*.py' -v
```

La evidencia es una instantánea del código y esquema de esta ejecución. Repetir
la recuperación después de cambios de esquema. La comprobación del hash debe
ejecutarse desde este directorio con `shasum -a 256 -c ARCHIVO.sql.sha256`.
