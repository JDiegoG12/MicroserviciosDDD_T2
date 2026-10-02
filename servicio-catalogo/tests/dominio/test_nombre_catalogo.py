"""Pruebas del value object ``NombreCatalogo`` (CONTRATOS.md 4 y 11.2 v1.8)."""

import pytest

from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion
from catalogo.dominio.nombre_catalogo import LONGITUD_MAXIMA_NOMBRE, NombreCatalogo


def test_quita_los_espacios_del_inicio_y_del_final() -> None:
    assert NombreCatalogo("   Álgebra  ").valor == "Álgebra"


def test_conserva_tildes_y_mayusculas_en_el_valor() -> None:
    assert NombreCatalogo("Diseño de software").valor == "Diseño de software"


@pytest.mark.parametrize("texto", ["", "   ", "\t\n"])
def test_rechaza_nombre_vacio(texto: str) -> None:
    with pytest.raises(SolicitudInvalidaExcepcion) as error:
        NombreCatalogo(texto)
    assert error.value.codigo == "SOLICITUD_INVALIDA"


def test_acepta_exactamente_120_caracteres() -> None:
    assert len(NombreCatalogo("a" * LONGITUD_MAXIMA_NOMBRE).valor) == 120


def test_rechaza_121_caracteres() -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        NombreCatalogo("a" * (LONGITUD_MAXIMA_NOMBRE + 1))


def test_los_espacios_de_los_extremos_no_cuentan_para_el_limite() -> None:
    assert NombreCatalogo("  " + "a" * 120 + "  ").valor == "a" * 120


def test_cuenta_puntos_de_codigo_y_no_unidades_utf16() -> None:
    # Cada emoji ocupa 2 unidades UTF-16 pero es un solo punto de código (CONTRATOS.md 4).
    assert len(NombreCatalogo("😀" * 120).valor) == 120
    with pytest.raises(SolicitudInvalidaExcepcion):
        NombreCatalogo("😀" * 121)


def test_rechaza_valor_que_no_es_texto() -> None:
    with pytest.raises(SolicitudInvalidaExcepcion):
        NombreCatalogo(123)  # type: ignore[arg-type]


@pytest.mark.parametrize(
    "variante",
    ["Estadística", "  estadistica ", "ESTADÍSTICA", "estadística", "Estadistica"],
)
def test_normalizado_ignora_mayusculas_tildes_y_espacios_de_los_extremos(variante: str) -> None:
    assert NombreCatalogo(variante).normalizado == "estadistica"


def test_normalizado_reduce_los_espacios_internos_a_uno() -> None:
    assert NombreCatalogo("Medidas   de \t tendencia  central").normalizado == (
        "medidas de tendencia central"
    )


def test_normalizado_quita_dieresis() -> None:
    assert NombreCatalogo("Pingüinos").normalizado == "pinguinos"


def test_normalizado_conserva_la_enie_como_letra_propia() -> None:
    assert NombreCatalogo("  AÑO ").normalizado == "año"
    assert NombreCatalogo("Año").normalizado != NombreCatalogo("Ano").normalizado
    assert NombreCatalogo("Diseño").normalizado == "diseño"


def test_normalizado_quita_la_tilde_aunque_haya_enie_en_el_mismo_nombre() -> None:
    assert NombreCatalogo("Añadir ÁNGULO").normalizado == "añadir angulo"


def test_normalizado_distingue_nombres_realmente_distintos() -> None:
    assert NombreCatalogo("Álgebra").normalizado != NombreCatalogo("Estadística").normalizado


def test_igualdad_exacta_de_valor() -> None:
    assert NombreCatalogo(" Álgebra ") == NombreCatalogo("Álgebra")
    assert NombreCatalogo("álgebra") != NombreCatalogo("Álgebra")
