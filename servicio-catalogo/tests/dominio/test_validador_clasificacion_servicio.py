"""Pruebas de ``ValidadorClasificacionServicio``: los seis resultados, con los ids de CONTRATOS 4.3."""

import pytest

from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion
from catalogo.dominio.validador_clasificacion_servicio import ValidadorClasificacionServicio
from tests import ids_catalogo_semilla as ids
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria

INEXISTENTE = "99999999-9999-4999-8999-999999999999"


@pytest.fixture
def validador(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
) -> ValidadorClasificacionServicio:
    return ValidadorClasificacionServicio(repositorio_con_semilla)


def validar(
    validador: ValidadorClasificacionServicio, competencia: str, tema: str, subtema: str
):
    return validador.validar(
        CompetenciaId.desde_texto(competencia),
        TemaId.desde_texto(tema),
        SubtemaId.desde_texto(subtema),
    )


def test_resultado_1_terna_valida(validador: ValidadorClasificacionServicio) -> None:
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_TENDENCIA_CENTRAL
    )
    assert resultado.valida is True
    assert resultado.motivo is MotivoRechazoClasificacion.NINGUNO


def test_resultado_2_competencia_inexistente(validador: ValidadorClasificacionServicio) -> None:
    resultado = validar(
        validador, INEXISTENTE, ids.TEMA_ESTADISTICA, ids.SUBTEMA_TENDENCIA_CENTRAL
    )
    assert resultado.valida is False
    assert resultado.motivo is MotivoRechazoClasificacion.COMPETENCIA_INEXISTENTE
    assert resultado.detalle


def test_resultado_3_tema_inexistente(validador: ValidadorClasificacionServicio) -> None:
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, INEXISTENTE, ids.SUBTEMA_TENDENCIA_CENTRAL
    )
    assert resultado.valida is False
    assert resultado.motivo is MotivoRechazoClasificacion.TEMA_INEXISTENTE


def test_resultado_4_tema_que_existe_en_otra_competencia(
    validador: ValidadorClasificacionServicio,
) -> None:
    # "Patrones de diseño" pertenece a Diseño de software, no a Razonamiento cuantitativo.
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_PATRONES, ids.SUBTEMA_CREACIONALES
    )
    assert resultado.valida is False
    assert resultado.motivo is MotivoRechazoClasificacion.TEMA_NO_PERTENECE_A_COMPETENCIA


def test_resultado_5_subtema_inexistente(validador: ValidadorClasificacionServicio) -> None:
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, INEXISTENTE
    )
    assert resultado.valida is False
    assert resultado.motivo is MotivoRechazoClasificacion.SUBTEMA_INEXISTENTE


def test_resultado_6_subtema_que_existe_en_otro_tema(
    validador: ValidadorClasificacionServicio,
) -> None:
    # "Ecuaciones lineales" pertenece a Álgebra, no a Estadística.
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_ECUACIONES
    )
    assert resultado.valida is False
    assert resultado.motivo is MotivoRechazoClasificacion.SUBTEMA_NO_PERTENECE_A_TEMA


def test_subtema_de_otra_competencia_tambien_es_no_pertenece_a_tema(
    validador: ValidadorClasificacionServicio,
) -> None:
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_MICROSERVICIOS
    )
    assert resultado.motivo is MotivoRechazoClasificacion.SUBTEMA_NO_PERTENECE_A_TEMA


# --------------------------------------------------------- orden de verificación


def test_orden_competencia_inexistente_gana_sobre_tema_y_subtema_inexistentes(
    validador: ValidadorClasificacionServicio,
) -> None:
    resultado = validar(validador, INEXISTENTE, INEXISTENTE, INEXISTENTE)
    assert resultado.motivo is MotivoRechazoClasificacion.COMPETENCIA_INEXISTENTE


def test_orden_tema_inexistente_gana_sobre_subtema_inexistente(
    validador: ValidadorClasificacionServicio,
) -> None:
    resultado = validar(validador, ids.COMPETENCIA_RAZONAMIENTO, INEXISTENTE, INEXISTENTE)
    assert resultado.motivo is MotivoRechazoClasificacion.TEMA_INEXISTENTE


def test_orden_tema_ajeno_gana_sobre_subtema_inexistente(
    validador: ValidadorClasificacionServicio,
) -> None:
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_PATRONES, INEXISTENTE
    )
    assert resultado.motivo is MotivoRechazoClasificacion.TEMA_NO_PERTENECE_A_COMPETENCIA


def test_el_detalle_no_incluye_nombres_del_catalogo(
    validador: ValidadorClasificacionServicio,
) -> None:
    resultado = validar(
        validador, ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_PATRONES, ids.SUBTEMA_CREACIONALES
    )
    assert "Patrones de diseño" not in resultado.detalle
    assert "Razonamiento cuantitativo" not in resultado.detalle
