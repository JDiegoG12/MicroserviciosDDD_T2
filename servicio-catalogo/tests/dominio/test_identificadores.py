"""Pruebas de los identificadores (INV-22, SOLICITUD_INVALIDA por UUID mal formado)."""

from uuid import UUID

import pytest

from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId

TEXTO_VALIDO = "22222222-2222-4222-8222-000000000101"


def test_generar_crea_identificadores_distintos() -> None:
    assert CompetenciaId.generar() != CompetenciaId.generar()


def test_desde_texto_conserva_el_uuid() -> None:
    assert str(CompetenciaId.desde_texto(TEXTO_VALIDO)) == TEXTO_VALIDO


def test_desde_texto_normaliza_a_minuscula() -> None:
    assert str(TemaId.desde_texto("AAAAAAAA-2222-4222-8222-000000000101")) == (
        "aaaaaaaa-2222-4222-8222-000000000101"
    )


@pytest.mark.parametrize(
    "texto",
    [
        "",
        "no-es-un-uuid",
        "22222222-2222-4222-8222-00000000010",  # un dígito menos
        "22222222222242228222000000000101",  # sin guiones
        "{22222222-2222-4222-8222-000000000101}",  # con llaves
        "urn:uuid:22222222-2222-4222-8222-000000000101",
    ],
)
def test_desde_texto_rechaza_uuid_mal_formado(texto: str) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion) as error:
        SubtemaId.desde_texto(texto)
    assert error.value.codigo == "SOLICITUD_INVALIDA"


def test_desde_texto_rechaza_valor_que_no_es_texto() -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        CompetenciaId.desde_texto(None)  # type: ignore[arg-type]


def test_construir_con_valor_que_no_es_uuid_se_rechaza() -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        CompetenciaId("no-es-uuid")  # type: ignore[arg-type]


def test_igualdad_por_valor_y_tipo() -> None:
    uuid = UUID(TEXTO_VALIDO)
    assert CompetenciaId(uuid) == CompetenciaId(uuid)
    assert hash(CompetenciaId(uuid)) == hash(CompetenciaId(uuid))
    assert CompetenciaId(uuid) != TemaId(uuid)
