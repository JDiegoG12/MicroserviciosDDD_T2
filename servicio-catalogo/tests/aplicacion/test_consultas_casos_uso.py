"""Pruebas de ``ListarCompetenciasCasoUso`` y ``ObtenerCompetenciaCasoUso``."""

import pytest

from catalogo.aplicacion.casos_uso.crear_competencia_caso_uso import CrearCompetenciaCasoUso
from catalogo.aplicacion.casos_uso.listar_competencias_caso_uso import ListarCompetenciasCasoUso
from catalogo.aplicacion.casos_uso.obtener_competencia_caso_uso import ObtenerCompetenciaCasoUso
from catalogo.aplicacion.dtos.comandos import CrearCompetenciaComando, ObtenerCompetenciaConsulta
from catalogo.aplicacion.excepciones import CompetenciaNoEncontradaExcepcion
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion
from tests import ids_catalogo_semilla as ids
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria


def test_listar_devuelve_vacio_si_no_hay_competencias(
    repositorio: CompetenciaRepositorioEnMemoria, estudiante: UsuarioActual
) -> None:
    assert ListarCompetenciasCasoUso(repositorio).ejecutar(estudiante) == []


def test_listar_devuelve_la_jerarquia_anidada_ordenada_por_nombre_normalizado(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, estudiante: UsuarioActual
) -> None:
    competencias = ListarCompetenciasCasoUso(repositorio_con_semilla).ejecutar(estudiante)
    # "diseño de software" < "razonamiento cuantitativo"
    assert [c.nombre for c in competencias] == ["Diseño de software", "Razonamiento cuantitativo"]
    diseno, razonamiento = competencias
    assert [t.nombre for t in diseno.temas] == ["Arquitectura de software", "Patrones de diseño"]
    # "algebra" < "estadistica" aunque la tilde de Á sea posterior a la Z en Unicode.
    assert [t.nombre for t in razonamiento.temas] == ["Álgebra", "Estadística"]
    estadistica = razonamiento.temas[1]
    assert estadistica.tema_id == ids.TEMA_ESTADISTICA
    assert [s.nombre for s in estadistica.subtemas] == [
        "Medidas de tendencia central",
        "Probabilidad",
    ]


def test_listar_ordena_por_nombre_normalizado_y_no_por_mayusculas(
    repositorio: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    crear = CrearCompetenciaCasoUso(repositorio, publicador)
    for nombre in ["zeta", "Álgebra", "beta", "Árbol", "ALFA"]:
        crear.ejecutar(administrador, CrearCompetenciaComando(nombre))
    nombres = [c.nombre for c in ListarCompetenciasCasoUso(repositorio).ejecutar(administrador)]
    assert nombres == ["ALFA", "Álgebra", "Árbol", "beta", "zeta"]


def test_obtener_devuelve_la_forma_de_competencia_respuesta(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, estudiante: UsuarioActual
) -> None:
    respuesta = ObtenerCompetenciaCasoUso(repositorio_con_semilla).ejecutar(
        estudiante, ObtenerCompetenciaConsulta(ids.COMPETENCIA_DISENO)
    )
    assert respuesta.competencia_id == ids.COMPETENCIA_DISENO
    assert respuesta.nombre == "Diseño de software"
    assert respuesta.descripcion is None
    patrones = next(t for t in respuesta.temas if t.tema_id == ids.TEMA_PATRONES)
    assert [(s.subtema_id, s.nombre) for s in patrones.subtemas] == [
        (ids.SUBTEMA_CREACIONALES, "Patrones creacionales")
    ]


def test_obtener_competencia_inexistente(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, estudiante: UsuarioActual
) -> None:
    with pytest.raises(CompetenciaNoEncontradaExcepcion) as error:
        ObtenerCompetenciaCasoUso(repositorio_con_semilla).ejecutar(
            estudiante, ObtenerCompetenciaConsulta("99999999-9999-4999-8999-999999999999")
        )
    assert error.value.codigo == "COMPETENCIA_NO_ENCONTRADA"


def test_obtener_con_id_mal_formado_es_solicitud_invalida(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, estudiante: UsuarioActual
) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        ObtenerCompetenciaCasoUso(repositorio_con_semilla).ejecutar(
            estudiante, ObtenerCompetenciaConsulta("abc")
        )
