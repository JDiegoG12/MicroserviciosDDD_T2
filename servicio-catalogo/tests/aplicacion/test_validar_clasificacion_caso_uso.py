"""Pruebas de ``ValidarClasificacionCasoUso``: los seis resultados y los ids mal formados."""

import pytest

from catalogo.aplicacion.casos_uso.validar_clasificacion_caso_uso import ValidarClasificacionCasoUso
from catalogo.aplicacion.dtos.comandos import ValidarClasificacionConsulta
from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion
from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion as Motivo
from tests import ids_catalogo_semilla as ids
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria

INEXISTENTE = "99999999-9999-4999-8999-999999999999"


@pytest.fixture
def caso_uso(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
) -> ValidarClasificacionCasoUso:
    return ValidarClasificacionCasoUso(repositorio_con_semilla)


@pytest.mark.parametrize(
    "competencia,tema,subtema,valida,motivo",
    [
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_TENDENCIA_CENTRAL, True, Motivo.NINGUNO),
        (ids.COMPETENCIA_DISENO, ids.TEMA_ARQUITECTURA, ids.SUBTEMA_MICROSERVICIOS, True, Motivo.NINGUNO),
        (INEXISTENTE, ids.TEMA_ESTADISTICA, ids.SUBTEMA_TENDENCIA_CENTRAL, False, Motivo.COMPETENCIA_INEXISTENTE),
        (ids.COMPETENCIA_RAZONAMIENTO, INEXISTENTE, ids.SUBTEMA_TENDENCIA_CENTRAL, False, Motivo.TEMA_INEXISTENTE),
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_PATRONES, ids.SUBTEMA_CREACIONALES, False, Motivo.TEMA_NO_PERTENECE_A_COMPETENCIA),
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, INEXISTENTE, False, Motivo.SUBTEMA_INEXISTENTE),
        (ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_ECUACIONES, False, Motivo.SUBTEMA_NO_PERTENECE_A_TEMA),
    ],
    ids=[
        "valida_razonamiento",
        "valida_diseno",
        "competencia_inexistente",
        "tema_inexistente",
        "tema_no_pertenece_a_competencia",
        "subtema_inexistente",
        "subtema_no_pertenece_a_tema",
    ],
)
def test_los_seis_resultados_de_la_validacion(
    caso_uso: ValidarClasificacionCasoUso,
    competencia: str,
    tema: str,
    subtema: str,
    valida: bool,
    motivo: Motivo,
) -> None:
    respuesta = caso_uso.ejecutar(ValidarClasificacionConsulta(competencia, tema, subtema))
    assert respuesta.valida is valida
    assert respuesta.motivo is motivo
    assert respuesta.detalle


@pytest.mark.parametrize("campo", ["competencia_id", "tema_id", "subtema_id"])
@pytest.mark.parametrize("mal_formado", ["", "abc", "1234", "22222222-2222-4222-8222-00000000010"])
def test_un_id_mal_formado_es_solicitud_invalida_y_no_un_motivo_de_rechazo(
    caso_uso: ValidarClasificacionCasoUso, campo: str, mal_formado: str
) -> None:
    ternas = {
        "competencia_id": ids.COMPETENCIA_RAZONAMIENTO,
        "tema_id": ids.TEMA_ESTADISTICA,
        "subtema_id": ids.SUBTEMA_TENDENCIA_CENTRAL,
    }
    ternas[campo] = mal_formado
    with pytest.raises(SolicitudInvalidaExcepcion) as error:
        caso_uso.ejecutar(ValidarClasificacionConsulta(**ternas))
    assert error.value.codigo == "SOLICITUD_INVALIDA"


def test_no_exige_rol_porque_es_una_llamada_interna_entre_servicios(
    caso_uso: ValidarClasificacionCasoUso,
) -> None:
    # La firma de ejecutar no recibe UsuarioActual: no hay nada que verificar.
    respuesta = caso_uso.ejecutar(
        ValidarClasificacionConsulta(
            ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD
        )
    )
    assert respuesta.valida is True


def test_la_respuesta_no_devuelve_nombres_del_catalogo(
    caso_uso: ValidarClasificacionCasoUso,
) -> None:
    respuesta = caso_uso.ejecutar(
        ValidarClasificacionConsulta(
            ids.COMPETENCIA_RAZONAMIENTO, ids.TEMA_PATRONES, ids.SUBTEMA_CREACIONALES
        )
    )
    assert "Patrones" not in respuesta.detalle and "Razonamiento" not in respuesta.detalle
