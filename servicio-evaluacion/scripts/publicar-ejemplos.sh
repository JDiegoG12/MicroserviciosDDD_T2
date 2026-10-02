#!/usr/bin/env bash
# Publica los eventos de ejemplo de /contratos/eventos/ejemplos en RabbitMQ,
# mas varias variaciones de PreguntaPublicada (CONTRATOS.md 7).
# Uso: ./publicar-ejemplos.sh
set -euo pipefail
DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec node "${DIR_SCRIPT}/publicar-ejemplos.js" "$@"
