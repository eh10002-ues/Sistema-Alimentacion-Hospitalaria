#!/usr/bin/env bash
# HU-03: punto de entrada portable; la lógica usa solo la biblioteca estándar.
set -euo pipefail
exec python3 "$(dirname "$0")/operaciones.py" respaldo "$@"
