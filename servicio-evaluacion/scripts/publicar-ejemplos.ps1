# Publica los eventos de ejemplo de /contratos/eventos/ejemplos en RabbitMQ,
# mas varias variaciones de PreguntaPublicada (CONTRATOS.md 7).
# Uso: .\publicar-ejemplos.ps1
$ErrorActionPreference = 'Stop'
$directorioScript = Split-Path -Parent $MyInvocation.MyCommand.Path
node (Join-Path $directorioScript 'publicar-ejemplos.js')
exit $LASTEXITCODE
