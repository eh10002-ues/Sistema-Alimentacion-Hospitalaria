#!/usr/bin/env bash
# HU-03: nunca elimina ni recrea una base de datos.
set -euo pipefail
exec python3 "$(dirname "$0")/operaciones.py" restaurar "$@"
