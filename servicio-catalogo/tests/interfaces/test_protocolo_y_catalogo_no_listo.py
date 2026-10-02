"""Etapa 2b: errores de protocolo (CONTRATOS 5.3), correlación inválida (4.1), formas de UUID
(4) y comportamiento mientras el catálogo no está listo (9.3.6, v1.10)."""

from __future__ import annotations

import asyncio
import logging
from collections.abc import AsyncIterator
from types import SimpleNamespace
from uuid import UUID

import grpc
import httpx
import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import async_sessionmaker

from catalogo.infraestructura import inicializacion
from catalogo.infraestructura.base_datos.ejecutor_transaccional_sqlalchemy import (
    EjecutorTransaccionalSqlAlchemy,
    EstadoInicializacion,
    crear_motor,
)
from catalogo.infraestructura.correlacion import normalizar_id_correlacion
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2 as pb2
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2_grpc as pb2_grpc
from catalogo.interfaces.grpc.servidor_grpc import crear_servidor_grpc
from catalogo.interfaces.rest.aplicacion_fastapi import crear_aplicacion
from tests import ids_catalogo_semilla as ids
from tests.dobles.ejecutor_transaccional_en_memoria import EjecutorTransaccionalEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria
from tests.interfaces.test_api_rest import ADMIN, BASE, ENDPOINTS, IDS_ENDPOINTS, RAZ, es_problema, llamar

URL_SIN_BASE = "postgresql+psycopg://catalogo:catalogo@127.0.0.1:1/catalogo"


@pytest_asyncio.fixture
async def cliente(ejecutor_en_memoria: EjecutorTransaccionalEnMemoria) -> AsyncIterator[httpx.AsyncClient]:
    transporte = httpx.ASGITransport(app=crear_aplicacion(ejecutor_en_memoria))
    async with httpx.AsyncClient(transport=transporte, base_url="http://prueba") as cliente_http:
        yield cliente_http


def crear_ejecutor_real(catalogo_listo: bool) -> EjecutorTransaccionalSqlAlchemy:
    """Ejecutor SQLAlchemy real apuntando a un puerto sin base de datos."""
    return EjecutorTransaccionalSqlAlchemy(
        async_sessionmaker(crear_motor(URL_SIN_BASE)),
        PublicadorEventosEnMemoria(),
        EstadoInicializacion(catalogo_listo=catalogo_listo),
    )


# -------------------------------------------- 404 / 405 / 415 (CONTRATOS 5.3, v1.10)


async def test_ruta_inexistente_es_404_recurso_no_encontrado(cliente: httpx.AsyncClient) -> None:
    respuesta = await cliente.get("/api/v1/no-existe", headers=ADMIN)
    problema = es_problema(respuesta, 404, "RECURSO_NO_ENCONTRADO")
    assert problema["idCorrelacion"] == respuesta.headers["x-id-correlacion"]


async def test_metodo_no_permitido_es_405(cliente: httpx.AsyncClient) -> None:
    respuesta = await cliente.delete(BASE, headers=ADMIN)
    problema = es_problema(respuesta, 405, "METODO_NO_PERMITIDO")
    assert problema["idCorrelacion"] == respuesta.headers["x-id-correlacion"]


@pytest.mark.parametrize(
    "tipo",
    ["text/plain", "application/x-www-form-urlencoded", "application/xml", "multipart/form-data"],
)
@pytest.mark.parametrize("metodo", ["POST", "PUT"])
async def test_cuerpo_que_no_es_json_es_415(cliente: httpx.AsyncClient, metodo: str, tipo: str) -> None:
    ruta = BASE if metodo == "POST" else f"{BASE}/{RAZ}"
    respuesta = await cliente.request(
        metodo, ruta, content=b'{"nombre": "Nueva"}', headers={**ADMIN, "Content-Type": tipo}
    )
    problema = es_problema(respuesta, 415, "TIPO_DE_CONTENIDO_NO_SOPORTADO")
    assert problema["idCorrelacion"] == respuesta.headers["x-id-correlacion"]
    assert problema["instance"] == ruta


async def test_cuerpo_sin_tipo_de_contenido_es_415(cliente: httpx.AsyncClient) -> None:
    respuesta = await cliente.post(BASE, content=b'{"nombre": "Nueva"}', headers=ADMIN)
    es_problema(respuesta, 415, "TIPO_DE_CONTENIDO_NO_SOPORTADO")


