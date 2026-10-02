"""Traducción del agregado ``Competencia`` a su DTO ``CompetenciaRespuesta``."""

from catalogo.aplicacion.dtos.respuestas import (
    CompetenciaRespuesta,
    SubtemaRespuesta,
    TemaRespuesta,
)
from catalogo.dominio.competencia import Competencia


def a_competencia_respuesta(competencia: Competencia) -> CompetenciaRespuesta:
    """Convierte la competencia en su respuesta, con temas y subtemas ordenados.

    Los temas y los subtemas se devuelven ordenados por nombre normalizado, de forma
    ascendente (decisión del equipo sobre el orden de los listados).

    Args:
        competencia: Agregado a convertir.

    Returns:
        La respuesta con la jerarquía completa.
    """
    temas = sorted(competencia.temas, key=lambda tema: tema.nombre.normalizado)
    return CompetenciaRespuesta(
        competencia_id=str(competencia.id),
        nombre=competencia.nombre.valor,
        descripcion=competencia.descripcion,
        temas=tuple(
            TemaRespuesta(
                tema_id=str(tema.id),
                nombre=tema.nombre.valor,
                subtemas=tuple(
                    SubtemaRespuesta(subtema_id=str(subtema.id), nombre=subtema.nombre.valor)
                    for subtema in sorted(tema.subtemas, key=lambda s: s.nombre.normalizado)
                ),
            )
            for tema in temas
        ),
    )
