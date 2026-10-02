"""Pruebas del agregado ``Competencia``: INV-22, INV-23 e INV-24 y sus eventos."""

import pytest

from catalogo.dominio.competencia import LONGITUD_MAXIMA_DESCRIPCION, Competencia
from catalogo.dominio.eventos import (
    CompetenciaCreada,
    CompetenciaRenombrada,
    SubtemaCreado,
    TemaCreado,
)
from catalogo.dominio.excepciones import (
    NombreDuplicadoExcepcion,
    SolicitudInvalidaExcepcion,
    SubtemaNoEncontradoExcepcion,
    TemaNoEncontradoExcepcion,
)
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo


def n(texto: str) -> NombreCatalogo:
    return NombreCatalogo(texto)


@pytest.fixture
def competencia() -> Competencia:
    return Competencia.crear(n("Razonamiento cuantitativo"))


@pytest.fixture
def competencia_con_tema(competencia: Competencia) -> tuple[Competencia, TemaId]:
    tema = competencia.agregar_tema(n("Estadística"))
    return competencia, tema.id


# --------------------------------------------------------------- creación y eventos


def test_crear_registra_el_evento_competencia_creada(competencia: Competencia) -> None:
    eventos = competencia.extraer_eventos()
    assert eventos == [CompetenciaCreada(competencia.id, n("Razonamiento cuantitativo"))]


def test_crear_sin_descripcion_deja_none(competencia: Competencia) -> None:
    assert competencia.descripcion is None


def test_crear_con_descripcion_la_conserva() -> None:
    assert Competencia.crear(n("A"), "  Texto  ").descripcion == "Texto"


def test_crear_con_descripcion_en_blanco_equivale_a_ninguna() -> None:
    assert Competencia.crear(n("A"), "   ").descripcion is None


def test_crear_acepta_descripcion_de_exactamente_500_caracteres() -> None:
    assert len(Competencia.crear(n("A"), "x" * LONGITUD_MAXIMA_DESCRIPCION).descripcion) == 500


def test_crear_rechaza_descripcion_de_501_caracteres() -> None:
    with pytest.raises(SolicitudInvalidaExcepcion) as error:
        Competencia.crear(n("A"), "x" * (LONGITUD_MAXIMA_DESCRIPCION + 1))
    assert error.value.codigo == "SOLICITUD_INVALIDA"


def test_crear_con_identificador_fijo_lo_usa() -> None:
    identificador = CompetenciaId.generar()
    assert Competencia.crear(n("A"), competencia_id=identificador).id == identificador


def test_extraer_eventos_entrega_y_vacia_la_lista(competencia: Competencia) -> None:
    assert len(competencia.extraer_eventos()) == 1
    assert competencia.extraer_eventos() == []


def test_agregar_tema_y_subtema_registran_sus_eventos(competencia: Competencia) -> None:
    competencia.extraer_eventos()
    tema = competencia.agregar_tema(n("Estadística"))
    subtema = competencia.agregar_subtema(tema.id, n("Probabilidad"))
    assert competencia.extraer_eventos() == [
        TemaCreado(competencia.id, tema.id, n("Estadística")),
        SubtemaCreado(competencia.id, tema.id, subtema.id, n("Probabilidad")),
    ]


# ------------------------------------------------------------------------- INV-22


def test_inv22_renombrar_competencia_no_cambia_su_identificador(competencia: Competencia) -> None:
    identificador = competencia.id
    competencia.renombrar(n("Razonamiento lógico"))
    assert competencia.id == identificador
    assert competencia.nombre == n("Razonamiento lógico")


