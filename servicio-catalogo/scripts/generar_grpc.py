"""Genera el código Python de gRPC desde el ``.proto`` de ``/contratos``.

Uso (desde la carpeta ``servicio-catalogo``)::

    python scripts/generar_grpc.py

Lee ``contratos/proto/catalogo/v1/catalogo_academico.proto`` y deja los módulos
``catalogo_academico_pb2.py``, ``catalogo_academico_pb2.pyi`` y
``catalogo_academico_pb2_grpc.py`` en ``catalogo/interfaces/grpc/generado/``. Esa carpeta
**no se versiona** (CONTRATOS.md 6): se regenera en el Dockerfile y antes de las pruebas.

``protoc`` genera imports como ``from catalogo.v1 import catalogo_academico_pb2``, que
chocan con nuestro paquete ``catalogo``. Por eso el script deja los archivos planos en
``generado/`` y reescribe ese import para que apunte a
``catalogo.interfaces.grpc.generado``.

El proto se busca en este orden: la variable ``CATALOGO_RUTA_PROTO``, ``../contratos/proto``
(repositorio completo) y ``./contratos/proto`` (dentro de Docker, ``/app/contratos/proto``).
"""

from __future__ import annotations

import os
import re
import shutil
import sys
import tempfile
from pathlib import Path

from grpc_tools import protoc

CARPETA_SERVICIO = Path(__file__).resolve().parent.parent
ARCHIVO_PROTO = Path("catalogo/v1/catalogo_academico.proto")
CARPETA_SALIDA = CARPETA_SERVICIO / "catalogo" / "interfaces" / "grpc" / "generado"
PAQUETE_SALIDA = "catalogo.interfaces.grpc.generado"
IMPORT_GENERADO = re.compile(r"^from catalogo\.v1 import (\w+) as (\w+)$", re.MULTILINE)


def buscar_carpeta_proto() -> Path:
    """Busca la carpeta raíz de los ``.proto`` (la que contiene ``catalogo/v1``).

    Returns:
        La carpeta que contiene ``catalogo/v1/catalogo_academico.proto``.

    Raises:
        SystemExit: Si no se encuentra el archivo en ninguna ubicación.
    """
    candidatas = []
    if os.environ.get("CATALOGO_RUTA_PROTO"):
        candidatas.append(Path(os.environ["CATALOGO_RUTA_PROTO"]))
    candidatas.append(CARPETA_SERVICIO.parent / "contratos" / "proto")
    candidatas.append(CARPETA_SERVICIO / "contratos" / "proto")
    for candidata in candidatas:
        if (candidata / ARCHIVO_PROTO).is_file():
            return candidata
    buscadas = ", ".join(str(c) for c in candidatas)
    raise SystemExit(f"ERROR: no se encontró {ARCHIVO_PROTO} en: {buscadas}")


def compilar(carpeta_proto: Path, carpeta_temporal: Path) -> None:
    """Ejecuta ``protoc`` y deja el resultado en una carpeta temporal.

    Args:
        carpeta_proto: Carpeta raíz de los ``.proto``.
        carpeta_temporal: Carpeta donde ``protoc`` escribe su salida.

    Raises:
        SystemExit: Si ``protoc`` termina con error.
    """
    argumentos = [
        "protoc",
        f"-I{carpeta_proto}",
        f"--python_out={carpeta_temporal}",
        f"--pyi_out={carpeta_temporal}",
        f"--grpc_python_out={carpeta_temporal}",
        str(ARCHIVO_PROTO).replace("\\", "/"),
    ]
    codigo = protoc.main(argumentos)
    if codigo != 0:
        raise SystemExit(f"ERROR: protoc terminó con código {codigo}")


def instalar(carpeta_temporal: Path) -> list[Path]:
    """Copia los módulos generados a ``generado/`` corrigiendo los imports.

    Args:
        carpeta_temporal: Carpeta con la salida de ``protoc``.

    Returns:
        Los archivos escritos.
    """
    if CARPETA_SALIDA.exists():
        shutil.rmtree(CARPETA_SALIDA)
    CARPETA_SALIDA.mkdir(parents=True)
    escritos = []
    init = CARPETA_SALIDA / "__init__.py"
    init.write_text(
        '"""Código gRPC generado desde el .proto (no se versiona; ver scripts/generar_grpc.py)."""\n',
        encoding="utf-8",
    )
    escritos.append(init)
    for origen in sorted((carpeta_temporal / "catalogo" / "v1").iterdir()):
        texto = origen.read_text(encoding="utf-8")
        texto = IMPORT_GENERADO.sub(rf"from {PAQUETE_SALIDA} import \1 as \2", texto)
        destino = CARPETA_SALIDA / origen.name
        destino.write_text(texto, encoding="utf-8")
        escritos.append(destino)
    return escritos


def main() -> int:
    """Genera el código gRPC.

    Returns:
        ``0`` si todo salió bien.
    """
    carpeta_proto = buscar_carpeta_proto()
    with tempfile.TemporaryDirectory() as temporal:
        compilar(carpeta_proto, Path(temporal))
        escritos = instalar(Path(temporal))
    print(f"gRPC generado desde {carpeta_proto / ARCHIVO_PROTO}:")
    for archivo in escritos:
        print(f"  {archivo.relative_to(CARPETA_SERVICIO)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
