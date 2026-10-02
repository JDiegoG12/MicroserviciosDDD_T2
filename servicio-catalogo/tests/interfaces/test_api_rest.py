"""Pruebas de la API REST con el ejecutor en memoria (sin base de datos real)."""

from __future__ import annotations

import logging
import re
from collections.abc import AsyncIterator, Iterator
from uuid import UUID

import httpx
import pytest
import pytest_asyncio

from catalogo.aplicacion.excepciones import BaseDeDatosNoDisponibleExcepcion
from catalogo.dominio.excepciones import CatalogoExcepcion
from catalogo.infraestructura.registro_log import configurar_registro_log
from catalogo.interfaces.rest.aplicacion_fastapi import crear_aplicacion
from tests import ids_catalogo_semilla as ids
from tests.dobles.ejecutor_transaccional_en_memoria import EjecutorTransaccionalEnMemoria

BASE = "/api/v1/competencias"
RAZ = ids.COMPETENCIA_RAZONAMIENTO
INEXISTENTE = "99999999-9999-4999-8999-999999999999"
ADMIN = {"X-Usuario-Id": "11111111-1111-4111-8111-000000000001", "X-Roles": "ADMINISTRADOR"}
ESTUDIANTE = {"X-Usuario-Id": "11111111-1111-4111-8111-000000000006", "X-Roles": "ESTUDIANTE"}


@pytest_asyncio.fixture
async def cliente(ejecutor_en_memoria: EjecutorTransaccionalEnMemoria) -> AsyncIterator[httpx.AsyncClient]:
    aplicacion = crear_aplicacion(ejecutor_en_memoria)
    transporte = httpx.ASGITransport(app=aplicacion)
    async with httpx.AsyncClient(transport=transporte, base_url="http://prueba") as cliente_http:
        yield cliente_http


@pytest.fixture
def registro_restaurado() -> Iterator[None]:
    raiz = logging.getLogger()
    manejadores, nivel = list(raiz.handlers), raiz.level
    yield
    raiz.handlers[:] = manejadores
    raiz.setLevel(nivel)


# Los 8 endpoints de CONTRATOS 8.2: (método, ruta, cuerpo, ¿escritura?, éxito)
ENDPOINTS = [
    ("POST", BASE, {"nombre": "Nueva"}, True, 201),
    ("GET", BASE, None, False, 200),
    ("GET", f"{BASE}/{RAZ}", None, False, 200),
    ("PUT", f"{BASE}/{RAZ}", {"nombre": "Otro nombre"}, True, 200),
    ("POST", f"{BASE}/{RAZ}/temas", {"nombre": "Geometría"}, True, 201),
    ("PUT", f"{BASE}/{RAZ}/temas/{ids.TEMA_ESTADISTICA}", {"nombre": "Estadística básica"}, True, 200),
    ("POST", f"{BASE}/{RAZ}/temas/{ids.TEMA_ESTADISTICA}/subtemas", {"nombre": "Regresión"}, True, 201),
    (
        "PUT",
        f"{BASE}/{RAZ}/temas/{ids.TEMA_ESTADISTICA}/subtemas/{ids.SUBTEMA_PROBABILIDAD}",
        {"nombre": "Probabilidad básica"},
        True,
        200,
    ),
]
IDS_ENDPOINTS = [
    "crear_competencia", "listar", "obtener", "renombrar_competencia", "agregar_tema",
    "renombrar_tema", "agregar_subtema", "renombrar_subtema",
]
ESCRITURAS = [e for e in ENDPOINTS if e[3]]
IDS_ESCRITURAS = [i for i, e in zip(IDS_ENDPOINTS, ENDPOINTS) if e[3]]


async def llamar(cliente: httpx.AsyncClient, metodo: str, ruta: str, cuerpo=None, encabezados=None):
    return await cliente.request(metodo, ruta, json=cuerpo, headers=encabezados or {})


