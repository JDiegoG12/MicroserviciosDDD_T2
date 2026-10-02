"""Pruebas de ``CrearCompetenciaCasoUso`` (D-11, INV-24)."""

import pytest

from catalogo.aplicacion.casos_uso.crear_competencia_caso_uso import CrearCompetenciaCasoUso
from catalogo.aplicacion.dtos.comandos import CrearCompetenciaComando
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.eventos import CompetenciaCreada
from catalogo.dominio.excepciones import NombreDuplicadoExcepcion, SolicitudInvalidaExcepcion
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria


@pytest.fixture
def caso_uso(
    repositorio: CompetenciaRepositorioEnMemoria, publicador: PublicadorEventosEnMemoria
) -> CrearCompetenciaCasoUso:
    return CrearCompetenciaCasoUso(repositorio, publicador)


def test_crea_la_competencia_la_guarda_y_devuelve_su_respuesta(
    caso_uso: CrearCompetenciaCasoUso,
    repositorio: CompetenciaRepositorioEnMemoria,
    administrador: UsuarioActual,
) -> None:
    respuesta = caso_uso.ejecutar(
        administrador, CrearCompetenciaComando("Comunicación escrita", "Lectura y escritura")
    )
    assert respuesta.nombre == "Comunicación escrita"
    assert respuesta.descripcion == "Lectura y escritura"
    assert respuesta.temas == ()
    guardada = repositorio.listar_todas()
    assert [str(c.id) for c in guardada] == [respuesta.competencia_id]


def test_la_descripcion_es_opcional(
    caso_uso: CrearCompetenciaCasoUso, administrador: UsuarioActual
) -> None:
    assert caso_uso.ejecutar(administrador, CrearCompetenciaComando("A")).descripcion is None


def test_entrega_el_evento_competencia_creada_al_publicador(
    caso_uso: CrearCompetenciaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    respuesta = caso_uso.ejecutar(administrador, CrearCompetenciaComando("A"))
    assert len(publicador.eventos_publicados) == 1
    evento = publicador.eventos_publicados[0]
    assert isinstance(evento, CompetenciaCreada)
    assert str(evento.competencia_id) == respuesta.competencia_id


@pytest.mark.parametrize("repetido", ["Diseño de software", "diseño DE software", "  diseño  de   SOFTWARE "])
def test_inv24_nombre_duplicado_difiera_en_mayusculas_tildes_o_espacios(
    caso_uso: CrearCompetenciaCasoUso,
    repositorio: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
    repetido: str,
) -> None:
    caso_uso.ejecutar(administrador, CrearCompetenciaComando("Diseño de software"))
    publicador.eventos_publicados.clear()
    with pytest.raises(NombreDuplicadoExcepcion) as error:
        caso_uso.ejecutar(administrador, CrearCompetenciaComando(repetido))
    assert error.value.codigo == "NOMBRE_DUPLICADO"
    assert len(repositorio.listar_todas()) == 1
    assert publicador.eventos_publicados == []


@pytest.mark.parametrize("nombre", ["", "   ", "x" * 121])
def test_nombre_vacio_o_de_mas_de_120_caracteres_es_solicitud_invalida(
    caso_uso: CrearCompetenciaCasoUso, administrador: UsuarioActual, nombre: str
) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion) as error:
        caso_uso.ejecutar(administrador, CrearCompetenciaComando(nombre))
    assert error.value.codigo == "SOLICITUD_INVALIDA"


def test_descripcion_de_mas_de_500_caracteres_es_solicitud_invalida(
    caso_uso: CrearCompetenciaCasoUso,
    repositorio: CompetenciaRepositorioEnMemoria,
    administrador: UsuarioActual,
) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        caso_uso.ejecutar(administrador, CrearCompetenciaComando("A", "x" * 501))
    assert repositorio.esta_vacio()
