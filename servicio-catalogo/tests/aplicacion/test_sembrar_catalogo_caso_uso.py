"""Pruebas de ``SembrarCatalogoCasoUso`` y de los datos semilla (CONTRATOS.md 4.3)."""

import pytest

from catalogo.aplicacion.casos_uso.crear_competencia_caso_uso import CrearCompetenciaCasoUso
from catalogo.aplicacion.casos_uso.sembrar_catalogo_caso_uso import SembrarCatalogoCasoUso
from catalogo.aplicacion.datos_semilla import CATALOGO_SEMILLA
from catalogo.aplicacion.dtos.comandos import CrearCompetenciaComando
from catalogo.aplicacion.dtos.definiciones_semilla import (
    DefinicionCompetencia,
    DefinicionTema,
    SembrarCatalogoComando,
)
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.eventos import CompetenciaCreada, SubtemaCreado, TemaCreado
from catalogo.dominio.excepciones import NombreDuplicadoExcepcion
from tests import ids_catalogo_semilla as ids
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria


@pytest.fixture
def caso_uso(
    repositorio: CompetenciaRepositorioEnMemoria, publicador: PublicadorEventosEnMemoria
) -> SembrarCatalogoCasoUso:
    return SembrarCatalogoCasoUso(repositorio, publicador)


def contenido_del_repositorio(repositorio: CompetenciaRepositorioEnMemoria) -> list:
    """Devuelve el catálogo como (id, nombre, [(id, nombre, [(id, nombre)])]) ordenado por id."""
    return sorted(
        (
            str(c.id),
            c.nombre.valor,
            sorted(
                (
                    str(t.id),
                    t.nombre.valor,
                    sorted((str(s.id), s.nombre.valor) for s in t.subtemas),
                )
                for t in c.temas
            ),
        )
        for c in repositorio.listar_todas()
    )


def tabla_4_3_esperada() -> list:
    return sorted(
        (c_id, c_nombre, sorted((t_id, t_nombre, sorted(subtemas)) for t_id, t_nombre, subtemas in temas))
        for c_id, c_nombre, temas in ids.TABLA_4_3
    )


def test_los_datos_semilla_coinciden_exactamente_con_la_tabla_4_3(
    caso_uso: SembrarCatalogoCasoUso, repositorio: CompetenciaRepositorioEnMemoria
) -> None:
    resultado = caso_uso.ejecutar(SembrarCatalogoComando(CATALOGO_SEMILLA))
    assert resultado.sembrado is True
    assert resultado.competencias_creadas == 2
    assert contenido_del_repositorio(repositorio) == tabla_4_3_esperada()


def test_la_tabla_4_3_tiene_2_competencias_4_temas_y_5_subtemas(
    caso_uso: SembrarCatalogoCasoUso, repositorio: CompetenciaRepositorioEnMemoria
) -> None:
    caso_uso.ejecutar(SembrarCatalogoComando(CATALOGO_SEMILLA))
    competencias = repositorio.listar_todas()
    assert len(competencias) == 2
    assert sum(len(c.temas) for c in competencias) == 4
    assert sum(len(t.subtemas) for c in competencias for t in c.temas) == 5


def test_los_nombres_se_guardan_con_tildes(
    caso_uso: SembrarCatalogoCasoUso, repositorio: CompetenciaRepositorioEnMemoria
) -> None:
    caso_uso.ejecutar(SembrarCatalogoComando(CATALOGO_SEMILLA))
    nombres = {t.nombre.valor for c in repositorio.listar_todas() for t in c.temas}
    assert {"Estadística", "Álgebra"} <= nombres
    assert "Diseño de software" in {c.nombre.valor for c in repositorio.listar_todas()}


def test_una_segunda_ejecucion_no_duplica_nada(
    caso_uso: SembrarCatalogoCasoUso,
    repositorio: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
) -> None:
    caso_uso.ejecutar(SembrarCatalogoComando(CATALOGO_SEMILLA))
    eventos_primera_vez = len(publicador.eventos_publicados)
    segunda = caso_uso.ejecutar(SembrarCatalogoComando(CATALOGO_SEMILLA))
    assert segunda.sembrado is False
    assert segunda.competencias_creadas == 0
    assert contenido_del_repositorio(repositorio) == tabla_4_3_esperada()
    assert len(publicador.eventos_publicados) == eventos_primera_vez


def test_si_el_catalogo_no_esta_vacio_no_siembra(
    repositorio: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
    caso_uso: SembrarCatalogoCasoUso,
    administrador: UsuarioActual,
) -> None:
    CrearCompetenciaCasoUso(repositorio, publicador).ejecutar(
        administrador, CrearCompetenciaComando("Competencia propia")
    )
    publicador.eventos_publicados.clear()
    resultado = caso_uso.ejecutar(SembrarCatalogoComando(CATALOGO_SEMILLA))
    assert resultado.sembrado is False
    assert [c.nombre.valor for c in repositorio.listar_todas()] == ["Competencia propia"]
    assert publicador.eventos_publicados == []


def test_entrega_los_eventos_de_la_siembra_al_publicador(
    caso_uso: SembrarCatalogoCasoUso, publicador: PublicadorEventosEnMemoria
) -> None:
    caso_uso.ejecutar(SembrarCatalogoComando(CATALOGO_SEMILLA))
    tipos = [type(e) for e in publicador.eventos_publicados]
    assert tipos.count(CompetenciaCreada) == 2
    assert tipos.count(TemaCreado) == 4
    assert tipos.count(SubtemaCreado) == 5


def test_sin_definiciones_no_hay_nada_que_sembrar_pero_no_falla(
    caso_uso: SembrarCatalogoCasoUso, repositorio: CompetenciaRepositorioEnMemoria
) -> None:
    resultado = caso_uso.ejecutar(SembrarCatalogoComando(()))
    assert resultado.competencias_creadas == 0
    assert repositorio.esta_vacio()


def test_definiciones_con_nombres_repetidos_se_rechazan(
    caso_uso: SembrarCatalogoCasoUso,
) -> None:
    repetidas = SembrarCatalogoComando(
        (
            DefinicionCompetencia("22222222-2222-4222-8222-000000000101", "Uno"),
            DefinicionCompetencia("22222222-2222-4222-8222-000000000102", " UNO "),
        )
    )
    with pytest.raises(NombreDuplicadoExcepcion):
        caso_uso.ejecutar(repetidas)


def test_tras_la_siembra_los_ids_fijos_son_los_de_la_tabla(
    caso_uso: SembrarCatalogoCasoUso, repositorio: CompetenciaRepositorioEnMemoria
) -> None:
    caso_uso.ejecutar(
        SembrarCatalogoComando(
            (
                DefinicionCompetencia(
                    ids.COMPETENCIA_RAZONAMIENTO,
                    "Razonamiento cuantitativo",
                    (DefinicionTema(ids.TEMA_ESTADISTICA, "Estadística"),),
                ),
            )
        )
    )
    (competencia,) = repositorio.listar_todas()
    assert str(competencia.id) == ids.COMPETENCIA_RAZONAMIENTO
    assert str(competencia.temas[0].id) == ids.TEMA_ESTADISTICA
