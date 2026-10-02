"""Fixtures compartidas de las pruebas."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

from catalogo.dominio.competencia import Competencia
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from tests import ids_catalogo_semilla as semilla
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.ejecutor_transaccional_en_memoria import EjecutorTransaccionalEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria

CARPETA_SERVICIO = Path(__file__).resolve().parent.parent
CARPETA_GENERADA = CARPETA_SERVICIO / "catalogo" / "interfaces" / "grpc" / "generado"


def pytest_configure(config: pytest.Config) -> None:
    """Genera el código gRPC si falta (no se versiona; ver ``scripts/generar_grpc.py``)."""
    if not (CARPETA_GENERADA / "catalogo_academico_pb2.py").exists():
        subprocess.run(
            [sys.executable, str(CARPETA_SERVICIO / "scripts" / "generar_grpc.py")], check=True
        )


@pytest.fixture
def repositorio() -> CompetenciaRepositorioEnMemoria:
    """Repositorio en memoria vacío."""
    return CompetenciaRepositorioEnMemoria()


@pytest.fixture
def repositorio_con_semilla(
    repositorio: CompetenciaRepositorioEnMemoria,
) -> CompetenciaRepositorioEnMemoria:
    """Repositorio con la tabla 4.3 cargada directamente con el dominio."""
    for competencia_id, nombre_competencia, temas in semilla.TABLA_4_3:
        competencia = Competencia.crear(
            NombreCatalogo(nombre_competencia), competencia_id=CompetenciaId.desde_texto(competencia_id)
        )
        for tema_id, nombre_tema, subtemas in temas:
            competencia.agregar_tema(NombreCatalogo(nombre_tema), TemaId.desde_texto(tema_id))
            for subtema_id, nombre_subtema in subtemas:
                competencia.agregar_subtema(
                    TemaId.desde_texto(tema_id),
                    NombreCatalogo(nombre_subtema),
                    SubtemaId.desde_texto(subtema_id),
                )
        repositorio.guardar(competencia)
    return repositorio


@pytest.fixture
def publicador_en_memoria() -> PublicadorEventosEnMemoria:
    """Publicador que recuerda los eventos."""
    return PublicadorEventosEnMemoria()


@pytest.fixture
def ejecutor_en_memoria(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
    publicador_en_memoria: PublicadorEventosEnMemoria,
) -> EjecutorTransaccionalEnMemoria:
    """Ejecutor en memoria sobre el catálogo semilla."""
    return EjecutorTransaccionalEnMemoria(repositorio_con_semilla, publicador_en_memoria)


def pytest_asyncio_loop_factories(config: pytest.Config, item: pytest.Item) -> dict:
    """En Windows ``psycopg`` asíncrono exige ``SelectorEventLoop`` (en Linux no hace falta)."""
    import asyncio

    if sys.platform == "win32":
        return {"selector": asyncio.SelectorEventLoop}
    return {"predeterminado": asyncio.new_event_loop}
