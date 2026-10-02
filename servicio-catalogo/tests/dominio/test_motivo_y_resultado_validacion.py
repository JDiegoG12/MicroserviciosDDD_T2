"""Pruebas de ``MotivoRechazoClasificacion`` y ``ResultadoValidacionClasificacion``."""

import pytest

from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion
from catalogo.dominio.resultado_validacion_clasificacion import ResultadoValidacionClasificacion


def test_motivos_con_el_mismo_orden_y_valores_que_motivo_rechazo_de_contratos_seccion_6() -> None:
    esperado = [
        ("NINGUNO", 0),
        ("COMPETENCIA_INEXISTENTE", 1),
        ("TEMA_INEXISTENTE", 2),
        ("TEMA_NO_PERTENECE_A_COMPETENCIA", 3),
        ("SUBTEMA_INEXISTENTE", 4),
        ("SUBTEMA_NO_PERTENECE_A_TEMA", 5),
    ]
    assert [(m.name, m.value) for m in MotivoRechazoClasificacion] == esperado


def test_resultado_aceptado() -> None:
    resultado = ResultadoValidacionClasificacion.aceptada()
    assert resultado.valida is True
    assert resultado.motivo is MotivoRechazoClasificacion.NINGUNO
    assert resultado.detalle


def test_resultado_rechazado() -> None:
    resultado = ResultadoValidacionClasificacion.rechazada(
        MotivoRechazoClasificacion.TEMA_INEXISTENTE, "El tema no existe."
    )
    assert resultado.valida is False
    assert resultado.motivo is MotivoRechazoClasificacion.TEMA_INEXISTENTE
    assert resultado.detalle == "El tema no existe."


def test_resultado_rechazado_con_motivo_ninguno_es_incoherente() -> None:
    with pytest.raises(ValueError):
        ResultadoValidacionClasificacion.rechazada(MotivoRechazoClasificacion.NINGUNO, "x")


def test_resultado_valido_con_motivo_de_rechazo_es_incoherente() -> None:
    with pytest.raises(ValueError):
        ResultadoValidacionClasificacion(True, MotivoRechazoClasificacion.TEMA_INEXISTENTE, "x")
