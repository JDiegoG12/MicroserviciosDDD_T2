#!/usr/bin/env bash
# Valida los contratos compartidos (/contratos) usando solo Docker.
#
# Dentro de un contenedor python:3.12-slim:
#   1. compila contratos/proto/catalogo/v1/catalogo_academico.proto con grpcio-tools;
#   2. valida cada ejemplo de contratos/eventos/ejemplos/ contra su JSON Schema.
# La lógica vive en scripts/validar_contratos.py (compartida con validar-contratos.ps1).
#
# Uso (desde cualquier carpeta):  ./scripts/validar-contratos.sh
# Sale con código 0 si todo es válido y distinto de 0 si algo falla.
set -euo pipefail

IMAGEN="python:3.12-slim"
DEPENDENCIAS="grpcio-tools==1.84.0 jsonschema==4.26.0 rfc3339-validator==0.1.4"

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# En Git Bash (Windows) Docker necesita la ruta como C:/... y hay que
# desactivar la conversión automática de rutas de MSYS.
if command -v cygpath >/dev/null 2>&1; then
  RAIZ_DOCKER="$(cygpath -m "$RAIZ")"
  export MSYS_NO_PATHCONV=1
else
  RAIZ_DOCKER="$RAIZ"
fi

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: Docker no está disponible. Inicia Docker y vuelve a intentarlo." >&2
  exit 2
fi

echo "Preparando contenedor $IMAGEN (instala: $DEPENDENCIAS)..."
docker run --rm \
  -v "${RAIZ_DOCKER}:/repo:ro" \
  -w /repo \
  -e PIP_DISABLE_PIP_VERSION_CHECK=1 \
  -e PIP_ROOT_USER_ACTION=ignore \
  -e PYTHONIOENCODING=utf-8 \
  -e PYTHONDONTWRITEBYTECODE=1 \
  "$IMAGEN" \
  sh -c "pip install --quiet --no-cache-dir $DEPENDENCIAS && python scripts/validar_contratos.py"
