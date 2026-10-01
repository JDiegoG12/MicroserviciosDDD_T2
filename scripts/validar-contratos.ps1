<#
.SYNOPSIS
  Valida los contratos compartidos (/contratos) usando solo Docker.

.DESCRIPTION
  Equivalente para Windows de scripts/validar-contratos.sh. Dentro de un
  contenedor python:3.12-slim:
    1. compila contratos/proto/catalogo/v1/catalogo_academico.proto con grpcio-tools;
    2. valida cada ejemplo de contratos/eventos/ejemplos/ contra su JSON Schema.
  La lógica vive en scripts/validar_contratos.py.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\scripts\validar-contratos.ps1
#>

# Nota: no se usa $ErrorActionPreference = "Stop" porque en PowerShell 5.1 convierte
# la salida de error de los ejecutables nativos (docker) en errores terminantes.
$imagen = 'python:3.12-slim'
$dependencias = 'grpcio-tools==1.84.0 jsonschema==4.26.0 rfc3339-validator==0.1.4'

# Muestra bien las tildes que imprime el contenedor (UTF-8).
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'ERROR: Docker no está disponible. Inicia Docker y vuelve a intentarlo.'
    exit 2
}

Write-Host "Preparando contenedor $imagen (instala: $dependencias)..."
docker run --rm `
    -v "${raiz}:/repo:ro" `
    -w /repo `
    -e PIP_DISABLE_PIP_VERSION_CHECK=1 `
    -e PIP_ROOT_USER_ACTION=ignore `
    -e PYTHONIOENCODING=utf-8 `
    -e PYTHONDONTWRITEBYTECODE=1 `
    $imagen `
    sh -c "pip install --quiet --no-cache-dir $dependencias && python scripts/validar_contratos.py"
exit $LASTEXITCODE
