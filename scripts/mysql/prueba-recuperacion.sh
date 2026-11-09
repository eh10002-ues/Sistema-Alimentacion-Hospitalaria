#!/usr/bin/env bash
set -euo pipefail
exec python3 "$(dirname "$0")/../../tests/hu03/prueba_recuperacion.py" "$@"