def es_problema(respuesta: httpx.Response, estado: int, codigo: str) -> dict:
    assert respuesta.status_code == estado, respuesta.text
    assert respuesta.headers["content-type"].startswith("application/problem+json")
    cuerpo = respuesta.json()
    assert cuerpo["codigo"] == codigo
    assert cuerpo["status"] == estado
    return cuerpo


# ------------------------------------------------------------------- casos felices


@pytest.mark.parametrize("metodo,ruta,cuerpo,escritura,exito", ENDPOINTS, ids=IDS_ENDPOINTS)
async def test_cada_endpoint_responde_su_caso_feliz(
    cliente: httpx.AsyncClient, metodo: str, ruta: str, cuerpo, escritura: bool, exito: int
) -> None:
    respuesta = await llamar(cliente, metodo, ruta, cuerpo, ADMIN)
    assert respuesta.status_code == exito, respuesta.text
    assert respuesta.headers["content-type"].startswith("application/json")
    assert UUID(respuesta.headers["x-id-correlacion"])


async def test_crear_competencia_responde_201_con_location_y_forma_de_8_2(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "POST", BASE, {"nombre": "Comunicación escrita", "descripcion": "Texto"}, ADMIN)
    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert set(cuerpo) == {"competenciaId", "nombre", "descripcion", "temas"}
    assert cuerpo["nombre"] == "Comunicación escrita" and cuerpo["descripcion"] == "Texto"
    assert cuerpo["temas"] == []
    assert respuesta.headers["location"] == f"{BASE}/{cuerpo['competenciaId']}"


async def test_listar_devuelve_las_dos_competencias_sembradas_con_su_jerarquia_y_ids_de_4_3(
    cliente: httpx.AsyncClient,
) -> None:
    respuesta = await llamar(cliente, "GET", BASE, None, ESTUDIANTE)
    assert respuesta.status_code == 200
    lista = respuesta.json()
    assert [c["competenciaId"] for c in lista] == [ids.COMPETENCIA_DISENO, ids.COMPETENCIA_RAZONAMIENTO]
    razonamiento = lista[1]
    assert [t["temaId"] for t in razonamiento["temas"]] == [ids.TEMA_ALGEBRA, ids.TEMA_ESTADISTICA]
    estadistica = razonamiento["temas"][1]
    assert estadistica["subtemas"] == [
        {"subtemaId": ids.SUBTEMA_TENDENCIA_CENTRAL, "nombre": "Medidas de tendencia central"},
        {"subtemaId": ids.SUBTEMA_PROBABILIDAD, "nombre": "Probabilidad"},
    ]


async def test_agregar_tema_y_subtema_devuelven_la_competencia_completa(cliente: httpx.AsyncClient) -> None:
    tema = await llamar(cliente, "POST", f"{BASE}/{RAZ}/temas", {"nombre": "Geometría"}, ADMIN)
    assert tema.status_code == 201
    assert tema.headers["location"] == f"{BASE}/{RAZ}"
    nuevo = next(t for t in tema.json()["temas"] if t["nombre"] == "Geometría")
    subtema = await llamar(
        cliente, "POST", f"{BASE}/{RAZ}/temas/{nuevo['temaId']}/subtemas", {"nombre": "Triángulos"}, ADMIN
    )
    assert subtema.status_code == 201
    creado = next(t for t in subtema.json()["temas"] if t["nombre"] == "Geometría")
    assert [s["nombre"] for s in creado["subtemas"]] == ["Triángulos"]


