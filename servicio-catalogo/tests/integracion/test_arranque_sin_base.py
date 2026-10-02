"""Integración (Testcontainers): el servicio arranca con PostgreSQL detenido y se recupera.

CONTRATOS 9.3.6 (v1.10): ningún servicio depende de la base de datos para iniciar. Las
migraciones y la siembra corren en segundo plano con reintentos; mientras tanto REST responde
503 y gRPC ``UNAVAILABLE``.
"""

from __future__ import annotations

import asyncio
import logging
import socket
import time

import grpc
import httpx
import pytest

from catalogo.infraestructura import inicializacion
from catalogo.infraestructura.configuracion import Configuracion
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2 as pb2
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2_grpc as pb2_grpc
from catalogo.principal import servir
from tests import ids_catalogo_semilla as ids
from tests.integracion.conftest import IMAGEN_POSTGRES

ADMIN = {"X-Usuario-Id": "11111111-1111-4111-8111-000000000001", "X-Roles": "ADMINISTRADOR"}
ESPERA_MAXIMA_SEGUNDOS = 90


def puerto_libre() -> int:
    with socket.socket() as zocalo:
        zocalo.bind(("127.0.0.1", 0))
        return zocalo.getsockname()[1]


async def esperar(condicion, descripcion: str, segundos: float = ESPERA_MAXIMA_SEGUNDOS):
    """Reintenta una corrutina hasta que devuelva algo verdadero."""
    limite = time.monotonic() + segundos
    while time.monotonic() < limite:
        try:
            resultado = await condicion()
            if resultado:
                return resultado
        except (httpx.TransportError, grpc.aio.AioRpcError):
            pass
        await asyncio.sleep(0.3)
    raise AssertionError(f"Tiempo agotado esperando: {descripcion}")


async def validar_grpc(puerto: int):
    async with grpc.aio.insecure_channel(f"127.0.0.1:{puerto}") as canal:
        return await pb2_grpc.CatalogoAcademicoStub(canal).ValidarClasificacion(
            pb2.ValidarClasificacionSolicitud(
                competencia_id=ids.COMPETENCIA_RAZONAMIENTO,
                tema_id=ids.TEMA_ESTADISTICA,
                subtema_id=ids.SUBTEMA_TENDENCIA_CENTRAL,
            )
        )


async def detener(tarea: asyncio.Task) -> None:
    tarea.cancel()
    await asyncio.gather(tarea, return_exceptions=True)


async def test_arranca_sin_postgres_migra_siembra_y_no_duplica_en_un_segundo_arranque(
    monkeypatch: pytest.MonkeyPatch, caplog: pytest.LogCaptureFixture
) -> None:
    try:
        from testcontainers.community.postgres import PostgresContainer
    except ImportError:
        from testcontainers.postgres import PostgresContainer
    from testcontainers.core.docker_client import DockerClient

    try:
        DockerClient().client.ping()
    except Exception as error:
        pytest.skip(f"Docker/Testcontainers no está disponible: {error}")

    # Reintentos rápidos para que la prueba no espere los 30 s del máximo real.
    monkeypatch.setattr(inicializacion, "ESPERA_INICIAL_SEGUNDOS", 0.3)
    monkeypatch.setattr(inicializacion, "ESPERA_MAXIMA_SEGUNDOS", 1.0)
    caplog.set_level(logging.INFO)

    puerto_bd = puerto_libre()
    contenedor = PostgresContainer(IMAGEN_POSTGRES, driver="psycopg").with_bind_ports(5432, puerto_bd)
    url = f"postgresql+psycopg://{contenedor.username}:{contenedor.password}@127.0.0.1:{puerto_bd}/{contenedor.dbname}"
    puerto_http, puerto_grpc = puerto_libre(), puerto_libre()
    tarea = asyncio.create_task(servir(Configuracion(puerto_http, puerto_grpc, url)))
    try:
        async with httpx.AsyncClient(base_url=f"http://127.0.0.1:{puerto_http}") as cliente:
            # --- 1. PostgreSQL detenido (nunca arrancó): el servicio arranca igual.
            async def salud():
                return (await cliente.get("/salud")).status_code == 200

            await esperar(salud, "que REST responda /salud", 20)
            assert (await cliente.get("/docs")).status_code == 200
            assert (await cliente.get("/openapi.json")).status_code == 200
            sin_base = await cliente.get("/api/v1/competencias", headers=ADMIN)
            assert sin_base.status_code == 503
            assert sin_base.json()["codigo"] == "BASE_DE_DATOS_NO_DISPONIBLE"
            with pytest.raises(grpc.aio.AioRpcError) as error:
                await validar_grpc(puerto_grpc)
            assert error.value.code() == grpc.StatusCode.UNAVAILABLE
            await asyncio.sleep(1.5)  # deja que se registren varios reintentos
            assert tarea.done() is False

            # --- 2. Se inicia PostgreSQL: se migra, se siembra y todo responde normalmente.
            await asyncio.to_thread(contenedor.start)

            async def catalogo_responde():
                respuesta = await cliente.get("/api/v1/competencias", headers=ADMIN)
                return respuesta.json() if respuesta.status_code == 200 else None

            competencias = await esperar(catalogo_responde, "que el catálogo quede listo")
            assert {c["competenciaId"] for c in competencias} == {ids.COMPETENCIA_RAZONAMIENTO, ids.COMPETENCIA_DISENO}
            assert sum(len(c["temas"]) for c in competencias) == 4
            assert sum(len(t["subtemas"]) for c in competencias for t in c["temas"]) == 5
            valida = await validar_grpc(puerto_grpc)
            assert valida.valida is True and valida.motivo == pb2.MOTIVO_RECHAZO_NINGUNO

        mensajes = [r.getMessage() for r in caplog.records]
        assert any("Intento 1 fallido" in m for m in mensajes), "no se registró el primer intento fallido"
        assert any("Running upgrade" in m for m in mensajes), "no se registró la migración"
        assert any("Catálogo listo" in m and "catálogo sembrado" in m for m in mensajes)

        # --- 3. Segundo arranque sobre la misma base: no duplica la siembra.
        await detener(tarea)
        caplog.clear()
        puerto_http2, puerto_grpc2 = puerto_libre(), puerto_libre()
        tarea = asyncio.create_task(servir(Configuracion(puerto_http2, puerto_grpc2, url)))
        async with httpx.AsyncClient(base_url=f"http://127.0.0.1:{puerto_http2}") as cliente2:

            async def lista_de_nuevo():
                respuesta = await cliente2.get("/api/v1/competencias", headers=ADMIN)
                return respuesta.json() if respuesta.status_code == 200 else None

            otra_vez = await esperar(lista_de_nuevo, "el segundo arranque")
            assert len(otra_vez) == 2
            assert sum(len(t["subtemas"]) for c in otra_vez for t in c["temas"]) == 5
        assert any("no se sembró" in r.getMessage() for r in caplog.records)
    finally:
        await detener(tarea)
        try:
            contenedor.stop()
        except Exception:
            pass