@pytest.mark.parametrize(
    "tipo",
    ["application/json", "application/json; charset=utf-8", "APPLICATION/JSON", "application/vnd.api+json"],
)
async def test_los_tipos_json_se_aceptan(cliente: httpx.AsyncClient, tipo: str) -> None:
    respuesta = await cliente.post(
        BASE, content=b'{"nombre": "Nueva"}', headers={**ADMIN, "Content-Type": tipo}
    )
    assert respuesta.status_code == 201


async def test_un_get_con_content_type_y_sin_cuerpo_no_es_415(cliente: httpx.AsyncClient) -> None:
    respuesta = await cliente.get(BASE, headers={**ADMIN, "Content-Type": "text/plain"})
    assert respuesta.status_code == 200


async def test_un_post_sin_cuerpo_sigue_siendo_400_de_validacion(cliente: httpx.AsyncClient) -> None:
    es_problema(await cliente.post(BASE, headers=ADMIN), 400, "SOLICITUD_INVALIDA")


# --------------------------------------------- X-Id-Correlacion inválido (4.1, v1.10)


async def test_correlacion_invalida_se_reemplaza_y_registra_un_aviso(
    cliente: httpx.AsyncClient, caplog: pytest.LogCaptureFixture
) -> None:
    with caplog.at_level(logging.WARNING, logger="catalogo.infraestructura.correlacion"):
        respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Id-Correlacion": "no-es-uuid"})
    assert respuesta.status_code == 200  # nunca es error
    nuevo = respuesta.headers["x-id-correlacion"]
    assert UUID(nuevo)
    avisos = [r for r in caplog.records if r.levelno == logging.WARNING]
    assert len(avisos) == 1
    assert "no-es-uuid" in avisos[0].getMessage() and nuevo in avisos[0].getMessage()


@pytest.mark.parametrize("valor", [None, "", "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b", "0B6F2C4E-1D3A-4E5F-8A7B-9C0D1E2F3A4B"])
def test_correlacion_ausente_o_valida_no_genera_aviso(valor, caplog: pytest.LogCaptureFixture) -> None:
    with caplog.at_level(logging.WARNING, logger="catalogo.infraestructura.correlacion"):
        resultado = normalizar_id_correlacion(valor)
    assert UUID(resultado)
    assert not caplog.records


# ---------------------------- reglas de la v1.9 / v1.10 en REST y gRPC (ajuste 7)


@pytest.mark.parametrize("usuario", [ADMIN["X-Usuario-Id"].upper()])
async def test_x_usuario_id_en_mayusculas_se_acepta(cliente: httpx.AsyncClient, usuario: str) -> None:
    respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Usuario-Id": usuario})
    assert respuesta.status_code == 200


@pytest.mark.parametrize(
    "usuario",
    ["11111111111141118111000000000001", "{11111111-1111-4111-8111-000000000001}",
     "urn:uuid:11111111-1111-4111-8111-000000000001", "no-es-uuid"],
    ids=["sin_guiones", "con_llaves", "urn", "texto"],
)
async def test_x_usuario_id_con_otra_forma_es_400(cliente: httpx.AsyncClient, usuario: str) -> None:
    respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Usuario-Id": usuario})
    es_problema(respuesta, 400, "SOLICITUD_INVALIDA")


# --------------------------- catalogo_listo = False: REST 503 y gRPC UNAVAILABLE


@pytest_asyncio.fixture
async def cliente_catalogo_no_listo() -> AsyncIterator[httpx.AsyncClient]:
    transporte = httpx.ASGITransport(app=crear_aplicacion(crear_ejecutor_real(catalogo_listo=False)))
    async with httpx.AsyncClient(transport=transporte, base_url="http://prueba") as cliente_http:
        yield cliente_http


@pytest.mark.parametrize("ruta", ["/salud", "/docs", "/openapi.json"])
async def test_sin_catalogo_listo_salud_docs_y_openapi_responden_200(
    cliente_catalogo_no_listo: httpx.AsyncClient, ruta: str
) -> None:
    assert (await cliente_catalogo_no_listo.get(ruta)).status_code == 200


@pytest.mark.parametrize("metodo,ruta,cuerpo,escritura,exito", ENDPOINTS, ids=IDS_ENDPOINTS)
async def test_sin_catalogo_listo_cada_endpoint_responde_503(
    cliente_catalogo_no_listo: httpx.AsyncClient, metodo, ruta, cuerpo, escritura, exito
) -> None:
    respuesta = await llamar(cliente_catalogo_no_listo, metodo, ruta, cuerpo, ADMIN)
    problema = es_problema(respuesta, 503, "BASE_DE_DATOS_NO_DISPONIBLE")
    assert problema["idCorrelacion"] == respuesta.headers["x-id-correlacion"]


