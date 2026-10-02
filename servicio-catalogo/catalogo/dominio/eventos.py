"""Eventos de dominio del catálogo (MODELO-DOMINIO.md 11.6 y erratas E-5).

Son internos: el catálogo no publica al broker (CONTRATOS.md 1). Los acumula el agregado
``Competencia`` y el caso de uso los entrega a ``PublicadorEventosPuerto`` después de
guardar. El modelo del catálogo no tiene fechas en el contrato, así que no las llevan.
"""

from __future__ import annotations

from dataclasses import dataclass

from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo


@dataclass(frozen=True, slots=True)
class EventoDeDominio:
    """Base de los eventos de dominio del catálogo."""


@dataclass(frozen=True, slots=True)
class CompetenciaCreada(EventoDeDominio):
    """Se creó una competencia (emitido por ``Competencia.crear``).

    Attributes:
        competencia_id: Identificador de la competencia creada.
        nombre: Nombre con el que se creó.
    """

    competencia_id: CompetenciaId
    nombre: NombreCatalogo


@dataclass(frozen=True, slots=True)
class CompetenciaRenombrada(EventoDeDominio):
    """Se cambió el nombre de una competencia (emitido por ``Competencia.renombrar``).

    Attributes:
        competencia_id: Identificador de la competencia (no cambia, INV-22).
        nombre_anterior: Nombre antes del cambio.
        nombre_nuevo: Nombre después del cambio.
    """

    competencia_id: CompetenciaId
    nombre_anterior: NombreCatalogo
    nombre_nuevo: NombreCatalogo


@dataclass(frozen=True, slots=True)
class TemaCreado(EventoDeDominio):
    """Se agregó un tema a una competencia (emitido por ``Competencia.agregar_tema``).

    Attributes:
        competencia_id: Competencia a la que pertenece el tema.
        tema_id: Identificador del tema creado.
        nombre: Nombre del tema.
    """

    competencia_id: CompetenciaId
    tema_id: TemaId
    nombre: NombreCatalogo


@dataclass(frozen=True, slots=True)
class SubtemaCreado(EventoDeDominio):
    """Se agregó un subtema a un tema (emitido por ``Competencia.agregar_subtema``).

    Attributes:
        competencia_id: Competencia dueña del agregado.
        tema_id: Tema al que pertenece el subtema.
        subtema_id: Identificador del subtema creado.
        nombre: Nombre del subtema.
    """

    competencia_id: CompetenciaId
    tema_id: TemaId
    subtema_id: SubtemaId
    nombre: NombreCatalogo
