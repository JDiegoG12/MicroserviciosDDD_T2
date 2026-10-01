"""Valida los contratos compartidos del monorepo.

Este módulo lo ejecutan ``scripts/validar-contratos.sh`` y
``scripts/validar-contratos.ps1`` dentro de un contenedor ``python:3.12-slim``
que ya tiene instalados ``grpcio-tools``, ``jsonschema`` y
``rfc3339-validator``. No está pensado para ejecutarse directamente en el equipo.

Comprueba:
    1. Que ``contratos/proto/catalogo/v1/catalogo_academico.proto`` compile con
       ``grpc_tools.protoc`` (Python y stubs gRPC).
    2. Que cada JSON Schema de ``contratos/eventos`` sea un esquema válido de
       draft 2020-12.
    3. Que cada ejemplo de ``contratos/eventos/ejemplos`` sea válido contra su
       esquema (se emparejan por nombre: ``x.v1.ejemplo.json`` con
       ``x.v1.schema.json``) y que todo esquema tenga su ejemplo.
    4. Que cada ejemplo "con campo extra" (``x.v1.ejemplo-campo-extra.json``)
       sea RECHAZADO por su esquema (los esquemas son estrictos para el
       productor, CONTRATOS.md §7.8); si el esquema lo aceptara, sería un
       error porque dejaría de ser estricto.

Termina con código 0 si todo es correcto y 1 si hay algún error.
"""

import json
import sys
import tempfile
from importlib import resources
from pathlib import Path

from grpc_tools import protoc
from jsonschema import Draft202012Validator
from jsonschema.exceptions import SchemaError

RAIZ = Path(__file__).resolve().parent.parent
CARPETA_PROTO = RAIZ / "contratos" / "proto"
ARCHIVO_PROTO = Path("catalogo/v1/catalogo_academico.proto")
CARPETA_EVENTOS = RAIZ / "contratos" / "eventos"
CARPETA_EJEMPLOS = CARPETA_EVENTOS / "ejemplos"
SUFIJO_ESQUEMA = ".schema.json"
SUFIJO_EJEMPLO = ".ejemplo.json"
SUFIJO_EJEMPLO_CAMPO_EXTRA = ".ejemplo-campo-extra.json"


class Resumen:
    """Acumula el resultado (OK / ERROR) de cada archivo revisado."""

    def __init__(self):
        """Crea un resumen vacío."""
        self.filas = []

    def registrar(self, archivo, correcto, detalles=()):
        """Registra el resultado de un archivo y lo imprime de inmediato.

        Args:
            archivo: Ruta relativa a la raíz del repositorio.
            correcto: ``True`` si el archivo pasó la validación.
            detalles: Mensajes de error que explican el fallo.
        """
        self.filas.append((archivo, correcto))
        estado = "OK   " if correcto else "ERROR"
        print(f"  [{estado}] {archivo}")
        for detalle in detalles:
            print(f"           - {detalle}")

    def hay_errores(self):
        """Indica si algún archivo falló.

        Returns:
            ``True`` si al menos un archivo quedó en ERROR.
        """
        return any(not correcto for _, correcto in self.filas)


def relativa(ruta):
    """Devuelve la ruta relativa a la raíz del repositorio, con barras '/'.

    Args:
        ruta: Ruta absoluta dentro del repositorio.

    Returns:
        Texto con la ruta relativa.
    """
    return ruta.relative_to(RAIZ).as_posix()


def validar_proto(resumen):
    """Compila el .proto del catálogo con grpcio-tools.

    Args:
        resumen: Resumen donde se registra el resultado.
    """
    print("\n1) Compilación del contrato gRPC")
    incluidos_estandar = resources.files("grpc_tools") / "_proto"
    with tempfile.TemporaryDirectory() as salida:
        codigo = protoc.main([
            "grpc_tools.protoc",
            f"-I{CARPETA_PROTO}",
            f"-I{incluidos_estandar}",
            f"--python_out={salida}",
            f"--grpc_python_out={salida}",
            str(ARCHIVO_PROTO),
        ])
    detalles = [] if codigo == 0 else [f"protoc terminó con código {codigo} (ver mensajes arriba)"]
    resumen.registrar(relativa(CARPETA_PROTO / ARCHIVO_PROTO), codigo == 0, detalles)


def cargar_json(ruta):
    """Lee un archivo JSON en UTF-8.

    Args:
        ruta: Archivo a leer.

    Returns:
        El contenido decodificado.

    Raises:
        ValueError: Si el archivo no es JSON válido.
    """
    with ruta.open(encoding="utf-8") as archivo:
        return json.load(archivo)


def validar_esquemas(resumen):
    """Comprueba que cada JSON Schema sea válido según draft 2020-12.

    Args:
        resumen: Resumen donde se registra cada esquema.

    Returns:
        Diccionario ``nombre base -> (ruta, esquema)`` de los esquemas válidos.
    """
    print("\n2) Esquemas JSON de eventos (draft 2020-12)")
    esquemas = {}
    for ruta in sorted(CARPETA_EVENTOS.rglob(f"*{SUFIJO_ESQUEMA}")):
        try:
            esquema = cargar_json(ruta)
            Draft202012Validator.check_schema(esquema)
        except (ValueError, SchemaError) as error:
            resumen.registrar(relativa(ruta), False, [str(error).splitlines()[0]])
            continue
        esquemas[ruta.name.removesuffix(SUFIJO_ESQUEMA)] = (ruta, esquema)
        resumen.registrar(relativa(ruta), True)
    return esquemas


