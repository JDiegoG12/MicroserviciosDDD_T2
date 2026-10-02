"""Fixtures de integración: un contenedor PostgreSQL 16 real por sesión de pruebas.

Si Docker no está disponible las pruebas se **omiten** con un mensaje claro: no se
simulan con dobles (CONTRATOS.md / etapa 2).
"""

from __future__ import annotations

from collections.abc import AsyncIterator, Iterator

import pytest
import pytest_asyncio
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, async_sessionmaker

from catalogo.infraestructura.base_datos.ejecutor_transaccional_sqlalchemy import (
    EjecutorTransaccionalSqlAlchemy,
    EstadoInicializacion,
    crear_motor,
)
from catalogo.infraestructura.inicializacion import aplicar_migraciones
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria

IMAGEN_POSTGRES = "postgres:16"


def pytest_collection_modifyitems(items: list[pytest.Item]) -> None:
    """Marca como ``integracion`` todo lo que está en esta carpeta."""
    for item in items:
        if "tests/integracion" in item.nodeid.replace("\\", "/"):
            item.add_marker(pytest.mark.integracion)


@pytest.fixture(scope="session")
def url_postgres() -> Iterator[str]:
    """Levanta PostgreSQL 16 con Testcontainers y devuelve su URL ``postgresql+psycopg://``."""
    try:
        try:
            from testcontainers.community.postgres import PostgresContainer
        except ImportError:  # versiones anteriores de testcontainers
            from testcontainers.postgres import PostgresContainer

        contenedor = PostgresContainer(IMAGEN_POSTGRES, driver="psycopg")
        contenedor.start()
    except Exception as error:  # Docker no disponible o imagen inalcanzable
        pytest.skip(f"Docker/Testcontainers no está disponible: {error}")
    try:
        yield contenedor.get_connection_url()
    finally:
        contenedor.stop()


@pytest_asyncio.fixture
async def motor(url_postgres: str) -> AsyncIterator[AsyncEngine]:
    """Motor asíncrono sobre una base vacía (se recrea el esquema en cada prueba)."""
    motor = crear_motor(url_postgres)
    async with motor.begin() as conexion:
        await conexion.execute(text("DROP SCHEMA public CASCADE"))
        await conexion.execute(text("CREATE SCHEMA public"))
    yield motor
    await motor.dispose()


@pytest_asyncio.fixture
async def motor_migrado(motor: AsyncEngine) -> AsyncEngine:
    """Motor con las migraciones de Alembic ya aplicadas."""
    await aplicar_migraciones(motor)
    return motor


@pytest.fixture
def publicador() -> PublicadorEventosEnMemoria:
    """Publicador que recuerda los eventos."""
    return PublicadorEventosEnMemoria()


@pytest.fixture
def ejecutor(
    motor_migrado: AsyncEngine, publicador: PublicadorEventosEnMemoria
) -> EjecutorTransaccionalSqlAlchemy:
    """Ejecutor transaccional listo, sobre la base migrada."""
    return EjecutorTransaccionalSqlAlchemy(
        async_sessionmaker(motor_migrado, expire_on_commit=False),
        publicador,
        EstadoInicializacion(catalogo_listo=True),
    )