async def test_con_el_catalogo_listo_pero_la_base_caida_tambien_responde_503() -> None:
    transporte = httpx.ASGITransport(app=crear_aplicacion(crear_ejecutor_real(catalogo_listo=True)))
    async with httpx.AsyncClient(transport=transporte, base_url="http://prueba") as cliente_http:
        es_problema(await llamar(cliente_http, "GET", BASE, None, ADMIN), 503, "BASE_DE_DATOS_NO_DISPONIBLE")
        assert (await cliente_http.get("/salud")).status_code == 200


@pytest_asyncio.fixture
async def canal_catalogo_no_listo() -> AsyncIterator[grpc.aio.Channel]:
    servidor = crear_servidor_grpc(crear_ejecutor_real(catalogo_listo=False))
    puerto = servidor.add_insecure_port("127.0.0.1:0")
    await servidor.start()
    async with grpc.aio.insecure_channel(f"127.0.0.1:{puerto}") as canal:
        yield canal
    await servidor.stop(grace=None)


async def test_sin_catalogo_listo_grpc_responde_unavailable_en_espanol(
    canal_catalogo_no_listo: grpc.aio.Channel,
) -> None:
    solicitud = pb2.ValidarClasificacionSolicitud(
        competencia_id=ids.COMPETENCIA_RAZONAMIENTO, tema_id=ids.TEMA_ESTADISTICA, subtema_id=ids.SUBTEMA_TENDENCIA_CENTRAL
    )
    with pytest.raises(grpc.aio.AioRpcError) as error:
        await pb2_grpc.CatalogoAcademicoStub(canal_catalogo_no_listo).ValidarClasificacion(solicitud)
    assert error.value.code() == grpc.StatusCode.UNAVAILABLE
    assert "base de datos" in error.value.details()


# ------------------------------------------ reintentos de la inicialización (9.3.6)


async def test_la_inicializacion_reintenta_con_espera_progresiva_hasta_un_maximo_de_30_s(
    monkeypatch: pytest.MonkeyPatch, caplog: pytest.LogCaptureFixture
) -> None:
    esperas: list[float] = []
    llamadas = {"migraciones": 0, "siembra": 0}

    async def migraciones_falsas(motor) -> None:
        llamadas["migraciones"] += 1
        if llamadas["migraciones"] <= 7:
            raise ConnectionRefusedError("sin base")

    async def siembra_falsa(ejecutor) -> bool:
        llamadas["siembra"] += 1
        return True

    async def espera_falsa(segundos: float) -> None:
        esperas.append(segundos)

    monkeypatch.setattr(inicializacion, "aplicar_migraciones", migraciones_falsas)
    monkeypatch.setattr(inicializacion, "sembrar_catalogo", siembra_falsa)
    monkeypatch.setattr(
        inicializacion, "asyncio", SimpleNamespace(sleep=espera_falsa, CancelledError=asyncio.CancelledError)
    )
    estado = EstadoInicializacion()

    with caplog.at_level(logging.INFO, logger=inicializacion.registro.name):
        await inicializacion.inicializar_base_de_datos(None, None, estado)

    assert esperas == [1, 2, 4, 8, 16, 30, 30]
    assert llamadas == {"migraciones": 8, "siembra": 1}  # la siembra va después de las migraciones
    assert estado.catalogo_listo is True
    mensajes = [r.getMessage() for r in caplog.records]
    assert any("intento 1:" in m for m in mensajes) and any("Intento 1 fallido" in m for m in mensajes)
    assert any("Intento 7 fallido" in m for m in mensajes)
    assert any("Catálogo listo tras 8 intento(s)" in m and "sembrado" in m for m in mensajes)


async def test_mientras_fallan_las_migraciones_el_catalogo_no_queda_listo(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    estado = EstadoInicializacion()

    async def migraciones_que_fallan(motor) -> None:
        raise ConnectionRefusedError("sin base")

    async def espera_que_cancela(segundos: float) -> None:
        raise asyncio.CancelledError

    monkeypatch.setattr(inicializacion, "aplicar_migraciones", migraciones_que_fallan)
    monkeypatch.setattr(
        inicializacion, "asyncio", SimpleNamespace(sleep=espera_que_cancela, CancelledError=asyncio.CancelledError)
    )
    with pytest.raises(asyncio.CancelledError):
        await inicializacion.inicializar_base_de_datos(None, None, estado)
    assert estado.catalogo_listo is False