def describir_error(error):
    """Convierte un error de jsonschema en una línea legible.

    Args:
        error: Error de validación de jsonschema.

    Returns:
        Texto con la ruta JSON del campo y el mensaje.
    """
    return f"{error.json_path}: {error.message}"


def validar_ejemplos(resumen, esquemas):
    """Valida cada ejemplo contra el esquema del mismo nombre base.

    Args:
        resumen: Resumen donde se registra cada ejemplo.
        esquemas: Esquemas válidos devueltos por ``validar_esquemas``.
    """
    print("\n3) Ejemplos de eventos contra su esquema")
    if "date-time" not in Draft202012Validator.FORMAT_CHECKER.checkers:
        resumen.registrar("(entorno)", False, ["falta rfc3339-validator: no se puede comprobar format date-time"])
        return
    con_ejemplo = set()
    for ruta in sorted(CARPETA_EJEMPLOS.glob(f"*{SUFIJO_EJEMPLO}")):
        nombre = ruta.name.removesuffix(SUFIJO_EJEMPLO)
        if nombre not in esquemas:
            resumen.registrar(relativa(ruta), False, [f"no existe un esquema válido {nombre}{SUFIJO_ESQUEMA}"])
            continue
        con_ejemplo.add(nombre)
        try:
            instancia = cargar_json(ruta)
        except ValueError as error:
            resumen.registrar(relativa(ruta), False, [f"JSON inválido: {error}"])
            continue
        validador = Draft202012Validator(
            esquemas[nombre][1], format_checker=Draft202012Validator.FORMAT_CHECKER
        )
        errores = sorted(validador.iter_errors(instancia), key=lambda error: list(error.path))
        resumen.registrar(relativa(ruta), not errores, [describir_error(error) for error in errores])
    for nombre in sorted(set(esquemas) - con_ejemplo):
        resumen.registrar(relativa(esquemas[nombre][0]), False, ["este esquema no tiene ejemplo en contratos/eventos/ejemplos"])


def validar_ejemplos_con_campo_extra(resumen, esquemas):
    """Comprueba que el esquema estricto RECHACE un ejemplo con un campo extra.

    Los esquemas de /contratos son estrictos para el productor
    (``additionalProperties: false``), mientras que el consumidor es
    tolerante y acepta campos desconocidos (CONTRATOS.md §7.7.4 y §7.8). Por
    eso aquí el resultado esperado es el opuesto al de ``validar_ejemplos``:
    si el esquema rechaza el campo extra, el archivo queda en OK; si lo
    acepta, es un ERROR porque el esquema dejó de ser estricto.

    Args:
        resumen: Resumen donde se registra cada archivo.
        esquemas: Esquemas válidos devueltos por ``validar_esquemas``.
    """
    print("\n4) Ejemplos con campo extra (deben ser RECHAZADOS por el esquema estricto)")
    for ruta in sorted(CARPETA_EJEMPLOS.glob(f"*{SUFIJO_EJEMPLO_CAMPO_EXTRA}")):
        nombre = ruta.name.removesuffix(SUFIJO_EJEMPLO_CAMPO_EXTRA)
        if nombre not in esquemas:
            resumen.registrar(relativa(ruta), False, [f"no existe un esquema válido {nombre}{SUFIJO_ESQUEMA}"])
            continue
        try:
            instancia = cargar_json(ruta)
        except ValueError as error:
            resumen.registrar(relativa(ruta), False, [f"JSON inválido: {error}"])
            continue
        validador = Draft202012Validator(
            esquemas[nombre][1], format_checker=Draft202012Validator.FORMAT_CHECKER
        )
        errores = sorted(validador.iter_errors(instancia), key=lambda error: list(error.path))
        if errores:
            resumen.registrar(relativa(ruta), True, ["rechazado como se esperaba: " + describir_error(error) for error in errores])
        else:
            resumen.registrar(relativa(ruta), False, ["el esquema aceptó el campo extra: dejó de ser estricto (additionalProperties: false)"])


def main():
    """Ejecuta todas las validaciones e imprime el resumen final.

    Returns:
        Código de salida del proceso: 0 si todo es válido, 1 si hay errores.
    """
    print("Validación de contratos · Banco de Preguntas")
    resumen = Resumen()
    validar_proto(resumen)
    esquemas = validar_esquemas(resumen)
    validar_ejemplos(resumen, esquemas)
    validar_ejemplos_con_campo_extra(resumen, esquemas)
    errores = sum(1 for _, correcto in resumen.filas if not correcto)
    print(f"\nResumen: {len(resumen.filas) - errores} OK · {errores} ERROR")
    if resumen.hay_errores():
        print("Resultado: ERROR. Revisa los archivos marcados arriba.")
        return 1
    print("Resultado: OK. Todos los contratos son válidos.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