async def test_los_campos_desconocidos_del_cuerpo_se_ignoran(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(
        cliente, "POST", f"{BASE}/{RAZ}/temas", {"nombre": "Geometría", "descripcion": "no aplica", "otro": 1}, ADMIN
    )
    assert respuesta.status_code == 201


async def test_el_cuerpo_acepta_el_nombre_con_espacios_de_los_extremos(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "POST", BASE, {"nombre": "   Nueva   "}, ADMIN)
    assert respuesta.json()["nombre"] == "Nueva"


@pytest.mark.parametrize(
    "cuerpo,esperado",
    [
        ({"nombre": "Razonamiento cuantitativo"}, "Algo"),  # ausente: se conserva
        ({"nombre": "Razonamiento cuantitativo", "descripcion": None}, None),  # null: se borra
        ({"nombre": "Razonamiento cuantitativo", "descripcion": ""}, None),  # "": se borra
        ({"nombre": "Razonamiento cuantitativo", "descripcion": "Nueva"}, "Nueva"),  # texto: reemplaza
    ],
    ids=["ausente_conserva", "null_borra", "vacia_borra", "texto_reemplaza"],
)
async def test_put_de_competencia_distingue_descripcion_ausente_null_y_vacia(
    cliente: httpx.AsyncClient, cuerpo: dict, esperado
) -> None:
    await llamar(cliente, "PUT", f"{BASE}/{RAZ}", {"nombre": "Razonamiento cuantitativo", "descripcion": "Algo"}, ADMIN)
    respuesta = await llamar(cliente, "PUT", f"{BASE}/{RAZ}", cuerpo, ADMIN)
    assert respuesta.status_code == 200
    assert respuesta.json()["descripcion"] == esperado


async def test_renombrar_con_una_variante_actualiza_el_nombre_guardado(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "PUT", f"{BASE}/{RAZ}", {"nombre": "RAZONAMIENTO cuantitativo"}, ADMIN)
    assert respuesta.status_code == 200
    assert respuesta.json()["nombre"] == "RAZONAMIENTO cuantitativo"
    assert respuesta.json()["competenciaId"] == RAZ


# ------------------------------------------------------------- 401 (autenticación)


@pytest.mark.parametrize("metodo,ruta,cuerpo,escritura,exito", ENDPOINTS, ids=IDS_ENDPOINTS)
@pytest.mark.parametrize(
    "encabezados",
    [
        {},
        {"X-Usuario-Id": ADMIN["X-Usuario-Id"]},
        {"X-Roles": "ADMINISTRADOR"},
        {"X-Usuario-Id": ADMIN["X-Usuario-Id"], "X-Roles": ""},
        {"X-Usuario-Id": ADMIN["X-Usuario-Id"], "X-Roles": " , "},
    ],
    ids=["sin_encabezados", "sin_roles", "sin_usuario", "roles_vacios", "roles_solo_comas"],
)
async def test_sin_identidad_responde_401_no_autenticado(
    cliente: httpx.AsyncClient, metodo, ruta, cuerpo, escritura, exito, encabezados
) -> None:
    respuesta = await llamar(cliente, metodo, ruta, cuerpo, encabezados)
    es_problema(respuesta, 401, "NO_AUTENTICADO")


async def test_la_falta_de_identidad_gana_sobre_un_cuerpo_invalido(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "POST", BASE, {"nombre": ""}, {})
    es_problema(respuesta, 401, "NO_AUTENTICADO")


# ------------------------------------------------------------------ 403 (rol)


@pytest.mark.parametrize("metodo,ruta,cuerpo,escritura,exito", ESCRITURAS, ids=IDS_ESCRITURAS)
@pytest.mark.parametrize("roles", ["ESTUDIANTE", "AUTOR", "DOCENTE", "AUTOR,DOCENTE", "REVISOR"])
async def test_escritura_sin_rol_administrador_responde_403(
    cliente: httpx.AsyncClient, metodo, ruta, cuerpo, escritura, exito, roles: str
) -> None:
    respuesta = await llamar(cliente, metodo, ruta, cuerpo, {**ADMIN, "X-Roles": roles})
    es_problema(respuesta, 403, "ACCESO_DENEGADO")


async def test_varios_roles_que_incluyen_administrador_pueden_escribir(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "POST", BASE, {"nombre": "Nueva"}, {**ADMIN, "X-Roles": "AUTOR, ADMINISTRADOR"})
    assert respuesta.status_code == 201


@pytest.mark.parametrize("rol", ["ADMINISTRADOR", "AUTOR", "REVISOR", "DOCENTE", "ESTUDIANTE"])
async def test_las_lecturas_las_puede_hacer_cualquier_rol(cliente: httpx.AsyncClient, rol: str) -> None:
    respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Roles": rol})
    assert respuesta.status_code == 200


# ------------------------------------------------------------------ 400 (forma)


async def test_usuario_id_que_no_es_uuid_responde_400(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Usuario-Id": "no-es-uuid"})
    es_problema(respuesta, 400, "SOLICITUD_INVALIDA")


async def test_un_rol_desconocido_responde_400(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Roles": "ADMINISTRADOR,SUPERUSUARIO"})
    cuerpo = es_problema(respuesta, 400, "SOLICITUD_INVALIDA")
    assert "SUPERUSUARIO" in cuerpo["detail"]


@pytest.mark.parametrize(
    "cuerpo,campo",
    [
        ({}, "nombre"),
        ({"nombre": ""}, "nombre"),
        ({"nombre": "   "}, "nombre"),
        ({"nombre": "x" * 121}, "nombre"),
        ({"nombre": 123}, "nombre"),
        ({"nombre": None}, "nombre"),
        ({"nombre": "Bien", "descripcion": "x" * 501}, "descripcion"),
        ({"nombre": "Bien", "descripcion": 5}, "descripcion"),
    ],
    ids=["sin_nombre", "vacio", "en_blanco", "121_caracteres", "numero", "nulo", "descripcion_501", "descripcion_numero"],
)
async def test_cuerpo_invalido_responde_400_con_la_lista_de_errores(
    cliente: httpx.AsyncClient, cuerpo: dict, campo: str
) -> None:
    respuesta = await llamar(cliente, "POST", BASE, cuerpo, ADMIN)
    problema = es_problema(respuesta, 400, "SOLICITUD_INVALIDA")
    assert [e["campo"] for e in problema["errores"]] == [campo]
    assert problema["errores"][0]["mensaje"]


async def test_nombre_de_exactamente_120_caracteres_es_valido(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "POST", BASE, {"nombre": "x" * 120}, ADMIN)
    assert respuesta.status_code == 201


async def test_json_mal_formado_responde_400(cliente: httpx.AsyncClient) -> None:
    respuesta = await cliente.post(BASE, content=b"{nombre:", headers={**ADMIN, "Content-Type": "application/json"})
    problema = es_problema(respuesta, 400, "SOLICITUD_INVALIDA")
    assert problema["errores"] == [{"campo": "cuerpo", "mensaje": "El cuerpo no es un JSON válido."}]


async def test_el_nombre_vacio_se_informa_con_un_mensaje_claro(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "POST", BASE, {"nombre": "  "}, ADMIN)
    problema = es_problema(respuesta, 400, "SOLICITUD_INVALIDA")
    assert problema["errores"] == [{"campo": "nombre", "mensaje": "No puede estar vacío."}]


async def test_un_cuerpo_con_bytes_que_no_son_utf8_responde_400(cliente: httpx.AsyncClient) -> None:
    respuesta = await cliente.post(
        BASE, content=b'{"nombre": "Comunicaci' + bytes([0xF3]) + b'n"}', headers={**ADMIN, "Content-Type": "application/json"}
    )
    problema = es_problema(respuesta, 400, "SOLICITUD_INVALIDA")
    assert "UTF-8" in problema["detail"]


async def test_cuerpo_que_no_es_un_objeto_responde_400(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "POST", BASE, ["a"], ADMIN)
    es_problema(respuesta, 400, "SOLICITUD_INVALIDA")


@pytest.mark.parametrize(
    "ruta",
    [f"{BASE}/no-es-uuid", f"{BASE}/{RAZ.replace('-', '')}", f"{BASE}/{{{RAZ}}}", f"{BASE}/{RAZ}/temas/xyz"],
    ids=["texto", "sin_guiones", "con_llaves", "tema_mal_formado"],
)
async def test_un_id_de_ruta_mal_formado_responde_400(cliente: httpx.AsyncClient, ruta: str) -> None:
    respuesta = await llamar(cliente, "PUT", ruta, {"nombre": "Otro"}, ADMIN)
    es_problema(respuesta, 400, "SOLICITUD_INVALIDA")


async def test_un_id_de_ruta_en_mayusculas_se_acepta(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "GET", f"{BASE}/{RAZ.upper()}", None, ADMIN)
    assert respuesta.status_code == 200
    assert respuesta.json()["competenciaId"] == RAZ


# ------------------------------------------------------------ 404 y 409 (negocio)


async def test_competencia_inexistente_responde_404(cliente: httpx.AsyncClient) -> None:
    for metodo, ruta, cuerpo in [
        ("GET", f"{BASE}/{INEXISTENTE}", None),
        ("PUT", f"{BASE}/{INEXISTENTE}", {"nombre": "x"}),
        ("POST", f"{BASE}/{INEXISTENTE}/temas", {"nombre": "x"}),
    ]:
        es_problema(await llamar(cliente, metodo, ruta, cuerpo, ADMIN), 404, "COMPETENCIA_NO_ENCONTRADA")


async def test_tema_inexistente_responde_404(cliente: httpx.AsyncClient) -> None:
    for metodo, ruta, cuerpo in [
        ("PUT", f"{BASE}/{RAZ}/temas/{INEXISTENTE}", {"nombre": "x"}),
        ("POST", f"{BASE}/{RAZ}/temas/{INEXISTENTE}/subtemas", {"nombre": "x"}),
        ("PUT", f"{BASE}/{RAZ}/temas/{ids.TEMA_PATRONES}", {"nombre": "x"}),  # tema de otra competencia
    ]:
        es_problema(await llamar(cliente, metodo, ruta, cuerpo, ADMIN), 404, "TEMA_NO_ENCONTRADO")


async def test_subtema_inexistente_o_de_otro_tema_responde_404(cliente: httpx.AsyncClient) -> None:
    for subtema in (INEXISTENTE, ids.SUBTEMA_ECUACIONES):
        ruta = f"{BASE}/{RAZ}/temas/{ids.TEMA_ESTADISTICA}/subtemas/{subtema}"
        es_problema(await llamar(cliente, "PUT", ruta, {"nombre": "x"}, ADMIN), 404, "SUBTEMA_NO_ENCONTRADO")


@pytest.mark.parametrize("repetido", ["Diseño de software", "diseño DE software", "  DISEÑO   de  software "])
async def test_nombre_de_competencia_repetido_responde_409(cliente: httpx.AsyncClient, repetido: str) -> None:
    respuesta = await llamar(cliente, "POST", BASE, {"nombre": repetido}, ADMIN)
    es_problema(respuesta, 409, "NOMBRE_DUPLICADO")


async def test_renombrar_con_el_nombre_de_otra_competencia_responde_409(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "PUT", f"{BASE}/{RAZ}", {"nombre": "diseño de software"}, ADMIN)
    es_problema(respuesta, 409, "NOMBRE_DUPLICADO")


async def test_nombres_repetidos_de_tema_y_subtema_responden_409(cliente: httpx.AsyncClient) -> None:
    es_problema(await llamar(cliente, "POST", f"{BASE}/{RAZ}/temas", {"nombre": "ESTADISTICA"}, ADMIN), 409, "NOMBRE_DUPLICADO")
    ruta = f"{BASE}/{RAZ}/temas/{ids.TEMA_ESTADISTICA}/subtemas"
    es_problema(await llamar(cliente, "POST", ruta, {"nombre": "probabilidad"}, ADMIN), 409, "NOMBRE_DUPLICADO")


async def test_la_enie_es_letra_propia_en_la_unicidad(cliente: httpx.AsyncClient) -> None:
    assert (await llamar(cliente, "POST", BASE, {"nombre": "Año"}, ADMIN)).status_code == 201
    assert (await llamar(cliente, "POST", BASE, {"nombre": "Ano"}, ADMIN)).status_code == 201
    es_problema(await llamar(cliente, "POST", BASE, {"nombre": " AÑO "}, ADMIN), 409, "NOMBRE_DUPLICADO")


# -------------------------------------------------- formato problem+json y mapeo 5.3

ESTADO_ESPERADO_POR_CODIGO = {
    "SOLICITUD_INVALIDA": 400,
    "NO_AUTENTICADO": 401,
    "ACCESO_DENEGADO": 403,
    "COMPETENCIA_NO_ENCONTRADA": 404,
    "TEMA_NO_ENCONTRADO": 404,
    "SUBTEMA_NO_ENCONTRADO": 404,
    "NOMBRE_DUPLICADO": 409,
    "BASE_DE_DATOS_NO_DISPONIBLE": 503,
}
"""Tabla 5.3 de CONTRATOS.md, restringida a los códigos del catálogo."""


def todas_las_excepciones() -> list[type[CatalogoExcepcion]]:
    pendientes, vistas = [CatalogoExcepcion], []
    while pendientes:
        clase = pendientes.pop()
        for hija in clase.__subclasses__():
            vistas.append(hija)
            pendientes.append(hija)
    return vistas


def test_todo_codigo_de_excepcion_del_servicio_esta_en_la_tabla_esperada() -> None:
    codigos = {clase.codigo for clase in todas_las_excepciones()}
    assert codigos == set(ESTADO_ESPERADO_POR_CODIGO)


@pytest.mark.parametrize("clase", sorted(todas_las_excepciones(), key=lambda c: c.codigo), ids=lambda c: c.codigo)
async def test_cada_codigo_de_error_se_traduce_al_http_de_la_tabla_5_3(
    cliente: httpx.AsyncClient, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria, clase: type[CatalogoExcepcion]
) -> None:
    ejecutor_en_memoria.falla = clase("Mensaje de prueba.")
    respuesta = await llamar(cliente, "GET", BASE, None, ADMIN)
    problema = es_problema(respuesta, ESTADO_ESPERADO_POR_CODIGO[clase.codigo], clase.codigo)
    assert problema["detail"] == "Mensaje de prueba."


async def test_el_formato_problem_json_incluye_todos_los_campos_de_5_2(cliente: httpx.AsyncClient) -> None:
    correlacion = "9a1b6f2c-1d3a-4e5f-8a7b-9c0d1e2f3a4b"
    respuesta = await llamar(
        cliente, "POST", BASE, {"nombre": ""}, {**ADMIN, "X-Id-Correlacion": correlacion}
    )
    problema = es_problema(respuesta, 400, "SOLICITUD_INVALIDA")
    assert set(problema) == {"type", "title", "status", "detail", "instance", "codigo", "idCorrelacion", "errores"}
    assert problema["type"] == "https://banco-preguntas/errores/SOLICITUD_INVALIDA"
    assert problema["instance"] == BASE
    assert problema["idCorrelacion"] == correlacion
    assert respuesta.headers["x-id-correlacion"] == correlacion


async def test_un_error_inesperado_responde_500_error_interno_sin_exponer_trazas(
    cliente: httpx.AsyncClient, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    ejecutor_en_memoria.falla = RuntimeError("secreto interno")
    respuesta = await llamar(cliente, "GET", BASE, None, ADMIN)
    problema = es_problema(respuesta, 500, "ERROR_INTERNO")
    assert "secreto" not in respuesta.text and "Traceback" not in respuesta.text
    assert problema["idCorrelacion"] == respuesta.headers["x-id-correlacion"]


async def test_sin_base_de_datos_responde_503_pero_salud_sigue_en_200(
    cliente: httpx.AsyncClient, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    ejecutor_en_memoria.falla = BaseDeDatosNoDisponibleExcepcion("sin base")
    es_problema(await llamar(cliente, "GET", BASE, None, ADMIN), 503, "BASE_DE_DATOS_NO_DISPONIBLE")
    assert (await cliente.get("/salud")).status_code == 200


async def test_una_ruta_inexistente_responde_problem_json_404_recurso_no_encontrado(cliente: httpx.AsyncClient) -> None:
    es_problema(await cliente.get("/api/v1/no-existe", headers=ADMIN), 404, "RECURSO_NO_ENCONTRADO")


async def test_un_metodo_no_permitido_responde_problem_json_405(cliente: httpx.AsyncClient) -> None:
    es_problema(await cliente.delete(BASE, headers=ADMIN), 405, "METODO_NO_PERMITIDO")


async def test_no_existe_ningun_endpoint_delete(cliente: httpx.AsyncClient) -> None:
    esquema = (await cliente.get("/openapi.json")).json()
    metodos = {m for ruta in esquema["paths"].values() for m in ruta}
    assert "delete" not in metodos


# ------------------------------------------------------------------- correlación


async def test_si_llega_x_id_correlacion_se_devuelve_el_mismo(cliente: httpx.AsyncClient) -> None:
    correlacion = "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b"
    respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Id-Correlacion": correlacion})
    assert respuesta.headers["x-id-correlacion"] == correlacion


async def test_si_no_llega_se_genera_un_uuid(cliente: httpx.AsyncClient) -> None:
    primera = await llamar(cliente, "GET", BASE, None, ADMIN)
    segunda = await llamar(cliente, "GET", BASE, None, ADMIN)
    assert UUID(primera.headers["x-id-correlacion"]) != UUID(segunda.headers["x-id-correlacion"])


async def test_si_la_correlacion_recibida_no_es_uuid_se_reemplaza(cliente: httpx.AsyncClient) -> None:
    respuesta = await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Id-Correlacion": "cualquiera"})
    assert UUID(respuesta.headers["x-id-correlacion"])


async def test_el_caso_de_uso_corre_con_la_correlacion_de_la_peticion(
    cliente: httpx.AsyncClient, ejecutor_en_memoria: EjecutorTransaccionalEnMemoria
) -> None:
    correlacion = "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b"
    await llamar(cliente, "GET", BASE, None, {**ADMIN, "X-Id-Correlacion": correlacion})
    assert ejecutor_en_memoria.correlaciones_vistas == [correlacion]


async def test_cada_linea_de_log_de_la_peticion_lleva_id_correlacion(
    cliente: httpx.AsyncClient, capsys: pytest.CaptureFixture[str], registro_restaurado: None
) -> None:
    configurar_registro_log("INFO")
    correlacion = "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b"
    await llamar(cliente, "POST", BASE, {"nombre": ""}, {**ADMIN, "X-Id-Correlacion": correlacion})
    # Se excluyen las líneas del propio cliente de prueba (httpx), que no son del servicio.
    lineas = [
        linea for linea in capsys.readouterr().out.splitlines() if " catalogo." in linea
    ]
    assert lineas, "no se escribió ninguna línea de log"
    assert all(f"idCorrelacion={correlacion}" in linea for linea in lineas), lineas
    assert any("POST /api/v1/competencias -> 400" in linea for linea in lineas)
    assert all(re.match(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2} (INFO|WARNING|ERROR)", linea) for linea in lineas)


# --------------------------------------------------- salud, Swagger y OpenAPI


async def test_salud_responde_200_sin_encabezados(cliente: httpx.AsyncClient) -> None:
    respuesta = await cliente.get("/salud")
    assert respuesta.status_code == 200
    assert respuesta.json() == {"estado": "OK", "servicio": "servicio-catalogo"}


async def test_swagger_y_openapi_estan_en_sus_rutas_por_defecto(cliente: httpx.AsyncClient) -> None:
    docs = await cliente.get("/docs")
    assert docs.status_code == 200 and "swagger" in docs.text.lower()
    openapi = await cliente.get("/openapi.json")
    assert openapi.status_code == 200
    assert openapi.json()["openapi"].startswith("3.")


async def test_openapi_documenta_los_8_endpoints_con_encabezados_ejemplos_y_errores(
    cliente: httpx.AsyncClient,
) -> None:
    esquema = (await cliente.get("/openapi.json")).json()
    operaciones = {
        (metodo.upper(), ruta): operacion
        for ruta, metodos in esquema["paths"].items()
        for metodo, operacion in metodos.items()
    }
    for metodo, ruta, _cuerpo, escritura, exito in ENDPOINTS:
        plantilla = re.sub(r"/[0-9a-f-]{36}", "/{}", ruta)
        coincidencia = [
            op for (m, r), op in operaciones.items()
            if m == metodo and re.sub(r"\{[^}]+\}", "{}", r) == plantilla
        ]
        assert coincidencia, f"falta {metodo} {ruta}"
        operacion = coincidencia[0]
        assert operacion["summary"] and operacion["description"]
        assert "D-11" in operacion["description"]
        encabezados = {p["name"] for p in operacion["parameters"] if p["in"] == "header"}
        assert encabezados == {"X-Usuario-Id", "X-Roles", "X-Id-Correlacion"}
        respuestas = operacion["responses"]
        assert str(exito) in respuestas
        assert "422" not in respuestas
        esperados = {"400", "401", "500", "503"} | ({"403"} if escritura else set())
        assert esperados <= set(respuestas), (metodo, ruta, set(respuestas))
        for estado in esperados:
            contenido = respuestas[estado]["content"]
            assert "application/problem+json" in contenido and "application/json" not in contenido
            assert contenido["application/problem+json"]["examples"]
    assert "HTTPValidationError" not in esquema["components"]["schemas"]
    assert "ProblemaRespuesta" in esquema["components"]["schemas"]


async def test_openapi_documenta_los_codigos_de_error_propios_de_cada_endpoint(cliente: httpx.AsyncClient) -> None:
    esquema = (await cliente.get("/openapi.json")).json()
    ruta = esquema["paths"]["/api/v1/competencias/{competencia_id}/temas/{tema_id}/subtemas/{subtema_id}"]["put"]
    ejemplos_404 = ruta["responses"]["404"]["content"]["application/problem+json"]["examples"]
    assert set(ejemplos_404) == {"COMPETENCIA_NO_ENCONTRADA", "TEMA_NO_ENCONTRADO", "SUBTEMA_NO_ENCONTRADO"}
    assert "NOMBRE_DUPLICADO" in ruta["responses"]["409"]["content"]["application/problem+json"]["examples"]
    crear = esquema["paths"]["/api/v1/competencias"]["post"]
    assert "Location" in crear["responses"]["201"]["description"]
    assert crear["requestBody"]["content"]["application/json"]["schema"]["$ref"].endswith("CompetenciaSolicitud")


async def test_los_ids_de_ruta_se_documentan_como_texto_con_ejemplo(cliente: httpx.AsyncClient) -> None:
    esquema = (await cliente.get("/openapi.json")).json()
    operacion = esquema["paths"]["/api/v1/competencias/{competencia_id}"]["get"]
    parametro = next(p for p in operacion["parameters"] if p["name"] == "competencia_id")
    assert parametro["schema"]["type"] == "string"
    assert parametro["examples"] if "examples" in parametro else parametro["schema"].get("examples")