def test_inv22_renombrar_tema_y_subtema_no_cambia_sus_identificadores(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    subtema = competencia.agregar_subtema(tema_id, n("Probabilidad"))
    competencia.renombrar_tema(tema_id, n("Estadística descriptiva"))
    competencia.renombrar_subtema(tema_id, subtema.id, n("Probabilidad básica"))
    tema = competencia.buscar_tema(tema_id)
    assert tema is not None and tema.nombre == n("Estadística descriptiva")
    assert tema.buscar_subtema(subtema.id).nombre == n("Probabilidad básica")  # type: ignore[union-attr]


def test_inv22_el_identificador_es_independiente_del_nombre() -> None:
    primera = Competencia.crear(n("Igual"))
    segunda = Competencia.crear(n("Igual"))
    assert primera.id != segunda.id


def test_renombrar_registra_competencia_renombrada(competencia: Competencia) -> None:
    competencia.extraer_eventos()
    competencia.renombrar(n("Otro nombre"))
    assert competencia.extraer_eventos() == [
        CompetenciaRenombrada(competencia.id, n("Razonamiento cuantitativo"), n("Otro nombre"))
    ]


def test_renombrar_con_el_mismo_nombre_no_cambia_nada_ni_emite_evento(
    competencia: Competencia,
) -> None:
    competencia.extraer_eventos()
    competencia.renombrar(n("Razonamiento cuantitativo"))
    assert competencia.nombre == n("Razonamiento cuantitativo")
    assert competencia.extraer_eventos() == []


def test_renombrar_con_una_variante_actualiza_el_texto_guardado_y_emite_el_evento(
    competencia: Competencia,
) -> None:
    competencia.extraer_eventos()
    competencia.renombrar(n("  RAZONAMIENTO   cuantitativo "))
    assert competencia.nombre == n("RAZONAMIENTO   cuantitativo")
    assert competencia.extraer_eventos() == [
        CompetenciaRenombrada(
            competencia.id, n("Razonamiento cuantitativo"), n("RAZONAMIENTO   cuantitativo")
        )
    ]


def test_renombrar_sin_descripcion_conserva_la_actual() -> None:
    competencia = Competencia.crear(n("A"), "Descripción")
    competencia.renombrar(n("B"))
    assert competencia.descripcion == "Descripción"


@pytest.mark.parametrize("borrar", [None, "", "   "])
def test_renombrar_con_descripcion_null_o_vacia_la_borra(borrar: str | None) -> None:
    competencia = Competencia.crear(n("A"), "Vieja")
    competencia.renombrar(n("A"), borrar)
    assert competencia.descripcion is None


def test_renombrar_con_descripcion_la_reemplaza() -> None:
    competencia = Competencia.crear(n("A"), "Vieja")
    competencia.renombrar(n("A"), "Nueva")
    assert competencia.descripcion == "Nueva"


def test_renombrar_con_descripcion_demasiado_larga_se_rechaza_sin_cambiar_nada() -> None:
    competencia = Competencia.crear(n("A"), "Vieja")
    with pytest.raises(SolicitudInvalidaExcepcion):
        competencia.renombrar(n("B"), "x" * 501)
    assert competencia.nombre == n("A") and competencia.descripcion == "Vieja"


# ------------------------------------------------------------------------- INV-23


def test_inv23_agregar_subtema_a_un_tema_inexistente_lanza_tema_no_encontrado(
    competencia: Competencia,
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion) as error:
        competencia.agregar_subtema(TemaId.generar(), n("Probabilidad"))
    assert error.value.codigo == "TEMA_NO_ENCONTRADO"


def test_inv23_un_tema_de_otra_competencia_no_sirve_para_agregar_subtemas() -> None:
    una = Competencia.crear(n("Una"))
    otra = Competencia.crear(n("Otra"))
    tema_ajeno = otra.agregar_tema(n("Tema ajeno"))
    with pytest.raises(TemaNoEncontradoExcepcion):
        una.agregar_subtema(tema_ajeno.id, n("Subtema"))
    assert una.temas == ()


def test_inv23_renombrar_un_tema_inexistente_lanza_tema_no_encontrado(
    competencia: Competencia,
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion):
        competencia.renombrar_tema(TemaId.generar(), n("Nuevo"))


def test_inv23_renombrar_subtema_con_tema_inexistente_lanza_tema_no_encontrado(
    competencia: Competencia,
) -> None:
    with pytest.raises(TemaNoEncontradoExcepcion):
        competencia.renombrar_subtema(TemaId.generar(), SubtemaId.generar(), n("Nuevo"))


def test_inv23_renombrar_un_subtema_inexistente_lanza_subtema_no_encontrado(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    with pytest.raises(SubtemaNoEncontradoExcepcion) as error:
        competencia.renombrar_subtema(tema_id, SubtemaId.generar(), n("Nuevo"))
    assert error.value.codigo == "SUBTEMA_NO_ENCONTRADO"


def test_inv23_un_subtema_de_otro_tema_no_se_puede_renombrar_desde_este(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    otro_tema = competencia.agregar_tema(n("Álgebra"))
    subtema = competencia.agregar_subtema(otro_tema.id, n("Ecuaciones lineales"))
    with pytest.raises(SubtemaNoEncontradoExcepcion):
        competencia.renombrar_subtema(tema_id, subtema.id, n("Otro"))


def test_inv23_los_elementos_agregados_quedan_dentro_de_la_jerarquia(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    subtema = competencia.agregar_subtema(tema_id, n("Probabilidad"))
    tema = competencia.buscar_tema(tema_id)
    assert tema is not None
    assert tema.subtemas == (subtema,)
    assert competencia.temas == (tema,)


def test_agregar_con_identificadores_fijos_los_conserva(competencia: Competencia) -> None:
    tema_id, subtema_id = TemaId.generar(), SubtemaId.generar()
    competencia.agregar_tema(n("Estadística"), tema_id)
    competencia.agregar_subtema(tema_id, n("Probabilidad"), subtema_id)
    tema = competencia.buscar_tema(tema_id)
    assert tema is not None and tema.buscar_subtema(subtema_id) is not None


def test_no_se_puede_repetir_un_identificador_de_tema_ni_de_subtema(
    competencia: Competencia,
) -> None:
    tema_id, subtema_id = TemaId.generar(), SubtemaId.generar()
    competencia.agregar_tema(n("Uno"), tema_id)
    competencia.agregar_subtema(tema_id, n("Sub uno"), subtema_id)
    with pytest.raises(SolicitudInvalidaExcepcion):
        competencia.agregar_tema(n("Dos"), tema_id)
    otro_tema = competencia.agregar_tema(n("Tres"))
    with pytest.raises(SolicitudInvalidaExcepcion):
        competencia.agregar_subtema(otro_tema.id, n("Sub dos"), subtema_id)


# ------------------------------------------------------------------------- INV-24


@pytest.mark.parametrize(
    "repetido", ["Estadística", "estadística", "ESTADÍSTICA", "  Estadística  ", "estadistica"]
)
def test_inv24_tema_duplicado_difiera_en_mayusculas_tildes_o_espacios(
    competencia_con_tema: tuple[Competencia, TemaId], repetido: str
) -> None:
    competencia, _ = competencia_con_tema
    with pytest.raises(NombreDuplicadoExcepcion) as error:
        competencia.agregar_tema(n(repetido))
    assert error.value.codigo == "NOMBRE_DUPLICADO"
    assert len(competencia.temas) == 1


def test_inv24_tema_duplicado_con_espacios_internos_distintos() -> None:
    competencia = Competencia.crear(n("A"))
    competencia.agregar_tema(n("Patrones de diseño"))
    with pytest.raises(NombreDuplicadoExcepcion):
        competencia.agregar_tema(n("patrones   de  DISEÑO"))


@pytest.mark.parametrize("repetido", ["Probabilidad", "probabilidad", "  PROBABILIDAD ", "probabilídad"])
def test_inv24_subtema_duplicado_dentro_de_su_tema(
    competencia_con_tema: tuple[Competencia, TemaId], repetido: str
) -> None:
    competencia, tema_id = competencia_con_tema
    competencia.agregar_subtema(tema_id, n("Probabilidad"))
    with pytest.raises(NombreDuplicadoExcepcion) as error:
        competencia.agregar_subtema(tema_id, n(repetido))
    assert error.value.codigo == "NOMBRE_DUPLICADO"


def test_inv24_el_mismo_nombre_de_subtema_en_otro_tema_es_valido(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    otro_tema = competencia.agregar_tema(n("Álgebra"))
    competencia.agregar_subtema(tema_id, n("Introducción"))
    competencia.agregar_subtema(otro_tema.id, n("introducción"))


def test_inv24_el_mismo_nombre_de_tema_en_otra_competencia_es_valido() -> None:
    Competencia.crear(n("Una")).agregar_tema(n("Estadística"))
    Competencia.crear(n("Otra")).agregar_tema(n("Estadística"))


def test_inv24_renombrar_tema_con_el_nombre_de_otro_tema_lanza_nombre_duplicado(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    competencia.agregar_tema(n("Álgebra"))
    with pytest.raises(NombreDuplicadoExcepcion):
        competencia.renombrar_tema(tema_id, n("  ALGEBRA "))
    tema = competencia.buscar_tema(tema_id)
    assert tema is not None and tema.nombre == n("Estadística")


def test_inv24_renombrar_subtema_con_el_nombre_de_otro_subtema_lanza_nombre_duplicado(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    competencia.agregar_subtema(tema_id, n("Probabilidad"))
    otro = competencia.agregar_subtema(tema_id, n("Medidas de tendencia central"))
    with pytest.raises(NombreDuplicadoExcepcion):
        competencia.renombrar_subtema(tema_id, otro.id, n("probabilidad"))


@pytest.mark.parametrize("variante", ["estadistica", "  ESTADÍSTICA  "])
def test_renombrar_tema_con_una_variante_no_es_duplicado_y_actualiza_el_texto(
    competencia_con_tema: tuple[Competencia, TemaId], variante: str
) -> None:
    competencia, tema_id = competencia_con_tema
    competencia.renombrar_tema(tema_id, n(variante))
    tema = competencia.buscar_tema(tema_id)
    assert tema is not None and tema.nombre == n(variante)


def test_renombrar_tema_con_su_mismo_texto_no_cambia_nada(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    competencia.renombrar_tema(tema_id, n(" Estadística "))
    tema = competencia.buscar_tema(tema_id)
    assert tema is not None and tema.nombre == n("Estadística")


@pytest.mark.parametrize("variante", ["probabilidad", "  PROBABILIDAD  "])
def test_renombrar_subtema_con_una_variante_no_es_duplicado_y_actualiza_el_texto(
    competencia_con_tema: tuple[Competencia, TemaId], variante: str
) -> None:
    competencia, tema_id = competencia_con_tema
    subtema = competencia.agregar_subtema(tema_id, n("Probabilidad"))
    competencia.renombrar_subtema(tema_id, subtema.id, n(variante))
    assert subtema.nombre == n(variante)


def test_renombrar_subtema_con_su_mismo_texto_no_cambia_nada(
    competencia_con_tema: tuple[Competencia, TemaId],
) -> None:
    competencia, tema_id = competencia_con_tema
    subtema = competencia.agregar_subtema(tema_id, n("Probabilidad"))
    competencia.renombrar_subtema(tema_id, subtema.id, n("Probabilidad "))
    assert subtema.nombre == n("Probabilidad")


def test_la_enie_es_una_letra_propia_y_no_cuenta_como_duplicado_de_la_n() -> None:
    competencia = Competencia.crear(n("A"))
    competencia.agregar_tema(n("Año"))
    competencia.agregar_tema(n("Ano"))
    with pytest.raises(NombreDuplicadoExcepcion):
        competencia.agregar_tema(n("  AÑO "))


def test_crear_con_descripcion_que_no_es_texto_es_solicitud_invalida() -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        Competencia.crear(n("A"), 123)  # type: ignore[arg-type]
