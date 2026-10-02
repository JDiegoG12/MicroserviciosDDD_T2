"""Pruebas del servidor gRPC con un servidor en proceso y un *stub* cliente generado."""

from __future__ import annotations

from collections.abc import AsyncIterator

import grpc
import pytest
import pytest_asyncio
from grpc_reflection.v1alpha import reflection_pb2, reflection_pb2_grpc

from catalogo.aplicacion.excepciones import BaseDeDatosNoDisponibleExcepcion
from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion
from catalogo.infraestructura.correlacion import METADATO_CORRELACION
from catalogo.interfaces.grpc import catalogo_academico_servicer
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2 as pb2
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2_grpc as pb2_grpc
from catalogo.interfaces.grpc.servidor_grpc import NOMBRE_SERVICIO_GRPC, crear_servidor_grpc
from tests import ids_catalogo_semilla as ids
from tests.dobles.ejecutor_transaccional_en_memoria import EjecutorTransaccionalEnMemoria

INEXISTENTE = "99999999-9999-4999-8999-999999999999"


@pytest_asyncio.fixture
async def canal(ejecutor_en_memoria: EjecutorTransaccionalEnMemoria) -> AsyncIterator[grpc.aio.Channel]:
    servidor = crear_servidor_grpc(ejecutor_en_memoria)
    puerto = servidor.add_insecure_port("127.0.0.1:0")
    await servidor.start()
    async with grpc.aio.insecure_channel(f"127.0.0.1:{puerto}") as canal_cliente:
        yield canal_cliente
    await servidor.stop(grace=None)


async def validar(canal: grpc.aio.Channel, competencia: str, tema: str, subtema: str, metadatos=None):
    stub = pb2_grpc.CatalogoAcademicoStub(canal)
    solicitud = pb2.ValidarClasificacionSolicitud(
        competencia_id=competencia, tema_id=tema, subtema_id=subtema
    )
    return await stub.ValidarClasificacion(solicitud, metadata=metadatos)


@pytest.mark.parametrize(
    "competencia,tema,subtema,valida,motivo",
    [
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_TENDENCIA_CENTRAL, True,
         pb2.MOTIVO_RECHAZO_NINGUNO),
        (INEXISTENTE, ids.TEMA_ESTADISTICA, ids.SUBTEMA_TENDENCIA_CENTRAL, False,
         pb2.MOTIVO_RECHAZO_COMPETENCIA_INEXISTENTE),
        (ids.COMPETENCIA_RAZONAMIENTO, INEXISTENTE, ids.SUBTEMA_TENDENCIA_CENTRAL, False,
         pb2.MOTIVO_RECHAZO_TEMA_INEXISTENTE),
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_PATRONES, ids.SUBTEMA_CREACIONALES, False,
         pb2.MOTIVO_RECHAZO_TEMA_NO_PERTENECE_A_COMPETENCIA),
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, INEXISTENTE, False,
         pb2.MOTIVO_RECHAZO_SUBTEMA_INEXISTENTE),
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_ECUACIONES, False,
         pb2.MOTIVO_RECHAZO_SUBTEMA_NO_PERTENECE_A_TEMA),
    ],
    ids=[
        "valida",
        "competencia_inexistente",
        "tema_inexistente",
        "tema_no_pertenece_a_competencia",
        "subtema_inexistente",
        "subtema_no_pertenece_a_tema",
    ],
)
async def test_los_seis_resultados_se_responden_con_ok(
    canal: grpc.aio.Channel, competencia: str, tema: str, subtema: str, valida: bool, motivo: int
) -> None:
    respuesta = await validar(canal, competencia, tema, subtema)
    assert respuesta.valida is valida
    assert respuesta.motivo == motivo
    assert respuesta.detalle


@pytest.mark.parametrize("campo", ["competencia", "tema", "subtema"])
@pytest.mark.parametrize("mal_formado", ["", "abc", "22222222-2222-4222-8222-00000000010"])
async def test_un_id_que_no_es_uuid_es_invalid_argument_con_mensaje_en_espanol(
    canal: grpc.aio.Channel, campo: str, mal_formado: str
) -> None:
    ternas = {
        "competencia": ids.COMPETENCIA_RAZONAMIENTO,
        "tema": ids.TEMA_ESTADISTICA,
        "subtema": ids.SUBTEMA_TENDENCIA_CENTRAL,
    }
    ternas[campo] = mal_formado
    with pytest.raises(grpc.aio.AioRpcError) as error:
        await validar(canal, **ternas)
    assert error.value.code() == grpc.StatusCode.INVALID_ARGUMENT
    assert "UUID" in error.value.details()


