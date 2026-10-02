"""Funciones compartidas por los casos de uso (no son casos de uso)."""

from catalogo.aplicacion.excepciones import CompetenciaNoEncontradaExcepcion
from catalogo.dominio.competencia import Competencia
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import CompetenciaId


def cargar_competencia(repositorio: CompetenciaRepositorio, competencia_id: str) -> Competencia:
    """Convierte el id recibido en texto y carga la competencia.

    Args:
        repositorio: Repositorio de competencias.
        competencia_id: UUID de la competencia, en texto.

    Returns:
        La competencia con su jerarquía completa.

    Raises:
        SolicitudInvalidaExcepcion: Si el id no es un UUID bien formado.
        CompetenciaNoEncontradaExcepcion: Si la competencia no existe.
    """
    identificador = CompetenciaId.desde_texto(competencia_id)
    competencia = repositorio.obtener_por_id(identificador)
    if competencia is None:
        raise CompetenciaNoEncontradaExcepcion(f"La competencia {identificador} no existe.")
    return competencia
