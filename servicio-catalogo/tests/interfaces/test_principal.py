"""Pruebas del arranque: configuración, puertos ocupados y parada limpia."""

from __future__ import annotations

import asyncio
import socket
import sys

import grpc
import httpx
import pytest

from catalogo.infraestructura.configuracion import Configuracion, ConfiguracionInvalidaError
from catalogo.principal import servir

URL_SIN_BASE = "postgresql+psycopg://catalogo:catalogo@127.0.0.1:1/catalogo"


def puerto_libre() -> int:
    with socket.socket() as zocalo:
        zocalo.bind(("127.0.0.1", 0))
        return zocalo.getsockname()[1]


def ocupar_puerto() -> socket.socket:
    """Ocupa un puerto de forma exclusiva (en Windows hace falta SO_EXCLUSIVEADDRUSE)."""
    zocalo = socket.socket()
    if hasattr(socket, "SO_EXCLUSIVEADDRUSE"):
        zocalo.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
    zocalo.bind(("0.0.0.0", 0))
    zocalo.listen()
    return zocalo


def configuracion(puerto_http: int, puerto_grpc: int) -> Configuracion:
    return Configuracion(puerto_http=puerto_http, puerto_grpc=puerto_grpc, url_base_datos=URL_SIN_BASE)


# ---------------------------------------------------------------- configuración


def test_valores_por_defecto_locales_de_la_tabla_9_2() -> None:
    c = Configuracion.desde_entorno({})
    assert (c.puerto_http, c.puerto_grpc) == (8082, 50051)
    assert c.url_base_datos == "postgresql+psycopg://catalogo:catalogo@localhost:5434/catalogo"


def test_lee_las_variables_de_la_tabla_9_2() -> None:
    c = Configuracion.desde_entorno(
        {
            "CATALOGO_PUERTO_HTTP": "9000",
            "CATALOGO_PUERTO_GRPC": "9001",
            "CATALOGO_BD_URL": "postgresql+psycopg://catalogo:catalogo@bd-catalogo:5432/catalogo",
        }
    )
    assert (c.puerto_http, c.puerto_grpc) == (9000, 9001)
    assert "bd-catalogo" in c.url_base_datos


@pytest.mark.parametrize("valor", ["abc", "0", "70000", "-1"])
def test_un_puerto_invalido_es_un_error_claro(valor: str) -> None:
    with pytest.raises(ConfiguracionInvalidaError, match="CATALOGO_PUERTO_HTTP"):
        Configuracion.desde_entorno({"CATALOGO_PUERTO_HTTP": valor})


def test_una_url_que_no_es_de_psycopg_es_un_error_claro() -> None:
    with pytest.raises(ConfiguracionInvalidaError, match=r"postgresql\+psycopg"):
        Configuracion.desde_entorno({"CATALOGO_BD_URL": "mysql://x"})


# ---------------------------------------------------------------------- arranque


@pytest.mark.skipif(
    sys.platform == "win32",
    reason="En Windows grpc enlaza un puerto ya ocupado; el caso se verifica en el contenedor Linux.",
)
async def test_si_el_puerto_grpc_esta_ocupado_el_proceso_termina_con_error() -> None:
    with ocupar_puerto() as ocupado:
        puerto = ocupado.getsockname()[1]
        assert await asyncio.wait_for(servir(configuracion(puerto_libre(), puerto)), 20) == 1


async def test_si_el_puerto_http_esta_ocupado_el_proceso_termina_con_error() -> None:
    with ocupar_puerto() as ocupado:
        puerto = ocupado.getsockname()[1]
        assert await asyncio.wait_for(servir(configuracion(puerto, puerto_libre())), 20) == 1


async def test_arranca_sin_base_de_datos_salud_200_endpoints_503_y_grpc_unavailable() -> None:
    puerto_http, puerto_grpc = puerto_libre(), puerto_libre()
    tarea = asyncio.create_task(servir(configuracion(puerto_http, puerto_grpc)))
    try:
        async with httpx.AsyncClient(base_url=f"http://127.0.0.1:{puerto_http}") as cliente:
            for _ in range(50):  # espera a que levante uvicorn
                try:
                    salud = await cliente.get("/salud")
                    break
                except httpx.TransportError:
                    await asyncio.sleep(0.2)
            assert salud.status_code == 200
            respuesta = await cliente.get(
                "/api/v1/competencias",
                headers={"X-Usuario-Id": "11111111-1111-4111-8111-000000000001", "X-Roles": "ADMINISTRADOR"},
            )
            assert respuesta.status_code == 503
            assert respuesta.json()["codigo"] == "BASE_DE_DATOS_NO_DISPONIBLE"
        from catalogo.interfaces.grpc.generado import catalogo_academico_pb2 as pb2
        from catalogo.interfaces.grpc.generado import catalogo_academico_pb2_grpc as pb2_grpc

        async with grpc.aio.insecure_channel(f"127.0.0.1:{puerto_grpc}") as canal:
            with pytest.raises(grpc.aio.AioRpcError) as error:
                await pb2_grpc.CatalogoAcademicoStub(canal).ValidarClasificacion(
                    pb2.ValidarClasificacionSolicitud(
                        competencia_id="22222222-2222-4222-8222-000000000101",
                        tema_id="22222222-2222-4222-8222-000000000201",
                        subtema_id="22222222-2222-4222-8222-000000000301",
                    )
                )
            assert error.value.code() == grpc.StatusCode.UNAVAILABLE
    finally:
        tarea.cancel()
        await asyncio.gather(tarea, return_exceptions=True)