async def test_el_enum_de_dominio_se_convierte_al_del_proto_valor_por_valor() -> None:
    for motivo in MotivoRechazoClasificacion:
        proto = catalogo_academico_servicer.motivo_a_proto(motivo)
        assert pb2.MotivoRechazo.Name(proto) == "MOTIVO_RECHAZO_" + motivo.name
        assert proto == motivo.value


async def test_lee_el_metadato_x_id_correlacion(
    canal: grpc.aio.Channel, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    correlacion = "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b"
    await validar(
        canal, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD,
        metadatos=((METADATO_CORRELACION, correlacion),),
    )
    assert ejecutor_en_memoria.correlaciones_vistas == [correlacion]


async def test_sin_metadato_genera_una_correlacion_nueva(
    canal: grpc.aio.Channel, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    await validar(canal, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD)
    (generada,) = ejecutor_en_memoria.correlaciones_vistas
    assert len(generada) == 36


async def test_sin_base_de_datos_responde_unavailable(
    canal: grpc.aio.Channel, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    ejecutor_en_memoria.falla = BaseDeDatosNoDisponibleExcepcion("sin base")
    with pytest.raises(grpc.aio.AioRpcError) as error:
        await validar(canal, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD)
    assert error.value.code() == grpc.StatusCode.UNAVAILABLE


async def test_un_error_inesperado_responde_internal_sin_exponer_detalles(
    canal: grpc.aio.Channel, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    ejecutor_en_memoria.falla = RuntimeError("secreto interno")
    with pytest.raises(grpc.aio.AioRpcError) as error:
        await validar(canal, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD)
    assert error.value.code() == grpc.StatusCode.INTERNAL
    assert "secreto" not in error.value.details()


async def test_la_reflexion_esta_activada_y_lista_el_servicio(canal: grpc.aio.Channel) -> None:
    stub = reflection_pb2_grpc.ServerReflectionStub(canal)
    llamada = stub.ServerReflectionInfo(iter([reflection_pb2.ServerReflectionRequest(list_services="")]))
    servicios = []
    async for respuesta in llamada:
        servicios += [s.name for s in respuesta.list_services_response.service]
        break
    assert NOMBRE_SERVICIO_GRPC == "bancopreguntas.catalogo.v1.CatalogoAcademico"
    assert NOMBRE_SERVICIO_GRPC in servicios


async def test_un_uuid_en_mayusculas_se_acepta_y_se_normaliza(canal: grpc.aio.Channel) -> None:
    respuesta = await validar(
        canal, ids.COMPETENCIA_RAZONAMIENTO.upper(), ids.TEMA_ESTADISTICA.upper(), ids.SUBTEMA_PROBABILIDAD.upper()
    )
    assert respuesta.valida is True


@pytest.mark.parametrize(
    "forma",
    ["22222222222242228222000000000101", "{22222222-2222-4222-8222-000000000101}",
     "urn:uuid:22222222-2222-4222-8222-000000000101"],
    ids=["sin_guiones", "con_llaves", "urn"],
)
async def test_otras_formas_de_uuid_son_invalid_argument(canal: grpc.aio.Channel, forma: str) -> None:
    with pytest.raises(grpc.aio.AioRpcError) as error:
        await validar(canal, forma, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD)
    assert error.value.code() == grpc.StatusCode.INVALID_ARGUMENT


async def test_internal_no_expone_trazas_ni_clases_internas(
    canal: grpc.aio.Channel, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    ejecutor_en_memoria.falla = RuntimeError("secreto interno")
    with pytest.raises(grpc.aio.AioRpcError) as error:
        await validar(canal, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD)
    detalle = error.value.details()
    assert error.value.code() == grpc.StatusCode.INTERNAL
    assert detalle == "Error interno del catálogo."
    assert "Traceback" not in detalle and "RuntimeError" not in detalle
