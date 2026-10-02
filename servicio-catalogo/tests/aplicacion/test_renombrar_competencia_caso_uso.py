"""Pruebas de ``RenombrarCompetenciaCasoUso`` (INV-22, INV-24)."""

import pytest

from catalogo.aplicacion.casos_uso.renombrar_competencia_caso_uso import (
    RenombrarCompetenciaCasoUso,
)
from catalogo.aplicacion.dtos.comandos import RenombrarCompetenciaComando
from catalogo.aplicacion.excepciones import CompetenciaNoEncontradaExcepcion
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.eventos import CompetenciaRenombrada
from catalogo.dominio.excepciones import NombreDuplicadoExcepcion, SolicitudInvalidaExcepcion
from tests import ids_catalogo_semilla as ids
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria


@pytest.fixture
def caso_uso(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
) -> RenombrarCompetenciaCasoUso:
    return RenombrarCompetenciaCasoUso(repositorio_con_semilla, publicador)


def test_inv22_renombra_conservando_el_identificador(
    caso_uso: RenombrarCompetenciaCasoUso,
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    respuesta = caso_uso.ejecutar(
        administrador,
        RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, "Razonamiento lógico"),
    )
    assert respuesta.competencia_id == ids.COMPETENCIA_RAZONAMIENTO
    assert respuesta.nombre == "Razonamiento lógico"
    assert [t.nombre for t in respuesta.temas] == ["Álgebra", "Estadística"]
    assert any(
        c.nombre.valor == "Razonamiento lógico" and str(c.id) == ids.COMPETENCIA_RAZONAMIENTO
        for c in repositorio_con_semilla.listar_todas()
    )
    assert [type(e) for e in publicador.eventos_publicados] == [CompetenciaRenombrada]


@pytest.mark.parametrize("identico", ["Razonamiento cuantitativo", "  Razonamiento cuantitativo "])
def test_renombrar_con_el_mismo_texto_no_cambia_nada_ni_emite_evento(
    caso_uso: RenombrarCompetenciaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
    identico: str,
) -> None:
    respuesta = caso_uso.ejecutar(
        administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, identico)
    )
    assert respuesta.nombre == "Razonamiento cuantitativo"
    assert publicador.eventos_publicados == []


@pytest.mark.parametrize(
    "variante", ["razonamiento cuantitativo", "RAZONAMIENTO   CUANTITATIVO", "Razonamiento Cuantitativo"]
)
def test_renombrar_con_una_variante_actualiza_el_texto_guardado_sin_ser_duplicado(
    caso_uso: RenombrarCompetenciaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
    variante: str,
) -> None:
    respuesta = caso_uso.ejecutar(
        administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, variante)
    )
    assert respuesta.nombre == variante.strip()
    assert [type(e) for e in publicador.eventos_publicados] == [CompetenciaRenombrada]


def test_descripcion_ausente_se_conserva_null_y_vacia_se_borran(
    caso_uso: RenombrarCompetenciaCasoUso, administrador: UsuarioActual
) -> None:
    nombre = "Razonamiento cuantitativo"
    con_texto = caso_uso.ejecutar(
        administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, nombre, "Algo")
    )
    assert con_texto.descripcion == "Algo"
    conservada = caso_uso.ejecutar(
        administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, nombre)
    )
    assert conservada.descripcion == "Algo"
    borrada = caso_uso.ejecutar(
        administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, nombre, None)
    )
    assert borrada.descripcion is None
    caso_uso.ejecutar(
        administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, nombre, "Otra")
    )
    vacia = caso_uso.ejecutar(
        administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, nombre, "")
    )
    assert vacia.descripcion is None


def test_inv24_renombrar_con_el_nombre_de_otra_competencia_es_duplicado(
    caso_uso: RenombrarCompetenciaCasoUso,
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    guardados = repositorio_con_semilla.cantidad_de_guardados
    with pytest.raises(NombreDuplicadoExcepcion) as error:
        caso_uso.ejecutar(
            administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, " DISEÑO de software")
        )
    assert error.value.codigo == "NOMBRE_DUPLICADO"
    assert repositorio_con_semilla.cantidad_de_guardados == guardados
    assert publicador.eventos_publicados == []


def test_competencia_inexistente_lanza_competencia_no_encontrada(
    caso_uso: RenombrarCompetenciaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(CompetenciaNoEncontradaExcepcion) as error:
        caso_uso.ejecutar(
            administrador,
            RenombrarCompetenciaComando("99999999-9999-4999-8999-999999999999", "Otro"),
        )
    assert error.value.codigo == "COMPETENCIA_NO_ENCONTRADA"


def test_id_mal_formado_es_solicitud_invalida(
    caso_uso: RenombrarCompetenciaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        caso_uso.ejecutar(administrador, RenombrarCompetenciaComando("no-es-uuid", "Otro"))


def test_nombre_vacio_es_solicitud_invalida(
    caso_uso: RenombrarCompetenciaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        caso_uso.ejecutar(
            administrador, RenombrarCompetenciaComando(ids.COMPETENCIA_RAZONAMIENTO, "  ")
        )


def test_puede_cambiar_la_descripcion(
    caso_uso: RenombrarCompetenciaCasoUso, administrador: UsuarioActual
) -> None:
    respuesta = caso_uso.ejecutar(
        administrador,
        RenombrarCompetenciaComando(
            ids.COMPETENCIA_RAZONAMIENTO, "Razonamiento cuantitativo", "Nueva descripción"
        ),
    )
    assert respuesta.descripcion == "Nueva descripción"
