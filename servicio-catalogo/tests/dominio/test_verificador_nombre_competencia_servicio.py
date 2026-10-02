"""Pruebas de ``VerificadorNombreCompetenciaServicio`` (INV-24 entre competencias)."""

import pytest

from catalogo.dominio.competencia import Competencia
from catalogo.dominio.excepciones import NombreDuplicadoExcepcion
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.dominio.verificador_nombre_competencia_servicio import (
    VerificadorNombreCompetenciaServicio,
)
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria


@pytest.fixture
def competencia_guardada(repositorio: CompetenciaRepositorioEnMemoria) -> Competencia:
    competencia = Competencia.crear(NombreCatalogo("Diseño de software"))
    repositorio.guardar(competencia)
    return competencia


@pytest.fixture
def verificador(repositorio: CompetenciaRepositorioEnMemoria) -> VerificadorNombreCompetenciaServicio:
    return VerificadorNombreCompetenciaServicio(repositorio)


def test_un_nombre_libre_es_aceptado(
    verificador: VerificadorNombreCompetenciaServicio, competencia_guardada: Competencia
) -> None:
    verificador.exigir_nombre_disponible(NombreCatalogo("Razonamiento cuantitativo"))


@pytest.mark.parametrize(
    "repetido", ["Diseño de software", "diseño de software", "DISEÑO DE SOFTWARE", "  diseño   de software "]
)
def test_inv24_un_nombre_repetido_difiera_en_mayusculas_tildes_o_espacios_lanza_nombre_duplicado(
    verificador: VerificadorNombreCompetenciaServicio,
    competencia_guardada: Competencia,
    repetido: str,
) -> None:
    with pytest.raises(NombreDuplicadoExcepcion) as error:
        verificador.exigir_nombre_disponible(NombreCatalogo(repetido))
    assert error.value.codigo == "NOMBRE_DUPLICADO"


def test_la_propia_competencia_no_cuenta_como_duplicado(
    verificador: VerificadorNombreCompetenciaServicio, competencia_guardada: Competencia
) -> None:
    verificador.exigir_nombre_disponible(NombreCatalogo("DISEÑO de software"), competencia_guardada.id)


def test_otra_competencia_si_cuenta_como_duplicado(
    verificador: VerificadorNombreCompetenciaServicio,
    repositorio: CompetenciaRepositorioEnMemoria,
    competencia_guardada: Competencia,
) -> None:
    otra = Competencia.crear(NombreCatalogo("Otra"))
    repositorio.guardar(otra)
    with pytest.raises(NombreDuplicadoExcepcion):
        verificador.exigir_nombre_disponible(NombreCatalogo("diseño de software"), otra.id)
