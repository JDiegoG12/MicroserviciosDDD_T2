"""Pruebas de los casos de uso de temas y subtemas (INV-22, INV-23, INV-24)."""

import pytest

from catalogo.aplicacion.casos_uso.agregar_subtema_caso_uso import AgregarSubtemaCasoUso
from catalogo.aplicacion.casos_uso.agregar_tema_caso_uso import AgregarTemaCasoUso
from catalogo.aplicacion.casos_uso.renombrar_subtema_caso_uso import RenombrarSubtemaCasoUso
from catalogo.aplicacion.casos_uso.renombrar_tema_caso_uso import RenombrarTemaCasoUso
from catalogo.aplicacion.dtos.comandos import (
    AgregarSubtemaComando,
    AgregarTemaComando,
    RenombrarSubtemaComando,
    RenombrarTemaComando,
)
from catalogo.aplicacion.excepciones import CompetenciaNoEncontradaExcepcion
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.eventos import SubtemaCreado, TemaCreado
from catalogo.dominio.excepciones import (
    NombreDuplicadoExcepcion,
    SolicitudInvalidaExcepcion,
    SubtemaNoEncontradoExcepcion,
    TemaNoEncontradoExcepcion,
)
from tests import ids_catalogo_semilla as ids
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria

INEXISTENTE = "99999999-9999-4999-8999-999999999999"
RAZ = ids.COMPETENCIA_RAZONAMIENTO


@pytest.fixture
def agregar_tema(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, publicador: PublicadorEventosEnMemoria
) -> AgregarTemaCasoUso:
    return AgregarTemaCasoUso(repositorio_con_semilla, publicador)


@pytest.fixture
def renombrar_tema(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, publicador: PublicadorEventosEnMemoria
) -> RenombrarTemaCasoUso:
    return RenombrarTemaCasoUso(repositorio_con_semilla, publicador)


@pytest.fixture
def agregar_subtema(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, publicador: PublicadorEventosEnMemoria
) -> AgregarSubtemaCasoUso:
    return AgregarSubtemaCasoUso(repositorio_con_semilla, publicador)


@pytest.fixture
def renombrar_subtema(
    repositorio_con_semilla: CompetenciaRepositorioEnMemoria, publicador: PublicadorEventosEnMemoria
) -> RenombrarSubtemaCasoUso:
    return RenombrarSubtemaCasoUso(repositorio_con_semilla, publicador)


# --------------------------------------------------------------------- AgregarTema


