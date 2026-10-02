"""Pruebas de roles: escrituras solo con ``ADMINISTRADOR`` (D-11, D-08); lecturas con cualquier rol."""

from collections.abc import Callable
from typing import Any

import pytest

from catalogo.aplicacion.casos_uso.agregar_subtema_caso_uso import AgregarSubtemaCasoUso
from catalogo.aplicacion.casos_uso.agregar_tema_caso_uso import AgregarTemaCasoUso
from catalogo.aplicacion.casos_uso.crear_competencia_caso_uso import CrearCompetenciaCasoUso
from catalogo.aplicacion.casos_uso.listar_competencias_caso_uso import ListarCompetenciasCasoUso
from catalogo.aplicacion.casos_uso.obtener_competencia_caso_uso import ObtenerCompetenciaCasoUso
from catalogo.aplicacion.casos_uso.renombrar_competencia_caso_uso import (
    RenombrarCompetenciaCasoUso,
)
from catalogo.aplicacion.casos_uso.renombrar_subtema_caso_uso import RenombrarSubtemaCasoUso
from catalogo.aplicacion.casos_uso.renombrar_tema_caso_uso import RenombrarTemaCasoUso
from catalogo.aplicacion.dtos.comandos import (
    AgregarSubtemaComando,
    AgregarTemaComando,
    CrearCompetenciaComando,
    ObtenerCompetenciaConsulta,
    RenombrarCompetenciaComando,
    RenombrarSubtemaComando,
    RenombrarTemaComando,
)
from catalogo.aplicacion.excepciones import AccesoDenegadoExcepcion
from catalogo.aplicacion.seguridad.usuario_actual import Rol, UsuarioActual
from tests import ids_catalogo_semilla as ids
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria

RAZ = ids.COMPETENCIA_RAZONAMIENTO
ID_USUARIO = "11111111-1111-4111-8111-000000000009"

# Cada escritura: (nombre, fábrica del caso de uso, comando válido).
ESCRITURAS: list[tuple[str, Callable[..., Any], Any]] = [
    ("crear_competencia", CrearCompetenciaCasoUso, CrearCompetenciaComando("Nueva")),
    (
        "renombrar_competencia",
        RenombrarCompetenciaCasoUso,
        RenombrarCompetenciaComando(RAZ, "Nombre nuevo"),
    ),
    ("agregar_tema", AgregarTemaCasoUso, AgregarTemaComando(RAZ, "Tema nuevo")),
    (
        "renombrar_tema",
        RenombrarTemaCasoUso,
        RenombrarTemaComando(RAZ, ids.TEMA_ESTADISTICA, "Nombre nuevo"),
    ),
    (
        "agregar_subtema",
        AgregarSubtemaCasoUso,
        AgregarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, "Subtema nuevo"),
    ),
    (
        "renombrar_subtema",
        RenombrarSubtemaCasoUso,
        RenombrarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD, "Nombre nuevo"),
    ),
]
IDS_ESCRITURAS = [escritura[0] for escritura in ESCRITURAS]

ROLES_SIN_PERMISO = [
    frozenset({Rol.ESTUDIANTE}),
    frozenset({Rol.AUTOR}),
    frozenset({Rol.REVISOR}),
    frozenset({Rol.DOCENTE}),
    frozenset({Rol.AUTOR, Rol.DOCENTE}),
    frozenset(),
]


@pytest.mark.parametrize("roles", ROLES_SIN_PERMISO, ids=lambda r: ",".join(sorted(x.name for x in r)) or "sin_roles")
@pytest.mark.parametrize("fabrica,comando", [(e[1], e[2]) for e in ESCRITURAS], ids=IDS_ESCRITURAS)
def test_escritura_sin_rol_administrador_lanza_acceso_denegado(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
    fabrica: Callable[..., Any],
    comando: Any,
    roles: frozenset[Rol],
) -> None:
    guardados = repositorio_con_semilla.cantidad_de_guardados
    caso_uso = fabrica(repositorio_con_semilla, publicador)
    with pytest.raises(AccesoDenegadoExcepcion) as error:
        caso_uso.ejecutar(UsuarioActual(ID_USUARIO, roles), comando)
    assert error.value.codigo == "ACCESO_DENEGADO"
    assert repositorio_con_semilla.cantidad_de_guardados == guardados
    assert publicador.eventos_publicados == []


@pytest.mark.parametrize("fabrica,comando", [(e[1], e[2]) for e in ESCRITURAS], ids=IDS_ESCRITURAS)
def test_escritura_con_varios_roles_que_incluyen_administrador_se_permite(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
    publicador: PublicadorEventosEnMemoria,
    fabrica: Callable[..., Any],
    comando: Any,
) -> None:
    guardados = repositorio_con_semilla.cantidad_de_guardados
    usuario = UsuarioActual(ID_USUARIO, frozenset({Rol.AUTOR, Rol.ADMINISTRADOR}))
    fabrica(repositorio_con_semilla, publicador).ejecutar(usuario, comando)
    assert repositorio_con_semilla.cantidad_de_guardados == guardados + 1


@pytest.mark.parametrize("rol", list(Rol))
def test_las_lecturas_las_puede_hacer_cualquier_rol(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, rol: Rol
) -> None:
    usuario = UsuarioActual(ID_USUARIO, frozenset({rol}))
    assert len(ListarCompetenciasCasoUso(repositorio_con_semilla).ejecutar(usuario)) == 2
    respuesta = ObtenerCompetenciaCasoUso(repositorio_con_semilla).ejecutar(
        usuario, ObtenerCompetenciaConsulta(RAZ)
    )
    assert respuesta.competencia_id == RAZ


def test_las_lecturas_sin_ningun_rol_son_acceso_denegado(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria,
) -> None:
    sin_roles = UsuarioActual(ID_USUARIO, frozenset())
    with pytest.raises(AccesoDenegadoExcepcion):
        ListarCompetenciasCasoUso(repositorio_con_semilla).ejecutar(sin_roles)
    with pytest.raises(AccesoDenegadoExcepcion):
        ObtenerCompetenciaCasoUso(repositorio_con_semilla).ejecutar(
            sin_roles, ObtenerCompetenciaConsulta(RAZ)
        )