def test_agregar_tema_lo_guarda_y_devuelve_la_competencia(
    agregar_tema: AgregarTemaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    respuesta = agregar_tema.ejecutar(administrador, AgregarTemaComando(RAZ, "Geometría"))
    assert [t.nombre for t in respuesta.temas] == ["Álgebra", "Estadística", "Geometría"]
    assert [type(e) for e in publicador.eventos_publicados] == [TemaCreado]


def test_agregar_tema_a_competencia_inexistente(
    agregar_tema: AgregarTemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(CompetenciaNoEncontradaExcepcion):
        agregar_tema.ejecutar(administrador, AgregarTemaComando(INEXISTENTE, "Geometría"))


@pytest.mark.parametrize("repetido", ["Estadística", "estadistica", "  ESTADÍSTICA  "])
def test_inv24_agregar_tema_duplicado_en_su_competencia(
    agregar_tema: AgregarTemaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
    repetido: str,
) -> None:
    with pytest.raises(NombreDuplicadoExcepcion):
        agregar_tema.ejecutar(administrador, AgregarTemaComando(RAZ, repetido))
    assert publicador.eventos_publicados == []


def test_el_mismo_nombre_de_tema_en_otra_competencia_es_valido(
    agregar_tema: AgregarTemaCasoUso, administrador: UsuarioActual
) -> None:
    agregar_tema.ejecutar(administrador, AgregarTemaComando(ids.COMPETENCIA_DISENO, "Estadística"))


def test_agregar_tema_con_nombre_vacio_es_solicitud_invalida(
    agregar_tema: AgregarTemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        agregar_tema.ejecutar(administrador, AgregarTemaComando(RAZ, ""))


# ----------------------------------------------------------------- RenombrarTema


def test_inv22_renombrar_tema_conserva_su_identificador(
    renombrar_tema: RenombrarTemaCasoUso, administrador: UsuarioActual
) -> None:
    respuesta = renombrar_tema.ejecutar(
        administrador, RenombrarTemaComando(RAZ, ids.TEMA_ESTADISTICA, "Estadística descriptiva")
    )
    tema = next(t for t in respuesta.temas if t.tema_id == ids.TEMA_ESTADISTICA)
    assert tema.nombre == "Estadística descriptiva"


def test_inv23_renombrar_tema_inexistente(
    renombrar_tema: RenombrarTemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion) as error:
        renombrar_tema.ejecutar(administrador, RenombrarTemaComando(RAZ, INEXISTENTE, "Otro"))
    assert error.value.codigo == "TEMA_NO_ENCONTRADO"


def test_inv23_renombrar_un_tema_de_otra_competencia_es_tema_no_encontrado(
    renombrar_tema: RenombrarTemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion):
        renombrar_tema.ejecutar(administrador, RenombrarTemaComando(RAZ, ids.TEMA_PATRONES, "Otro"))


def test_inv24_renombrar_tema_con_el_nombre_de_otro_tema(
    renombrar_tema: RenombrarTemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(NombreDuplicadoExcepcion):
        renombrar_tema.ejecutar(
            administrador, RenombrarTemaComando(RAZ, ids.TEMA_ESTADISTICA, " algebra ")
        )


def test_renombrar_tema_con_una_variante_no_es_duplicado_y_actualiza_el_texto(
    renombrar_tema: RenombrarTemaCasoUso, administrador: UsuarioActual
) -> None:
    respuesta = renombrar_tema.ejecutar(
        administrador, RenombrarTemaComando(RAZ, ids.TEMA_ESTADISTICA, "ESTADISTICA")
    )
    assert next(t for t in respuesta.temas if t.tema_id == ids.TEMA_ESTADISTICA).nombre == "ESTADISTICA"


def test_renombrar_tema_con_id_mal_formado(
    renombrar_tema: RenombrarTemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        renombrar_tema.ejecutar(administrador, RenombrarTemaComando(RAZ, "xyz", "Otro"))


def test_renombrar_tema_en_competencia_inexistente(
    renombrar_tema: RenombrarTemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(CompetenciaNoEncontradaExcepcion):
        renombrar_tema.ejecutar(
            administrador, RenombrarTemaComando(INEXISTENTE, ids.TEMA_ESTADISTICA, "Otro")
        )


# ------------------------------------------------------------------ AgregarSubtema


def test_agregar_subtema_lo_guarda_y_devuelve_la_competencia(
    agregar_subtema: AgregarSubtemaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    respuesta = agregar_subtema.ejecutar(
        administrador, AgregarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, "Regresión")
    )
    tema = next(t for t in respuesta.temas if t.tema_id == ids.TEMA_ESTADISTICA)
    assert [s.nombre for s in tema.subtemas] == [
        "Medidas de tendencia central",
        "Probabilidad",
        "Regresión",
    ]
    assert [type(e) for e in publicador.eventos_publicados] == [SubtemaCreado]


def test_inv23_agregar_subtema_a_tema_inexistente(
    agregar_subtema: AgregarSubtemaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion) as error:
        agregar_subtema.ejecutar(administrador, AgregarSubtemaComando(RAZ, INEXISTENTE, "Algo"))
    assert error.value.codigo == "TEMA_NO_ENCONTRADO"
    assert publicador.eventos_publicados == []


def test_inv23_agregar_subtema_a_un_tema_de_otra_competencia(
    agregar_subtema: AgregarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion):
        agregar_subtema.ejecutar(administrador, AgregarSubtemaComando(RAZ, ids.TEMA_PATRONES, "Algo"))


def test_agregar_subtema_a_competencia_inexistente(
    agregar_subtema: AgregarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(CompetenciaNoEncontradaExcepcion):
        agregar_subtema.ejecutar(
            administrador, AgregarSubtemaComando(INEXISTENTE, ids.TEMA_ESTADISTICA, "Algo")
        )


@pytest.mark.parametrize("repetido", ["Probabilidad", "PROBABILIDAD", "  probabilidad "])
def test_inv24_agregar_subtema_duplicado_en_su_tema(
    agregar_subtema: AgregarSubtemaCasoUso, administrador: UsuarioActual, repetido: str
) -> None:
    with pytest.raises(NombreDuplicadoExcepcion):
        agregar_subtema.ejecutar(
            administrador, AgregarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, repetido)
        )


def test_el_mismo_nombre_de_subtema_en_otro_tema_es_valido(
    agregar_subtema: AgregarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    agregar_subtema.ejecutar(administrador, AgregarSubtemaComando(RAZ, ids.TEMA_ALGEBRA, "Probabilidad"))


# ---------------------------------------------------------------- RenombrarSubtema


def test_inv22_renombrar_subtema_conserva_su_identificador(
    renombrar_subtema: RenombrarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    respuesta = renombrar_subtema.ejecutar(
        administrador,
        RenombrarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD, "Probabilidad básica"),
    )
    tema = next(t for t in respuesta.temas if t.tema_id == ids.TEMA_ESTADISTICA)
    subtema = next(s for s in tema.subtemas if s.subtema_id == ids.SUBTEMA_PROBABILIDAD)
    assert subtema.nombre == "Probabilidad básica"


def test_inv23_renombrar_subtema_inexistente(
    renombrar_subtema: RenombrarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(SubtemaNoEncontradoExcepcion) as error:
        renombrar_subtema.ejecutar(
            administrador, RenombrarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, INEXISTENTE, "Otro")
        )
    assert error.value.codigo == "SUBTEMA_NO_ENCONTRADO"


def test_inv23_renombrar_subtema_de_otro_tema(
    renombrar_subtema: RenombrarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(SubtemaNoEncontradoExcepcion):
        renombrar_subtema.ejecutar(
            administrador,
            RenombrarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, ids.SUBTEMA_ECUACIONES, "Otro"),
        )


def test_renombrar_subtema_con_tema_inexistente(
    renombrar_subtema: RenombrarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion):
        renombrar_subtema.ejecutar(
            administrador, RenombrarSubtemaComando(RAZ, INEXISTENTE, ids.SUBTEMA_PROBABILIDAD, "Otro")
        )


def test_inv24_renombrar_subtema_con_el_nombre_de_otro_subtema(
    renombrar_subtema: RenombrarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    with pytest.raises(NombreDuplicadoExcepcion):
        renombrar_subtema.ejecutar(
            administrador,
            RenombrarSubtemaComando(
                RAZ, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD, "medidas de TENDENCIA central"
            ),
        )


def test_renombrar_subtema_con_una_variante_no_es_duplicado(
    renombrar_subtema: RenombrarSubtemaCasoUso, administrador: UsuarioActual
) -> None:
    respuesta = renombrar_subtema.ejecutar(
        administrador,
        RenombrarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD, " PROBABILIDAD "),
    )
    tema = next(t for t in respuesta.temas if t.tema_id == ids.TEMA_ESTADISTICA)
    assert next(s for s in tema.subtemas if s.subtema_id == ids.SUBTEMA_PROBABILIDAD).nombre == "PROBABILIDAD"


def test_renombrar_subtema_con_su_mismo_nombre_no_cambia_nada(
    renombrar_subtema: RenombrarSubtemaCasoUso,
    publicador: PublicadorEventosEnMemoria,
    administrador: UsuarioActual,
) -> None:
    renombrar_subtema.ejecutar(
        administrador,
        RenombrarSubtemaComando(RAZ, ids.TEMA_ESTADISTICA, ids.SUBTEMA_PROBABILIDAD, " Probabilidad "),
    )
    assert publicador.eventos_publicados == []
